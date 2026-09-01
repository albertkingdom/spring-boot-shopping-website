# Layered Architecture & Transaction Boundaries

專案內部知識庫，Controller / Service / Repository 各自的職責、`@Transactional` 該放哪裡、以及為什麼一律 constructor injection。

## 一句話

**Controller 是「翻譯」，Service 是「決策」，Repository 是「儲藏室」**。每一層只做自己的事，Controller 不做業務決策，Service 不知道 HTTP，Repository 不知道業務規則。

## 分層職責

### Controller
- 接 HTTP request、解析路徑/查詢/body 到 DTO
- Bean Validation 驗輸入
- 呼叫 Service
- 把 Service 回傳轉成 HTTP response
- **不做**：業務規則、DB 查詢、entity 組裝、跨多個 DB 操作的協調

### Service
- 業務規則、多個 Repository 的協調
- Transaction 邊界（`@Transactional`）
- 呼叫外部服務（Cloudinary、payment gateway、mail）
- 回傳給 Controller 的資料 —— 讀取端回傳 Response DTO，寫入端可以回傳 id 或 entity（依情境）
- **不做**：HTTP 相關（HttpStatus、HttpHeaders、ResponseEntity）、驗證錯誤格式化

### Repository
- 純粹 CRUD + JPA 查詢
- 只認 entity 型別
- **不做**：業務決策、跨表協調

## 錯誤範例：Controller 承擔業務邏輯（重構前的 OrderController）

```java
@PostMapping
public HttpStatus saveOrder(@Valid @RequestBody CreateOrderRequest orderRequest, Principal principal) {
    Order newOrder = new Order();
    BigDecimal orderTotalPrice = BigDecimal.ZERO;
    for (CreateOrderItemRequest i : orderRequest.getItems()) {
        Product product = productServiceImpl.getProductById(i.getProductId());  // ← DB 查詢
        OrderItem orderItem = OrderItem.snapshotOf(product, i.getQuantity());   // ← Entity 組裝
        newOrder.addOrderItem(orderItem);
        BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
        orderTotalPrice = orderTotalPrice.add(lineTotal);                       // ← 業務計算
    }
    newOrder.setPriceSum(orderTotalPrice);
    newOrder.setUserId(userRepository.findByEmail(userEmail).getId());          // ← Repository 直接呼叫
    orderServiceImpl.saveOrder(newOrder);
    return HttpStatus.OK;
}
```

**問題**：
- Controller 直接 `userRepository`（違反 AGENTS.md #22）
- 商品查詢、快照建構、價格計算全在 Controller
- 想用「另一種入口」建立訂單（例：CLI、內部工具）就要 duplicate 這整段邏輯
- 沒有 `@Transactional`：product 查完、user 查完，最後 save 若失敗，只是 save 回滾，前面的 DB read 也用了 connection

## 重構後：Controller 薄、Service 厚

```java
// Controller — 4 lines
@PostMapping
public HttpStatus saveOrder(@Valid @RequestBody CreateOrderRequest req, Principal principal) {
    orderService.createOrder(req, principal.getName());
    return HttpStatus.OK;
}

// Service — the actual work
@Override
@Transactional
public Long createOrder(CreateOrderRequest request, String userEmail) {
    Order order = new Order();
    BigDecimal total = BigDecimal.ZERO;

    for (CreateOrderItemRequest item : request.getItems()) {
        Product product = productService.getProductById(item.getProductId());
        order.addOrderItem(OrderItem.snapshotOf(product, item.getQuantity()));
        total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    order.setPriceSum(total);
    order.setUserId(userRepository.findByEmail(userEmail).getId());

    return orderRepository.save(order).getId();
}
```

**好處**：
- Controller 只翻譯，好讀
- 業務邏輯集中，好測（`OrderServiceImplTest.createOrder_snapshotsProductsAndSumsTotalExactly` 可以完全用 mock 驗證）
- 一個 `@Transactional` 包整個流程 → 全成或全敗
- 想加新入口（例如 admin 幫使用者下單）→ 直接呼叫 `orderService.createOrder(...)`

## `@Transactional` 邊界該放哪

**原則**：一個 `@Transactional` 對應**一個業務動作**。

- **每個 Service public 方法**是候選：它是外界看到的動作單位
- **不要**放在 Controller 或 Repository
- **不要**放太上層讓一個 transaction 跨太多不相關的動作（會拖 lock 太久）
- **不要**放太下層讓一個業務動作被切成好幾個 transaction（該原子的不原子）

### 現有實作範例

```java
@Transactional
public Long createOrder(...) { /* 多個 read + 一個 write */ }

@Transactional(readOnly = true)
public OrderDetailResponse getOrderDetailById(Long id) { /* 純讀取，加 readOnly hint */ }

@Transactional
public void deleteOrder(Long id) { /* read + delete */ }
```

### `readOnly=true` 的效果

- 對 JPA：只 flush changes 不觸發，某些 provider 會做進階最佳化
- 對 JDBC 一些驅動：可以路由到 read replica（未來多 DB 時有用）
- **語意提示**：讓 reviewer 一眼看出這個方法不會寫

### `@Transactional` 的常見坑

**1. Same-class 呼叫失效**
```java
public void a() { b(); }              // b 上的 @Transactional 沒生效！
@Transactional public void b() { ... }
```
Spring `@Transactional` 是 AOP proxy 實作，只在**外部呼叫進 bean** 才觸發。同 class 內互呼直接走 `this`，繞過 proxy。
解法：把 `b` 拆到別的 bean，或用 `AopContext.currentProxy().b()`（不建議）。

**2. private 方法沒作用**
`@Transactional` 需要能被 override，private 不行。要 `public`（或至少 protected/package）。

**3. Rollback 只對 RuntimeException**
預設只有 `RuntimeException` / `Error` 才觸發 rollback。checked exception 不會。
如果拋 checked exception 也要 rollback：`@Transactional(rollbackFor = Exception.class)`。

**4. `try-catch` 吃掉 exception → 不 rollback**
```java
@Transactional
public void foo() {
    try {
        repo.save(x);
        somethingThatThrows();
    } catch (Exception e) {   // ← 吞掉，transaction 就 commit
        log.warn("...");
    }
}
```
要 rollback 就要重拋、或呼叫 `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()`。

**5. Propagation 預設 `REQUIRED`**
一個 `@Transactional` 方法內呼叫另一個 `@Transactional` 方法（跨 bean）→ 加入現有 transaction。多數情境是對的。要獨立 transaction（例：稽核 log 不管主流程成敗）用 `REQUIRES_NEW`。

## Constructor Injection

專案已全面改為 constructor injection，理由：

### `@Autowired` field 的問題

```java
@Component
public class OrderService {
    @Autowired private OrderRepository orderRepository;   // ← field injection
    @Autowired private UserRepository userRepository;
}
```

- **測試困難**：不能不啟 Spring 就構造這個物件。單元測試要 reflection 或 `@InjectMocks`
- **必填/選填不明確**：看 code 不知道哪些依賴是必要的
- **不能 final**：欄位不能 `final`，讓 mutability 潛入
- **易生循環依賴**：Spring 靜默處理循環注入，出事很難追

### Constructor injection

```java
@Component
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            ProductService productService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productService = productService;
    }
}
```

- **測試簡單**：`new OrderServiceImpl(mock, mock, mock)` 就能建
- **必填清楚**：constructor signature 就是依賴清單
- **可以 final**：immutable 欄位，thread-safe
- **循環依賴當場失敗**：Spring 建 bean 時抓到，馬上炸

### Spring 4.3+ 的甜點

單一 constructor 不需要標 `@Autowired`，Spring 自動用它注入。所以你只看到 constructor，沒有 annotation 噪音。

## Controller 依賴 Service **介面**，不是 `*Impl`

```java
// 對
public OrderController(OrderService orderService) { ... }

// 錯
public OrderController(OrderServiceImpl orderServiceImpl) { ... }
```

好處：
- 換實作（例：加 caching decorator、feature flag 分流）不用改 Controller
- 測試容易 mock（mock interface 比 mock class 快、不需要 Mockito subclass proxy）
- 讀 Controller 只需要看 interface，不會被 Impl 細節分心

`SecurityConfig` 也一樣，讀 `UserDetailsService` interface 不是 `UserServiceImpl`。

## 分層在專案的實際邊界

```
                                HTTP Request
                                     ↓
Controller  (dto/request/*) ────▶ 呼叫 Service ────▶ dto/response/*
                                     ↓
Service     (Service interface + Impl) ────▶ 業務邏輯 + @Transactional
                                     ↓
Repository  (Spring Data JPA interfaces) ────▶ CRUD + query
                                     ↓
Entity      (model/*, @Entity) ────▶ JPA mapping ────▶ MySQL DECIMAL/VARCHAR/...
```

**跨層規則**：
- Controller 只能持有 Service（介面）+ 一些工具（JwtUtil、AuthenticationManager）
- Service 可以持有 Repository、其他 Service（介面）、外部客戶端
- Repository 只能持有其他 Repository（透過 join query）或 entity references
- Entity 是「純資料 + 極少 helper method」，**不要**在 entity 內放 Service 呼叫或業務決策

## 相關

- `AGENTS.md` 第 22-26 行「分層與交易邊界」
- `docs/dto-and-mass-assignment.md`：request DTO 為什麼跟 entity 分離
- `docs/order-immutability.md`：`OrderItem.snapshotOf` 為什麼是 static factory 而不是 service method
- `ARCHITECTURE_TODO.md` P1「分層與交易邊界」（本 PR 完成大部分）

## 常見誤區

- **「Controller 呼叫 Repository 快一點」**：省一層看似方便，Controller 因此扛業務規則、tests 變重、換 UI 層時要重寫
- **「Service 回傳 entity 給 Controller，Controller 再組 JSON」**：entity 應該關在 service 層，Controller 只看 DTO
- **「反正 API 出去前有 filter，entity 帶敏感欄位沒差」**：filter/interceptor 是 defense-in-depth 不是主防線；entity 到得了 controller = 序列化風險（見 D 項 password hash 洩漏）
- **「@Transactional 加在 Controller 上比較保險」**：Controller 一個方法可能組合多個 Service call，你想要每個 Service 自己是 transaction 邊界；上層加 @Transactional 會把它們併成一個超大 transaction，長時間鎖 row
