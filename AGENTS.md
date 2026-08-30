# Project Rules

本檔案適用於整個後端 repository。除非使用者明確要求，所有修改都必須遵守以下規則。

## Project Scope

- 本 repository 是 Spring Boot 購物網站後端；前端位於另一個 repository，不要在未獲要求時修改前端。
- 目前技術基線為 Java 8、Spring Boot 2.6、Maven、Spring Data JPA、Spring Security、MySQL、JWT 與 Cloudinary。
- 一般功能修改必須維持目前 Java 與 Spring Boot 相容性；框架或 Java 升級應視為獨立 migration 工作，不得混入一般功能 PR。
- 架構改善項目與優先順序記錄在 `ARCHITECTURE_TODO.md`。完成項目時同步更新核選框與相關文件。

## Feature Specification and Planning

- 規劃或實作新 feature 時，必須先使用 `$feature-spec` skill 建立或更新 spec 與可核選的 todo list。
- 在 spec 與 todo list 足以指導並驗收實作前，不開始撰寫功能程式碼。Spec 的位置、必要內容、核選清單與更新規則以 `$feature-spec` skill 為準。
- 使用 `$feature-spec` 只代表建立或更新規劃文件，不代表已獲授權實作功能、commit、push 或部署。

## Architecture

- 維持單體應用，不為了形式拆成微服務。
- HTTP 呼叫方向應為 `Controller -> Service -> Repository -> Database`。
- Controller 只處理輸入驗證、身份資訊、Service 呼叫與 HTTP response，不直接使用 Repository，也不承擔價格計算或 Entity 組裝等商業邏輯。
- Service 應依賴 Repository 與外部服務，負責交易邊界及商業規則。
- Controller 優先依賴 Service 介面，不直接依賴 `*ServiceImpl`。
- 新程式碼使用 constructor injection，不新增 field injection。
- 跨多筆資料或多次 repository 操作的 use case 應明確評估並設定 `@Transactional`。
- JPA Entity、API request DTO 與 response DTO 應分離；不得直接將 `User` Entity 暴露為 API response。
- 新增或修改金額欄位時使用 `BigDecimal`，不得新增 `float` 或 `double` 金額計算。
- Order item 必須保存下單當時所需的歷史資料，不可只依賴商品目前的名稱與價格。

## Security and Configuration

- 不得把密碼、JWT signing secret、API key、private key 或其他真實憑證提交到 Git。
- 設定值優先由環境變數或 profile-specific configuration 注入；範例檔只能包含明確的 placeholder。
- 若發現已提交的 secret，應提出移除與輪替建議；刪除檔案中的值不等於完成輪替。
- 使用者 API response 不得包含 password 或 password hash。
- 管理員 endpoint 必須有明確的 `ROLE_ADMIN` 授權規則及測試。
- 不得將 JWT、Authorization header、密碼、完整 authentication object 或第三方憑證寫入 log。
- CORS 應列出允許來源，不得在正式環境使用萬用 `*`。
- Access token 與 refresh token 必須維持不同用途，驗證流程不得允許互相替代。

## API and Data Rules

- Request DTO 使用 Bean Validation，集合、ID、數量與分頁參數都要驗證。
- 數量必須為正數；page 不得小於零。
- 找不到資源回傳 `404`、輸入錯誤回傳 `400`、唯一性衝突回傳 `409`、未登入回傳 `401`、權限不足回傳 `403`。
- 使用全域 exception handler 提供一致且不洩漏內部實作細節的錯誤格式。
- 建立資源成功優先回傳 `201 Created` 與建立後的 response DTO。
- 公開 API 欄位命名必須一致；變更既有欄位時要檢查前端相容性。
- Schema 及必要 seed data 應透過 Flyway 或 Liquibase migration 管理，不依賴 `ddl-auto=update` 建立正式環境 schema。
- Repository 查詢要留意 N+1；不要以 `FetchType.EAGER` 作為預設解法。

## Cloudinary and File Uploads

- 驗證檔案大小、MIME type、空檔案與允許的副檔名。
- 不得直接使用使用者提供的原始檔名組成本機路徑；使用安全的暫存檔 API。
- 暫存檔必須在成功或失敗時都能清除。
- 同時異動資料庫與 Cloudinary 時，需定義失敗補償、重試或一致性策略。
- 更新商品但未上傳新圖片時，必須保留既有圖片資訊。

## Build and Tests

- 所有 Maven 指令使用 repository 內的 wrapper：`./mvnw`。
- 修改前先確認相關測試；修改後至少執行受影響範圍的測試。
- 提交前的完整驗證指令為：`./mvnw test`。
- 影響啟動、JPA mapping、migration 或 Spring context 時，還應執行：`./mvnw verify`。
- 新增商業邏輯必須補 Service unit test；新增或修改 endpoint 必須補 Controller/security test。
- 涉及 MySQL 特有行為時，優先使用 Testcontainers MySQL 做 integration test。
- Cloudinary 呼叫在自動測試中必須 mock，不可操作真實帳號或產生外部資源。
- 不以 `-DskipTests` 作為完成驗證的證據。

## Code Style

- Package 名稱一律小寫。
- 類別與方法命名應表達業務意圖，避免 `Custom*`、`Test*` 或空泛名稱出現在正式程式碼。
- 不新增註解掉的舊程式碼；版本歷史由 Git 保存。
- 不使用 `System.out.println` 或 `printStackTrace`；統一使用 logging framework，並避免敏感內容。
- 不保留未使用的 import、欄位、方法或依賴。
- 優先小範圍、可驗證的重構，不在功能 PR 中同時進行大規模 package 搬移。

## Git Workflow

本專案採用輕量 GitHub Flow／short-lived branch workflow。此流程同樣適用於 GitHub 或 GitLab hosting。

### Branches

- `master` 是唯一長期存在且隨時可部署的主分支。
- 不建立常駐的 `develop`、`release` 或 `hotfix` 分支。
- 每項工作從最新 `master` 建立短生命週期分支，一個分支只處理一個主題。
- 建議的人工作業分支格式：
  - `feat/<short-topic>`
  - `fix/<short-topic>`
  - `refactor/<short-topic>`
  - `test/<short-topic>`
  - `docs/<short-topic>`
  - `chore/<short-topic>`
- 分支名稱只能描述工作類型與主題，不得包含任何 AI 工具、模型、agent、生成工具或自動化作者名稱。
- 緊急修正仍從 `master` 建立 `fix/<short-topic>`，完成後直接透過 PR 合回 `master`。

### Commits

- 產生、建議或重新產生 commit message 時，必須使用 `$commit-msg` skill，並以該 skill 的輸出格式與檢查流程為準。
- `$commit-msg` 必須重新掃描目前 staged diff；若沒有 staged files，停止並提示先 stage，不得根據 unstaged changes 猜測訊息。
- 使用 `$commit-msg` 只代表產生文字建議，不代表已獲授權執行 `git add`、`git commit` 或 `git push`。
- Summary 使用祈使句、保持簡短，不加句號。
- Commit message 不得提及任何 AI 工具、模型、agent、生成工具或自動化作者身分。
- Commit message 只描述程式碼或文件本身的變更與目的，不描述變更由何種工具產生。
- 一個 commit 應是一個可理解、可回復的邏輯單位；不要混入無關格式化或檔案變更。
- 不得提交真實 secret、IDE 暫存檔、執行 log、build output 或本機資料庫檔案。

### Pull Requests

- 不直接 push 或 commit 到 `master`；所有變更透過 Pull Request／Merge Request 合併。
- PR 保持小而聚焦，說明問題、解法、風險、資料庫或 API 影響，以及實際執行的測試。
- 合併前必須通過 Maven tests、必要的 integration tests 與 secret scanning。
- 有 schema 或 API breaking change 時，PR 必須附 migration 與相容性說明。
- 合併策略使用 squash merge，讓 `master` 保持線性且每個 PR 對應一個主要提交。
- 合併前更新至最新 `master` 並解決衝突；不要對共享的 `master` force push。
- 合併後刪除工作分支。

### Releases

- 可部署版本以 annotated tag 標記，格式為 `vMAJOR.MINOR.PATCH`。
- 破壞相容性的 API 或資料庫變更增加 MAJOR；向下相容功能增加 MINOR；修正增加 PATCH。
- Release tag 只能建立在已通過 CI 的 `master` commit 上。

## Change Safety

- 開始修改前先檢查 `git status`，保留並避開使用者既有的未提交變更。
- 不使用 `git reset --hard`、`git checkout -- <file>` 或其他會丟失工作內容的指令。
- 不因測試或重構覆寫使用者的 Docker、環境或本機設定。
- 修改 migration、認證、授權、金額或訂單流程時，視為高風險變更並提高測試強度。
- 除非使用者明確要求，不執行 commit、push、merge、tag、部署或 secret rotation。

## Definition of Done

- [ ] 新 feature 已依 `$feature-spec` skill 在實作前建立 spec 與 todo list，並已更新為最終行為與完成狀態。
- [ ] 實作符合本檔案的分層、安全與資料規則。
- [ ] 受影響測試已新增或更新且通過。
- [ ] `./mvnw test` 通過；需要時 `./mvnw verify` 也通過。
- [ ] 沒有新增 secret、敏感 log 或不必要的公開欄位。
- [ ] API、migration、環境變數與操作方式的文件已同步。
- [ ] `ARCHITECTURE_TODO.md` 中對應項目已更新。
- [ ] Git diff 僅包含本次工作的相關變更。
