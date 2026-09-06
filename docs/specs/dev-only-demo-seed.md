# 僅開發環境的 Demo Seed

## 背景與目標

先前未提交的 `V5__seed_demo_data.sql` 曾位於正式 Flyway migration location。即使檔案尚未被 Git 追蹤，Maven 與 Docker build 仍會把它打包，Flyway 也會在所有 profile 掃描並執行它。這會使所有環境（包括 production）自動建立展示資料，且將固定的管理員密碼 hash 永久納入版本控制，違反 `docs/database-migration.md` 的安全規則。該檔案已移除；本規格定義其安全替代方案。

目標是讓正式環境只套用 schema 與必要角色 migration；啟用 `dev` profile 的本機資料庫一律建立 demo 資料。Demo 帳號的密碼必須由未提交的環境變數提供。

## 範圍

包含：

- 新增 `dev` profile 的 Flyway location 設定。
- 正式 location 僅保留 `V1` 至 `V4`；已移除先前的 `db/migration/V5__seed_demo_data.sql`。
- 將 demo seed 改為只在 `dev` profile 掃描的 repeatable migration。
- 透過 Flyway placeholder 由環境變數提供 demo 帳號 BCrypt hash。
- 讓重複啟動不會新增重複使用者、商品、訂單或 order item。
- 更新環境變數範例與資料庫操作文件。

不包含：

- 變更正式 schema、公開 API、認證流程或授權規則。
- 自動在 staging 或 production 建立 demo 資料。
- 將任何明文密碼、password hash 或真實憑證提交到 Git。

## 實作理由與指引

Flyway 在應用程式啟動前執行 SQL。基礎設定維持只掃描 `classpath:db/migration`，所以 production 只能看到 schema 與必要角色。`application-dev.properties` 會額外加入 `classpath:db/dev-migration`，讓本機以 `SPRING_PROFILES_ACTIVE=dev` 啟動時一律執行 demo seed。

Demo seed 使用 repeatable migration（`R__seed_demo_data.sql`），適合會隨展示內容調整的資料；但其每次 checksum 改變都可能再次執行，因此每筆 insert 必須由唯一鍵或 `NOT EXISTS` 保護。密碼不能寫進 SQL，而是由 `spring.flyway.placeholders` 從本機未提交的環境變數代入；缺少必要 hash 時啟動必須明確失敗，不能建立可預測的帳號。

在移除現有 V5 前，必須先查詢每個既有本機資料庫的 `flyway_schema_history`。若任何 disposable dev DB 已套用 V5，移除檔案後會因「已套用但無法解析的 migration」而驗證失敗。由於本專案尚未正式部署，這類 DB 必須以新 volume 或 `docker compose down -v` 明確重建；不得用 `flyway:repair` 隱藏歷史差異，也不得把它升格為 staging 或 production 資料庫。

## 情境與驗收條件

1. Given 未設定 `dev` profile，When 應用程式套用 Flyway，Then 只執行 `db/migration` 的 schema 與角色 migration，不建立 demo 使用者、商品、訂單或 order item。
2. Given `SPRING_PROFILES_ACTIVE=dev` 且所有必要 demo password hash 已設定，When 在空 MySQL 啟動應用程式，Then 基礎 migration 與 demo seed 都成功執行，並建立預期展示資料。
3. Given 已套用 demo seed 的 dev 資料庫，When 重啟應用程式或 repeatable seed 再次執行，Then 使用者、商品、訂單與 order item 的筆數不增加。
4. Given `dev` profile 缺少必要 demo password hash，When Flyway 啟動，Then 啟動失敗並提供不含密碼內容的設定錯誤。
5. Given production profile，When 檢查已套用 migration，Then 不含 dev-only migration 的紀錄與 demo 資料。

## API 影響

No impact。Endpoint、request、response、HTTP status 與 authorization 規則均不變；僅改變開發資料庫是否預先具備可登入的展示帳號與資料。

## 資料影響

- 正式 schema migration 維持 `V1` 至 `V4`；先前的 `V5__seed_demo_data.sql` 已移除且不得重新提交。
- `V5__seed_demo_data.sql` 不得暫留在任何被 Flyway 掃描或會被 application build 打包的目錄。
- 新增 `src/main/resources/db/dev-migration/R__seed_demo_data.sql`，僅由 `dev` profile 掃描。
- Demo 資料包含 users、users_roles、product、orders 與 order_item；須使用既有資料表與 snapshot 欄位。
- 不新增 production schema、欄位或 constraint。若現有 schema 沒有可支援 idempotency 的唯一鍵，seed SQL 必須以明確 `NOT EXISTS` 查詢保護。

### Demo 資料參考（不含 credential）

後續重寫 dev-only seed 時，保留以下展示資料意圖；不得沿用現有 V5 中的 email、password hash 或固定 order UUID：

| 商品 | 單價 |
| --- | ---: |
| Wireless Bluetooth Headphones | 1290.00 |
| Mechanical Keyboard | 2490.00 |
| USB-C Hub 7-in-1 | 890.00 |
| Ergonomic Mouse | 790.00 |
| 27" 4K Monitor | 8990.00 |
| Laptop Stand Aluminum | 1190.00 |
| Webcam 1080p | 990.00 |
| Desk LED Light Bar | 1490.00 |
| Portable SSD 1TB | 2290.00 |
| Noise Cancelling Earbuds | 3490.00 |

展示訂單保留一位一般 demo buyer 的兩筆訂單（Headphones + Keyboard + Hub，合計 4670.00；Monitor + Light Bar，合計 10480.00）與另一位一般 demo buyer 的一筆訂單（Mouse + Portable SSD + Webcam + Laptop Stand，合計 5260.00）。Demo 管理員與一般使用者的識別資料及密碼均應於本機以環境變數提供，且不寫入本規格或 seed SQL。

## 商業與安全規則

- Demo 資料絕不能在未啟用 `dev` profile 的環境建立。
- Demo 管理員與一般使用者的 password hash 僅能由未提交的環境變數提供，不能硬編碼於 SQL、properties、README 或 `.env.example`。
- 不記錄 password hash、JWT、Authorization header 或完整 authentication object。
- `ROLE_ADMIN` 僅分配給明確指定的 demo 管理員；一般 demo 使用者只能擁有 `ROLE_USER`。

## 錯誤與邊界情況

- 缺少或格式不正確的環境變數時，應用程式必須失敗，不可套用空字串或預設密碼。
- 啟動前需驗證 demo password hash 為預期 BCrypt 格式；Flyway placeholder 的存在檢查不足以驗證格式，且驗證過程不得記錄 hash。
- MySQL 已手動存在同 email、商品名稱或固定 order UUID 時，seed 必須不建立重複資料，也不能覆寫既有使用者密碼或訂單。
- 若 root row 與人工資料碰撞，seed 必須跳過該 root row 及其 dependent order item；不得以「最新一筆訂單」推測或附加關聯資料。
- Repeatable migration 內容改變後，重新執行仍需保持 idempotent。
- 非 `dev` profile 即使設定 demo 環境變數，也不得掃描 dev-only location。

## 測試策略

- Service unit test：No impact，沒有新增 Service 商業邏輯。
- Controller/security test：No impact，沒有 endpoint 或授權規則變更。
- Integration test：以乾淨 MySQL 驗證 default 與 `dev` profile 的 Flyway locations、seed 結果、重複執行的 idempotency，以及缺少 required placeholder 時的失敗行為。
- 最終驗證：執行受影響 integration test、`./mvnw test`；此項會影響設定與 migration，另執行 `./mvnw verify`。

## 實作 Todo

- [ ] 在不啟動應用程式的前提下，盤點既有 dev DB 的 `flyway_schema_history` 是否有 V5；若有，將該 DB 視為 disposable，明確重建其 volume，不得執行 `flyway:repair`。
- [x] 已移除 `src/main/resources/db/migration/V5__seed_demo_data.sql`，且僅在本規格保留不含 credential 的資料組合參考。
- [ ] 檢查 V1 schema 的實際 unique constraint；為 users、roles、users_roles、product、orders 與 order_item 定義明確 `NOT EXISTS` 條件、collision policy 與每筆 dependent data 的安全跳過行為。
- [ ] 核對每筆 demo order 的 `price_sum` 與 item snapshot 合計；修正目前 Alice 第二筆與 Bob 訂單金額不一致的資料。
- [ ] 新增 `application-dev.properties`，僅在 `dev` profile 擴充 Flyway location 並設定 required placeholder 的環境變數對應。
- [ ] 新增在 Flyway 之前執行的 demo password hash 格式驗證，定義 admin 與一般 demo user 的 placeholder 名稱與數量，且不在 log 輸出其值。
- [ ] 建立 `db/dev-migration/R__seed_demo_data.sql`，改用 placeholders 注入 password hash，且所有資料寫入可重複執行。
- [ ] 更新 `.env.example`，只列出 demo hash 的 placeholder 變數名稱，不填入值。
- [ ] 更新 `docs/database-migration.md` 與 frontend／root 執行文件，說明如何安全啟用 dev demo seed。
- [ ] 新增 MySQL integration test，覆蓋 default profile、dev profile、缺少設定與重複執行。
- [ ] 執行 `./mvnw test` 與 `./mvnw verify`，並依結果更新本規格核選清單。
