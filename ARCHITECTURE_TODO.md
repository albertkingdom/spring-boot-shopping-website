# Backend Architecture TODO

依照風險與影響程度排序。完成項目後，將 `[ ]` 改成 `[x]`。

## P0：安全性

- [x] 移除 `application.properties` 中的 MySQL 與 Cloudinary 憑證，改由環境變數或 Secret Manager 注入。
- [ ] 輪替目前已提交至版本庫的 MySQL 密碼與 Cloudinary API secret。
- [x] 移除 `JwtUtil` 中硬編碼的 JWT secret，改用環境設定並確保正式環境使用足夠強度的密鑰。
- [x] 建立 `UserResponse` DTO，禁止 API 回傳 `User` Entity 與 password hash。
- [x] 將 `/api/user/all` 限制為 `ROLE_ADMIN`，並確認一般登入者無法取得使用者清單。
- [x] 依開發、測試與正式環境限制 CORS allowed origins，移除萬用來源 `*`。
- [x] 移除可能洩漏 JWT、登入資訊、使用者資料或第三方回應的 `System.out.println`。

### 驗收條件

- [ ] Git 追蹤的設定檔不含任何真實密碼、API key 或 JWT signing secret。
- [ ] 一般使用者無法呼叫管理員使用者清單 API。
- [ ] 所有使用者相關 API response 都不包含 `password` 欄位。

## P1：帳號與初始化資料

- [x] 使用 Flyway 或 Liquibase 建立資料庫 schema migration。
- [x] 透過 migration 或僅限開發環境的 seed 建立 `ROLE_USER` 與 `ROLE_ADMIN`。
- [x] 提供安全、可重複執行的管理員建立方式，不在程式碼中硬編碼正式密碼。
- [x] 將「建立使用者」與「加入預設角色」放入同一個 `@Transactional` Service 方法。
- [x] 當 email 重複或預設角色不存在時，回傳明確的 `409` 或伺服器設定錯誤，而不是 NullPointerException。

### 驗收條件

- [ ] 全新資料庫啟動後可以正常註冊一般使用者。
- [ ] 註冊失敗時不會留下沒有角色的不完整帳號。
- [ ] 管理員建立流程有文件且可在不同環境安全執行。

## P1：分層與交易邊界

- [x] 將訂單建立、商品查詢、價格計算、使用者查詢與 Entity 組裝移到 `OrderService`。
- [x] Controller 只負責輸入驗證、呼叫 application/service layer 與建立 HTTP response。
- [x] Controller 改為依賴 `OrderService`、`ProductService`、`UserService` 介面，而不是 `*ServiceImpl`。
- [x] 全面改用 constructor injection，移除 field injection。
- [x] 為建立訂單、更新商品、刪除訂單及其他跨多次 DB 操作的方法加入適當的 `@Transactional`。
- [ ] 定義 Cloudinary 操作失敗時的補償策略，避免 DB 與圖片狀態不一致。

### 驗收條件

- [ ] Controller 不直接使用 Repository。
- [ ] Controller 不包含訂單價格計算或 Entity 組裝邏輯。
- [ ] 跨多筆資料的操作能完整成功或完整回滾。

## P1：Entity 與 API DTO 分離

- [ ] 將 `model` 拆分為 `entity`、`dto/request` 與 `dto/response`。
- [x] 為登入、註冊、商品建立／更新及訂單建立建立專用 Request DTO。
- [x] 為商品、訂單、使用者與分頁建立專用 Response DTO。
- [x] API 不直接接受或回傳 JPA Entity。
- [ ] 建立集中且可測試的 Entity／DTO mapping 邏輯。

### 驗收條件

- [ ] Entity 欄位變更不會自動改變公開 API 格式。
- [ ] API request 無法寫入 id、roles、password hash 等非預期欄位。
- [ ] API response 不依賴 Hibernate lazy loading 才能完成序列化。

## P1：金額與訂單正確性

- [x] 將 `Product.price`、`Order.priceSum`、DTO 金額與計算邏輯由 `Float` 改為 `BigDecimal`。
- [x] 將資料庫金額欄位改為具有明確精度與小數位數的 `DECIMAL`。
- [ ] 訂單總額只由後端依商品單價與數量計算，不採信前端傳入的總額。
- [x] 在 `OrderItem` 保存下單當下的商品名稱與單價快照。
- [ ] 為金額計算、四捨五入與商品改價後的歷史訂單補上測試。

### 驗收條件

- [ ] 金額計算沒有 binary floating-point 誤差。
- [ ] 商品之後改名或改價，不會改變既有訂單內容。

## P2：資料模型與查詢效能

- [ ] 評估將 `Order.userId` 改為正式的 `Order -> User` 關聯。
- [ ] 評估將 `OrderItem.productId` 改為 `OrderItem -> Product` 關聯，同時保留歷史快照。
- [ ] 將不必要的 `FetchType.EAGER` 改為 LAZY，使用 DTO projection 或明確 fetch query 取得畫面所需資料。
- [ ] 移除訂單明細逐項查詢商品造成的 N+1 query。
- [ ] 為常用查詢欄位與外鍵增加適當 index／constraint。

### 驗收條件

- [ ] 讀取單一訂單明細不會隨商品數量線性增加 SQL 查詢次數。
- [ ] 資料庫存在 user、product 與 order item 的完整性約束。

## P2：驗證、錯誤處理與 API 設計

- [ ] 為訂單 items 加上 `@NotEmpty`，product id 加上 `@NotNull`，quantity 加上 `@Positive`。
- [ ] 為分頁參數設定預設值並驗證不可小於零。
- [ ] 建立 `ResourceNotFoundException`、`ConflictException` 等具體例外。
- [ ] 由全域 exception handler 統一轉換成一致的錯誤 response。
- [ ] 找不到商品、訂單或使用者時回傳 `404`，重複 email 回傳 `409`，輸入錯誤回傳 `400`。
- [ ] 建立訂單成功時回傳 `201 Created` 與訂單 response，不將 `HttpStatus` 當作 response body。
- [ ] 統一 API 欄位命名，例如將 `access_token`／`refresh_token` 明確決定為 snake_case 或 camelCase。

## P2：JWT 與 Spring Security

- [ ] 將 access token 與 refresh token 加入明確的 token type，驗證時禁止互相替代。
- [ ] 加入 issuer、audience 與必要的 JWT claim 驗證。
- [ ] 評估 refresh token rotation、撤銷與登出策略。
- [ ] 不將內部 JWT 驗證例外訊息直接回傳給客戶端。
- [x] 移除未使用的 `CustomAuthenticationFilter` 與舊 Session Interceptor。
- [ ] 升級時將 `WebSecurityConfigurerAdapter` 改為 `SecurityFilterChain` 設定方式。
- [ ] 為未登入、一般使用者與管理員建立完整的 endpoint authorization 測試。

## P2：檔案上傳與 Cloudinary

- [ ] 使用 `Files.createTempFile`，不要直接以原始檔名組合本機路徑。
- [ ] 驗證圖片 MIME type、副檔名、檔案大小與空檔案。
- [ ] 在 `finally` 中清理暫存檔，確保上傳失敗時也不殘留檔案。
- [ ] 更新商品未上傳新圖片時，保留原本的 `imgUrl` 與 `imgName`。
- [ ] 刪除商品圖片失敗時提供 retry／補償機制。

## P2：測試與品質

- [ ] 建立 Testcontainers MySQL 測試骨架，供 Repository 與 Security 整合測試共用；同時將既有 `@SpringBootTest` 的 `contextLoads` 改用同一個 MySQL container，讓本地 `./mvnw verify` 無需手動啟動 MySQL。
- [ ] 以 Testcontainers 補 Repository 整合測試，涵蓋 `UserRepository`、`ProductRepository`、`OrderRepository`、`OrderItemRepository` 的 JPA mapping 與關鍵查詢（含分頁與 N+1 驗證）。
- [ ] 以 Testcontainers 補 Security 整合測試（不使用 `addFilters = false`），驗證未登入回 `401`、`ROLE_USER` 與 `ROLE_ADMIN` 授權邊界、JWT 過期與 refresh token 不得替代 access token。
- [ ] 補上 UserService 註冊、密碼加密與角色指派測試。
- [ ] 補上登入、JWT refresh、過期 token、錯誤角色與越權存取測試。
- [ ] 補上建立訂單、交易回滾、商品不存在及非法數量測試。
- [ ] Mock Cloudinary，測試上傳、更新與刪除失敗的處理。
- [ ] 將 CI 中的 `echo "run-tests"` 改成真正執行 Maven tests。

## P3：專案整理與升級

- [ ] 移除未使用的 import、註解程式碼與重複設定。
- [ ] 將 package 名稱統一為小寫，例如 `exception`、`interceptor`。
- [ ] 將設定拆為 `application-local`、`application-test`、`application-prod` profiles。
- [ ] 評估升級至仍受支援的 Java LTS、Spring Boot 與 Spring Security 版本。
- [ ] 更新 Swagger/OpenAPI、JWT、Cloudinary 與其他相依套件。
- [ ] 在 README 補充本機啟動、migration、seed、環境變數與管理員建立流程。

## 建議目標結構

- [ ] 評估由全域技術分層逐步整理為模組化單體：

```text
product/
  api/
  application/
  domain/
  infrastructure/

order/
  api/
  application/
  domain/
  infrastructure/

identity/
  api/
  application/
  domain/
  infrastructure/

shared/
  security/
  exception/
  config/
```

- [ ] 先完成安全性與交易問題，再進行 package 大搬移，避免重構期間同時改變過多行為。
