# Project Status

本文件用於跨工作階段交接目前進度。開始新工作前，先閱讀 `AGENTS.md`、本文件與 `ARCHITECTURE_TODO.md`，再以實際 Git 與 GitHub 狀態核對；外部狀態可能在本文件更新後改變。

最後更新：2026-09-02（PR #22 docs sync）

## Current Baseline

- 主要版本庫：GitHub `albertkingdom/spring-boot-shopping-website`
- 主分支：`master`
- 本機 `master` 追蹤：`origin/master`
- 建立本文件前的主分支 commit：`b2b67f3 chore: migrate project setup and secure configuration (#2)`
- 工作目錄在建立本文件前為乾淨狀態。
- 本機 `.env` 已保留並由 `.gitignore` 忽略；不得提交或輸出其中內容。
- GitLab CI 設定與 remote 暫時保留，尚未決定移除時間。
- 部署平台與部署流程延後決定。

## Completed

- [x] 將本機有效變更以不含新增密鑰的分支同步到 GitHub。
- [x] 新增 GitHub Actions CI：Java 8、Maven verify、MySQL 8 service 與 Docker image build。
- [x] 新增 Dependabot 的 GitHub Actions 更新設定。
- [x] 將 MySQL 與 Cloudinary 設定改由環境變數注入。
- [x] 新增安全的 `.env.example`、`.dockerignore` 與環境檔忽略規則。
- [x] PR #2 已通過 CI 並以 squash merge 合併。
- [x] 本機 `master` 已同步並改為追蹤 GitHub `origin/master`。
- [x] 已合併的工作分支已從本機與 GitHub 刪除。
- [x] [PR #5](https://github.com/albertkingdom/spring-boot-shopping-website/pull/5)：將 `JwtUtil` 的硬編碼 JWT secret 改由 `JWT_SECRET` 環境變數注入，並在啟動時驗證長度。
- [x] [PR #6](https://github.com/albertkingdom/spring-boot-shopping-website/pull/6)：加入專案交接文件（`PROJECT_STATUS.md`、`docs/jwt-basics.md`）並擴充 `ARCHITECTURE_TODO.md` P2 整合測試待辦。
- [x] [PR #7](https://github.com/albertkingdom/spring-boot-shopping-website/pull/7)：`SecurityConfig` 加入 `/api/user/**` 需要 `ROLE_ADMIN` 的規則，並補上 `UserControllerSecurityTest` 驗證匿名／一般使用者／管理員三種情境。
- [x] [PR #8](https://github.com/albertkingdom/spring-boot-shopping-website/pull/8)：將全部 `System.out.println` 與 `printStackTrace` 改為 SLF4J logger，並刪除未使用的 `CustomAuthenticationFilter`、`LoginInterceptor`、`TestInterceptor`、`WebConfig` 與其他註解掉的舊程式碼。
- [x] [PR #9](https://github.com/albertkingdom/spring-boot-shopping-website/pull/9)：新增 `UserResponse` DTO，`/api/user/all` 不再回傳 `User` entity 或 password hash，並補測試驗證 response 不含 `password` 欄位。
- [x] PR #3：Bump `actions/setup-java` 4.9.1 → 6.0.0（Dependabot）。
- [x] PR #4：Bump `actions/checkout` 6.1.0 → 7.0.1（Dependabot）。
- [x] [PR #10](https://github.com/albertkingdom/spring-boot-shopping-website/pull/10)：CORS 移除萬用 `*`，改為 `app.cors.allowed-origins` env 驅動白名單，預設允許 `http://localhost:3000`，並補上 preflight 測試。
- [x] [PR #11](https://github.com/albertkingdom/spring-boot-shopping-website/pull/11)：導入 Flyway（`flyway-core` 8.0.5），加入 `V1__baseline.sql`，將 `ddl-auto` 由 `update` 改為 `validate`，並附 `docs/database-migration.md`。Testcontainers 整合骨架（Batch 1 #2）因 Java 8 + Testcontainers 1.19 對 Docker Desktop for Mac 相容問題，延到 Batch 8 Java 升級後再做。
- [x] [PR #12](https://github.com/albertkingdom/spring-boot-shopping-website/pull/12)：`Product.price` 與 `Order.priceSum` 由 `Float` 改為 `BigDecimal`（`DECIMAL(10,2)`），`OrderController` 訂單累加改用 BigDecimal 精確運算，加入 `V2__price_columns_to_decimal.sql` migration 與 `docs/money-types.md`。
- [x] [PR #13](https://github.com/albertkingdom/spring-boot-shopping-website/pull/13)：`OrderItem` 新增 `product_name` + `unit_price` 快照欄位（`V3__order_item_snapshot_columns.sql`），下單時透過 `OrderItem.snapshotOf(product, quantity)` 拷貝當下值，讀單改讀 snapshot 不再 join `product` 表，並附 `docs/order-immutability.md`。
- [x] [PR #14](https://github.com/albertkingdom/spring-boot-shopping-website/pull/14)：`/api/register`、`/api/login`、`/api/order` 全部改用 `dto/request/*` 專用 DTO（`RegisterRequest`、`LoginRequest`、`CreateOrderRequest`、`CreateOrderItemRequest`），加 Bean Validation，`ApiExceptionHandler` 加 `MethodArgumentNotValidException` 處理器，並附 mass assignment 攻擊測試與 `docs/dto-and-mass-assignment.md`。刪除死程式碼 `OrderRequest` / `OrderRequestItem` / `InvalidRequestException`。
- [x] [PR #15](https://github.com/albertkingdom/spring-boot-shopping-website/pull/15)：新增 response DTO 家族 `ProductResponse`、`OrderSummaryResponse`、`OrderDetailResponse`、`OrderItemResponse`、`PageResponse<T>`；`ProductService` / `OrderService` 讀取端不再回傳 JPA entity；controllers 全部改回 DTO；刪除舊 wrapper `CustomOrderResponse`、`OrderItemDetail`、`ProductsPagination`、`OrdersPagination`。
- [x] [PR #16](https://github.com/albertkingdom/spring-boot-shopping-website/pull/16)：訂單建立邏輯（商品查詢、快照、價格計算、user 查詢）由 `OrderController` 搬到 `OrderService.createOrder`，加 `@Transactional`；全部 controller / service / filter / SecurityConfig 改為 constructor injection；Controller 依賴 service 介面而非 `*Impl`；`ProductServiceImpl` update/delete 也加 `@Transactional`；新增 `OrderServiceImplTest.createOrder_snapshotsProductsAndSumsTotalExactly` 用 BigDecimal 驗證訂單總額精確計算；附 `docs/layered-architecture.md`。
- [x] [PR #17](https://github.com/albertkingdom/spring-boot-shopping-website/pull/17)：加 `V4__seed_roles.sql` 建立 `ROLE_USER`/`ROLE_ADMIN`；新增 `UserService.register(RegisterRequest)` 把「建立使用者 + 加預設角色」放同一個 `@Transactional`；重複 email 拋 `ConflictException` → 409；缺 `ROLE_USER` seed 拋 `IllegalStateException`；`ApiExceptionHandler` 加 conflict handler；`docs/database-migration.md` 補「Seed data」與「Admin bootstrap」章節。
- [x] [PR #18](https://github.com/albertkingdom/spring-boot-shopping-website/pull/18)：JWT 強化：access/refresh token 加 `type` claim + `iss` + `aud`；`JwtUtil.verify(token, TokenType)` 集中驗簽名/過期/iss/aud/type；`CustomAuthorizationFilter` 只接 access token，`/refreshToken` 只接 refresh token；外部錯誤訊息改為 opaque `"invalid access/refresh token"` 不再洩漏內部例外；新增 `JwtUtilTest` 6 個 case 覆蓋 type 互相替代、iss、aud 拒絕；`application.properties` 新增 `jwt.issuer` / `jwt.audience` env driven；附 `docs/jwt-hardening.md`。
- [x] [PR #19](https://github.com/albertkingdom/spring-boot-shopping-website/pull/19)：新增 `ResourceNotFoundException`（→ 404）與 handler；service 內 `orElseThrow(RuntimeException::new)` 全改為丟具體例外；分頁參數加 `@Min(0)` + `defaultValue="0"` 驗證；`POST /api/order` 改回 `201 Created` + `Location` header + `{id}`；`OrderController` 標 `@Validated`；`ProductControllerTest` 加 3 個 case 覆蓋 not-found / pagination validation；附 `docs/error-handling.md`。`access_token/refresh_token` naming unification 尚未做（會影響前端契約，待另評估）。
- [x] [PR #20](https://github.com/albertkingdom/spring-boot-shopping-website/pull/20)：Cloudinary 上傳安全化：改用 `Files.createTempFile` 亂數命名（防路徑穿越）；magic byte + whitelist 驗證 MIME；size / 空檔驗證；`try-finally` 保證暫存檔清理；`ProductServiceImpl.updateProduct` 未上傳新圖時保留原 imgUrl/imgName；刪除 Cloudinary 圖失敗只記 `log.warn` 不 propagate（DB 已刪，孤兒圖靠 reconciliation）；`ApiExceptionHandler` 加 `IllegalArgumentException` → 400；`application.properties` 設 multipart size 上限；新 `CloudinaryServiceTest` 5 case + `docs/file-upload-safety.md`。
- [x] [PR #21](https://github.com/albertkingdom/spring-boot-shopping-website/pull/21)：升級 Java 8 → 21、Spring Boot 2.6.2 → 3.3.4、Spring Security 5 → 6、Hibernate 5.6 → 6.5、Cloudinary http44 1.30 → http5 2.3、Springfox → springdoc-openapi 2.6、JWT lib 3.18 → 4.4。全域 `javax.*` → `jakarta.*`；`SecurityConfig` 由 `WebSecurityConfigurerAdapter` 改為 `SecurityFilterChain` bean（`antMatchers` → `requestMatchers`、`authorizeRequests` → `authorizeHttpRequests`）；`Dockerfile` 改 multistage `eclipse-temurin:21`；CI 改 Java 21；36/36 tests 全過。
- [x] [PR #22](https://github.com/albertkingdom/spring-boot-shopping-website/pull/22)：把 `ARCHITECTURE_TODO.md` 各項驗收條件和實際已完成的 work item 打勾（P0/P1/P2 補上 ~13 個 checkbox），並改寫 `PROJECT_STATUS.md` Next Actions 分「高優先／中優先／部署／低優先」。

## Next Actions

請由上而下處理，完成後同步更新核選框與「最後更新」日期。

### 高優先

- [ ] **輪替所有曾提交到 Git 歷史的 MySQL 與 Cloudinary 憑證**；只在本機 `.env` 或部署平台 secrets 更新，不得寫入 repository。**人工作業**。
- [ ] **建立 Testcontainers MySQL 骨架**：加入 `AbstractIntegrationTest` 基底、`ShoppingWebsiteApplicationTests.contextLoads` 改用 container、CI 移除 `services.mysql`。Java 21 已於 PR #21 解鎖此工作。

### 中優先（P2 開放項目）

- [ ] 補齊測試覆蓋：`UserServiceImpl.register` 單元測試、login/refresh/expired token 端到端測試、`OrderService.createOrder` 交易回滾與非法輸入測試（分別對應 `ARCHITECTURE_TODO.md` P2 測試段落）。
- [ ] Refresh token rotation、撤銷與登出策略（`ARCHITECTURE_TODO.md` P2 JWT 段落）。
- [ ] 為未登入、一般使用者與管理員建立完整 endpoint authorization 測試（`ARCHITECTURE_TODO.md` P2 JWT 段落，目前僅 `/api/user/all` 有覆蓋）。
- [ ] 統一 `access_token`／`refresh_token` API 欄位命名（會影響前端契約，需先與前端協調）。
- [ ] 資料模型效能改善：`Order.userId` → `Order -> User` 關聯、`OrderItem.productId` → `OrderItem -> Product` 關聯（保留 snapshot）、`FetchType.EAGER` → `LAZY` 或 fetch join、消除訂單明細 N+1、加 index / FK constraint（`ARCHITECTURE_TODO.md` P2 資料模型與查詢效能段落）。
- [ ] `model/` 拆分為 `entity/` + `dto/*` 目錄，並建立集中且可測試的 Entity/DTO mapper（`ARCHITECTURE_TODO.md` P1 Entity 與 API DTO 分離段落）。

### 部署 / 環境

- [ ] 部署選定後，設定各環境 `APP_CORS_ALLOWED_ORIGINS`（staging / prod 各自的前端域名）。
- [ ] 部署選定後，設定各環境 `JWT_ISSUER` / `JWT_AUDIENCE`（讓 dev token 無法對 prod 使用）。
- [ ] 決定是否使用 `git filter-repo` 重寫歷史以清除舊密鑰；執行前必須先確認輪替完成、備份與協作者重新 clone 計畫。
- [ ] GitHub 流程穩定後，決定是否移除 `.gitlab-ci.yml` 與 GitLab remote。
- [ ] 選擇部署方案後，再設計 CD workflow；目前 CI 不負責部署。

### 低優先（收尾）

- [ ] README 補完：本機啟動、Flyway migration、seed、環境變數、admin 建立流程（Batch 9）。
- [ ] Cloudinary 孤兒圖 reconciliation job（`docs/file-upload-safety.md` 有 recipe）。
- [ ] 拆 `application.properties` 為 `application-local` / `application-test` / `application-prod` profiles。
- [ ] 評估由技術分層逐步整理為模組化單體（`ARCHITECTURE_TODO.md` 尾段「建議目標結構」）。

## Security Notes

- 移除目前檔案中的明文值，不代表歷史中的憑證已失效。
- 在完成憑證輪替前，安全工作優先於依賴版本更新與部署。
- 不在 issue、PR、commit message、log 或交接文件中貼出任何真實 secret。
- 重寫 Git 歷史會改變 commit SHA 並需要 force push，必須視為獨立且高風險的維護工作。

## Resume Checklist

開始後先執行唯讀檢查：

```bash
git status --short --branch
git log -3 --oneline --decorate
git remote -v
gh auth status
gh pr list --repo albertkingdom/spring-boot-shopping-website
```

如果 GitHub CLI 尚未重新登入，改由已登入的 GitHub 網頁確認 PR 與 Actions 狀態。

建議的新工作階段開場指示：

```text
請先讀 AGENTS.md、PROJECT_STATUS.md、ARCHITECTURE_TODO.md。
先不要修改檔案，檢查 Git 狀態、最近三個 commit 與 GitHub 未關閉的 PR，
摘要目前狀態後，從 PROJECT_STATUS.md 第一個未完成項目繼續。
```

## References

- [PR #2：GitHub CI 與安全設定遷移](https://github.com/albertkingdom/spring-boot-shopping-website/pull/2)
- [Architecture improvement checklist](ARCHITECTURE_TODO.md)
- [Project rules](AGENTS.md)
