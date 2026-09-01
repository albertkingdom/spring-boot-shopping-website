# Error Handling & Validation

專案內部知識庫，API 錯誤格式、HTTP 狀態碼慣例、Bean Validation、以及例外處理的分工。

## 一句話

**每個錯誤都有一個 HTTP status 碼、一個一致格式的 body、一個 typed exception，且客戶端看到的訊息不告訴攻擊者內部細節**。

## HTTP status 碼對照

| 情境 | Status | 例外 | 客戶端動作 |
|---|---|---|---|
| Body validation 失敗（`@Valid @RequestBody`） | 400 | `MethodArgumentNotValidException` | 修欄位重送 |
| Query/param validation 失敗（`@Min`、`@Pattern` 等） | 400 | `ConstraintViolationException` | 修 query 重送 |
| 找不到資源 | 404 | `ResourceNotFoundException` | 檢查 id / 不要重試 |
| 唯一性衝突（duplicate email） | 409 | `ConflictException` | 換值重送 |
| 未登入 | 401 / 403（見下） | Spring Security | 導向登入 |
| 權限不足 | 403 | Spring Security | 用戶不能改 |
| 建立成功 | **201 Created** + `Location` header | — | — |
| 一般成功讀取 | 200 OK | — | — |
| 刪除成功 | 200 OK / 204 No Content | — | — |
| 未預期錯誤 | 500 | 其他 `RuntimeException` | Log 端 debug |

### 401 vs 403 現況

Spring Security 預設在沒 `AuthenticationEntryPoint` 時，一律回 **403**。專案內未登入的匿名請求會拿到 403 而非 401，這是既有行為（`UserControllerSecurityTest.listAllUsers_returns403_whenAnonymous` 驗證）。要改成 401 需要設 entry point，屬另一個 refactor，未動。

## 統一的錯誤 body 格式

### Validation 失敗（400）

`ApiExceptionHandler.handleMethodArgumentNotValid` 與 `handleConstraintViolationException` 都回：

```json
{
  "message": "Invalid parameter",
  "errors": [
    {
      "resource": "registerRequest",
      "field": "email",
      "code": "Email",
      "message": "Not a valid email format."
    },
    {
      "resource": "registerRequest",
      "field": "password",
      "code": "Size",
      "message": "Password length should be at least 6 characters."
    }
  ]
}
```

- `errors` 陣列可包含多個欄位錯誤（一次驗完全部再回，不是遇到第一個就停）
- 每個 item 有 `field` / `code` / `message`，前端可依 `field` 對應到 UI 位置

### Business exception（404 / 409）

`handleNotFound` / `handleConflict` 回：

```json
{
  "message": "product not found: 42"
}
```

單一 `message` 欄位。**故意不揭露內部細節**（例如：DB row 是否存在、SQL 錯誤、stack trace）。

### 建立資源成功（201）

`POST /api/order`：

```
HTTP/1.1 201 Created
Location: /api/order/42
Content-Type: application/json

{"id": 42}
```

Client 可直接拿 `Location` 或 `id` 打 `GET /api/order/{id}` 拿完整詳情。**不要把 `HttpStatus.OK` 當 body 回傳**（前身 anti-pattern）。

## Bean Validation

### 使用位置

**Request DTO**：欄位級別 annotation，在 controller `@Valid @RequestBody`：

```java
public class RegisterRequest {
    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6)
    private String password;

    @NotBlank
    private String name;
}
```

**巢狀 DTO**：容器欄位加 `@Valid` 才會遞進去驗每個 element：

```java
public class CreateOrderRequest {
    @NotEmpty
    @Valid                                     // ← 沒 @Valid 只會驗 @NotEmpty
    private List<CreateOrderItemRequest> items;
}
```

**Query params / method params**：需要 controller 標 `@Validated`（class 級別）：

```java
@RestController
@RequestMapping("/api/order")
@Validated                                     // ← 開啟 method-level validation
public class OrderController {
    @GetMapping
    public PageResponse<...> getOrdersByPage(
            @RequestParam(name = "page", defaultValue = "0")
            @Min(value = 0, message = "page must be zero or greater.")
            int page) { ... }
}
```

沒 `@Validated` 就算加了 `@Min` 也**不會驗**。

### 常用 annotation

| 用途 | Annotation |
|---|---|
| String 不空白（不 null、非空、非只有空白） | `@NotBlank` |
| Object 不 null（允許空字串） | `@NotNull` |
| Collection / String 不空 | `@NotEmpty` |
| 字串長度 | `@Size(min=, max=)` |
| Email 格式 | `@Email` |
| Regex | `@Pattern(regexp="...")` |
| 數值 > 0 | `@Positive` / `@PositiveOrZero` |
| 數值範圍 | `@Min(0)`、`@Max(100)`、`@DecimalMin`、`@DecimalMax` |

### 錯誤訊息本地化

每個 constraint 都可以帶 `message = "..."`。這些訊息會經 `MessageSource` 展開（可放 `messages.properties`）。本專案目前寫死英文 message，簡單有效。

## Typed exceptions

專案內每個「可預期的錯誤」對到自己的 exception class：

```
com.albertkingdom.shoppingwebsite.exception/
├── ConflictException           # 409
└── ResourceNotFoundException   # 404
```

**每個 exception 都是 `RuntimeException`**，理由：

- Java checked exception 一路往上宣告，會傳染
- Service / Repository 內部有些邏輯就是「找不到丟例外」，強迫 caller 宣告 checked exception 沒好處
- Spring 事務只對 `RuntimeException` 觸發 rollback（見 `docs/layered-architecture.md`）—— checked exception 拋出來 transaction 會 commit，需要額外設 `@Transactional(rollbackFor=...)`

**丟例外的位置：Service 或更下層**。Controller 不主動丟業務例外，只把 Spring 抛的 validation exception 交給 global handler。

## Global Handler：`ApiExceptionHandler`

`@RestControllerAdvice` bean，處理跨 controller 的例外對應：

```java
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(...) { /* → 400 */ }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolationException(...) { /* → 400 */ }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(...) { /* → 404 */ }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflict(...) { /* → 409 */ }
}
```

**加新的錯誤類型時的動作**：
1. 建 `xxxException extends RuntimeException`（放 `exception/` package）
2. 從 service 內丟出（`throw new XxxException(...)`）
3. `ApiExceptionHandler` 加一個 `@ExceptionHandler` 方法
4. 補測試：`@ExceptionHandler` 對應到正確 status code + body

## Test 覆蓋

`ProductControllerTest`：
- `getProductById_returns404_whenServiceThrowsResourceNotFound` — 服務丟 `ResourceNotFoundException` → 404 + `{message: "product not found: 999"}`
- `getProductsByPage_returns400_whenPageNegative` — `?page=-1` → 400 + validation error
- `getProductsByPage_defaultsToZero_whenPageOmitted` — 沒 param → 走 default，200

`UserControllerSecurityTest`：
- `register_returns400_whenEmailInvalid` / `whenPasswordTooShort` — @Valid @RequestBody 失敗
- `register_returns409_whenEmailAlreadyRegistered` — service 丟 `ConflictException` → 409

## 對外訊息 opacity

- **Business exception** message 是**業務層寫的**（`"product not found: 42"`、`"email already registered"`），對合法用戶清楚、對攻擊者無資訊優勢
- **不外流**：SQL 錯誤、stack trace、JWT 驗證失敗細節、內部路徑
- **外流可以的**：validation 失敗的欄位名與 constraint code（前端要用來標紅框）
- **完整細節去哪**：`log.warn(msg, exception)` 或 `log.error(msg, exception)` 進 log；prod 端從 log 系統排查

## 相關

- `docs/dto-and-mass-assignment.md`：request DTO 為什麼跟 entity 分離
- `docs/jwt-hardening.md`：JWT 錯誤 opacity 案例
- `docs/layered-architecture.md`：例外從哪一層丟、`@Transactional` rollback 只認 `RuntimeException`
- `AGENTS.md` 第 46-49 行 HTTP 狀態碼慣例
- `ARCHITECTURE_TODO.md` P2「驗證、錯誤處理與 API 設計」

## 常見錯誤

### `@Valid` 沒加，validation 沒發生
Body 有 `@NotBlank`，但 controller 忘了 `@Valid` → 什麼都不驗。**每個 `@RequestBody` 前必加 `@Valid`**。

### `@Validated` 沒加在 class，method-level annotation 失效
`@Min` on `@RequestParam` 需要 controller class 標 `@Validated`。少加就無效但不會報錯，很隱性。

### Service 丟 checked exception → transaction 沒 rollback
Spring 預設只對 `RuntimeException` rollback。丟 `IOException` transaction 會 commit。要嘛用 RuntimeException，要嘛 `@Transactional(rollbackFor = Exception.class)`。

### Global handler 抓不到 exception
- 檢查 `@RestControllerAdvice` bean 被掃到（如果 `@WebMvcTest` 只載入部份 context，可能沒它 —— 需要 `@Import(ApiExceptionHandler.class)`）
- 檢查 `@ExceptionHandler` 的 exception 型別是丟出的例外的 super 或本身

### 錯誤 message 洩漏 SQL / DB info
不要 `throw new ResourceNotFoundException(sqlException.getMessage())`。永遠自己寫訊息、log 端才記完整 exception。

### 用 exception 控制正常流程
Exception 應該是「異常」。正常業務流程（例：找不到就回 null，或 empty Optional）不要用例外。用了會誤導 caller、降低效能、擾亂 log。
