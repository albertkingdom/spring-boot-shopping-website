# 測試與正式環境隔離

> Legacy feature spec：目前環境契約已整理至 [`openspec/specs/deployment-environments/spec.md`](../../openspec/specs/deployment-environments/spec.md)，本檔案保留作歷史參考，不再作為新變更的 source of truth。

## 背景與目標

原本專案只有一份 root `docker-compose.yml` 與單一 named volume `db`；目前已拆出 dev、staging、prod Compose override，且 GitHub Actions 已發布 deployment image、以不可變 digest 建立 release manifest，並成功部署 staging。Compose 以 `MYSQL_DATABASE` 初始化 MySQL，並在 container 內組合 Spring JDBC URL。`.env.example` 的 localhost JDBC URL 僅適用於直接在主機執行 Spring。GitHub Actions 已以 MySQL service 執行 `./mvnw verify`，但 GitLab CI 的 test job 仍是 placeholder。production promotion 與 rollback 尚未實際驗證，且仍需完成 production 環境設定。這使本機、測試與正式環境容易共用資料庫、JWT secret 或第三方帳號，且無法在部署前取得可信的驗證結果。

目標是建立可重複部署且互不共用資料或憑證的環境模型：本機 `dev`、部署於 Mac mini 的持久 `staging` 與 `prod`，並用 GitHub Actions 的短生命週期 MySQL 執行自動測試。Mac mini 的公開流量由 Cloudflare Tunnel 提供，不依賴固定 IP 或 router port forwarding。

## 範圍

包含：

- 為 dev、staging、prod 定義獨立的 MySQL、Compose project、volume 與未提交的環境變數檔。
- 透過兩個獨立的 Cloudflare Tunnel 將 staging 與 production hostname 安全連至 Mac mini 的 loopback-only frontend port。
- 以 GitHub-hosted runner 驗證與發布 image，並以 Mac mini self-hosted runner 執行受保護的 deploy job。
- 定義 Spring profile 與各環境非敏感設定。
- 將 dev-only demo seed 限制在 `dev` profile。
- 在 CI 執行 Maven 測試與必要的 MySQL integration test。
- 以同一個已驗證的 image 依序部署 staging 與 production。

不包含：

- 從 Docker Compose 遷移到 Kubernetes、微服務或雲端特定平台。
- 將正式資料複製到 staging；staging 只使用 synthetic 或已去識別資料。
- 在版本控制中儲存環境密碼、JWT secret、Cloudinary credential 或 private key。

## 實作理由與指引

Spring profile 僅控制應用程式行為，不能隔離資料庫。真正的隔離要由不同 MySQL 實例或至少不同 database、不同帳號與不同 Docker volume 組成。每個持久環境應在不同主機或不同 Docker daemon 執行；若暫時共用同一主機，必須以不同的 `COMPOSE_PROJECT_NAME`、資料庫名稱、volume 與 credential 隔離。

Compose base 檔必須是 deployment-safe：只引用 backend 與 frontend 的 image、預設不公開 application/database/admin ports，且不包含 phpMyAdmin 或 `build:`。`docker-compose.dev.yml` 才加入本機 `build:`、開發 ports 與 phpMyAdmin；staging/prod override 僅選取同一組已發布的 backend/frontend image digest，並分別綁定 Mac mini loopback port。Mac mini 以 Podman machine 與 `podman compose` 執行 Compose 格式檔案；每個環境使用獨立 Compose project、MySQL database、named volume、env file 與 Cloudflare Tunnel credential。

第一批實作先交付 Compose 與 profile 邊界：base 檔建立私有 network、MySQL、Spring 與 Nginx frontend，但不含 `build:`、host port 或 phpMyAdmin。dev override 使用本機 build，並只綁定 `127.0.0.1` 的 `3306`、`8080`、`8888` 與 `80`；staging/prod override 強制使用 `SPRING_IMAGE`、`FRONTEND_IMAGE`，並各自只綁定 `127.0.0.1:8081`、`127.0.0.1:8082` 的 frontend。Cloudflare Tunnel 與 CI image registry 會在這個基礎通過驗證後再實作，避免 hostname 指向未完成的服務。

本機驗證使用獨立的 `docker-compose.test.yml`：Java 21 test container 以唯讀方式取得 source，先複製到 container 內暫存目錄後執行 `./mvnw test`；MySQL 只存在同一 Compose project 的私有 network，不發布 host port，也不建立 named volume。測試結束後以 `docker compose down --volumes --remove-orphans` 移除 test container、network 與暫存資料。這條測試路徑不使用任何開發、staging 或 production credential。

GitHub-hosted runner 負責 checkout、Maven verify、Docker build 與發布不可變 digest/release manifest；初期只發布 `linux/arm64`，因為唯一部署目標是 M1 Mac mini，可降低 GitHub Actions 用量。image 發布到 private GHCR package，workflow 以短期 `GITHUB_TOKEN` 寫入 package，不保存 registry secret。它不持有 Mac mini 或 production secret。Mac mini self-hosted runner 只接受已通過 GitHub Environment protection 的 deploy job，從 manifest pull 已驗證 image，再以 `podman compose up -d` 執行環境限定部署。staging 通過 smoke test 後，production 使用同一份 manifest，不在 production 臨時重新 build。

### 已確認的 Cloudinary 策略

目前選擇暫時共用既有 Cloudinary 帳號與 credential，不另開 staging 帳號；隔離邊界改為 environment-specific upload folder。實作後 staging 只能寫入 `shopping-website/staging`，production 只能寫入 `shopping-website/prod`，既有 dev 路徑也必須明確設定，不能再由 `CloudinaryService` 寫死 `shopping-website`。這降低目前建立帳號與管理 credential 的成本，但 Cloudinary credential 不是強隔離邊界：持有同一組 credential 的環境仍可能誤操作其他資料夾。因此 staging 的管理權限、環境檔與操作流程仍必須受限，日後應能無資料搬遷地換成獨立 staging credential。

## 目標環境與啟動模型

| 環境 | 執行位置 | Spring profile | Compose project | 資料庫 | Seed | 公開網路 |
| --- | --- | --- | --- | --- | --- | --- |
| `dev` | 開發者本機 | `dev` | `shopping-dev` | `shopping_dev`、`shopping-dev_db` | 自動載入 dev-only demo seed | 僅本機開發所需 ports |
| `staging` | Mac mini | `staging` | `shopping-staging` | `shopping_staging`、獨立帳號與 named volume | 不載入 | Cloudflare Tunnel HTTPS hostname |
| `prod` | 同一台 Mac mini、獨立 stack | `prod` | `shopping-prod` | `shopping_prod`、獨立帳號與 named volume | 不載入 | Cloudflare Tunnel HTTPS hostname |

目標檔案結構如下：

```text
docker-compose.yml             # 共通 service 與內部 network
docker-compose.dev.yml         # 唯一含 build: 的檔案；本機 ports、phpMyAdmin、dev profile
docker-compose.staging.yml     # 已驗證的 SPRING_IMAGE/FRONTEND_IMAGE digest、staging entry point/profile
docker-compose.prod.yml        # 與 staging 相同 image digest、production entry point/profile
docker-compose.test.yml        # Java 21 + temporary MySQL 的隔離 Maven test
application.properties         # 共通且不含 secret 的設定
application-dev.properties     # dev CORS 與後續 dev-only Flyway seed 設定
application-staging.properties # staging 的非敏感行為設定
application-prod.properties    # production 的非敏感行為設定
```

環境變數檔不納入 Git：本機使用 `.env.dev`；Mac mini 的 staging 與 production 分別保存於 `/Users/yklin/services/shopping-website/staging/staging.env` 與 `/Users/yklin/services/shopping-website/prod/prod.env`，且權限僅允許部署帳號讀取。這個使用者目錄不需要額外 sudo 權限，較適合由 GitHub Actions runner 維護。每份檔案都必須明確指定 `SPRING_PROFILES_ACTIVE`、專屬 DB credential、JWT issuer/audience、CORS origin 與第三方服務 credential。

公開 hostname 已確認為 `shop.albertkingdom.com`（production）與 `staging-shop.albertkingdom.com`（staging）。兩者各自對應獨立 Tunnel；staging 在公開 DNS 建立後仍必須先由 Cloudflare Access 限制為部署者可用，不能直接當作公開 beta 站。

目標啟動命令如下；這些命令必須等上述 Compose override 與 profile 設定完成後才能使用：

```bash
# 開發者本機
docker compose --env-file .env.dev -p shopping-dev \
  -f docker-compose.yml -f docker-compose.dev.yml up --build

# staging 主機（Mac mini）
PATH=/opt/homebrew/bin:$PATH podman compose \
  --env-file /Users/yklin/services/shopping-website/staging/staging.env -p shopping-staging \
  -f docker-compose.yml -f docker-compose.staging.yml up -d

# production 主機（Mac mini）
PATH=/opt/homebrew/bin:$PATH podman compose \
  --env-file /Users/yklin/services/shopping-website/prod/prod.env -p shopping-prod \
  -f docker-compose.yml -f docker-compose.prod.yml up -d
```

每個 Cloudflare Tunnel 在 Mac mini 僅能轉送到所屬環境的 loopback frontend port；不得公開 MySQL、Spring 或 phpMyAdmin port。Cloudflare 處理使用者端 HTTPS，`cloudflared` 以 outbound-only connection 連到 Cloudflare。staging 與 prod 都只能使用 CI 已建立的同一對 `SPRING_IMAGE` 與 `FRONTEND_IMAGE` digest；不得以 `build:` 在目標主機重新建置。部署前先更新 staging，完成 smoke test 後再 promote 到 production。

## 部署操作順序

### 目前已實作的 CI 行為

1. Pull request：GitHub-hosted runner 執行 Maven verify，並 build backend/frontend image，但不發布 image、不執行部署。
2. `master`：verify 與兩個 image build 都成功後，發布 private GHCR `linux/arm64` image，並上傳含兩個 immutable digest 的 release manifest artifact。
3. `master` 的 publish 成功後，workflow 會在標記為 `shopping-deploy` 的 Mac mini runner 自動部署 staging 並執行 loopback smoke test。GitHub Environment 與 runner 已設定；但主機的實際 staging env file、image manifest 與服務尚未設定，因此尚未有任何 Mac mini 部署。
4. `v*` annotated tag 會啟動 production workflow；它會確認 tag 位於 `master`、下載同一 commit 成功 CI 的 manifest，並等待 GitHub `production` Environment approval 後才部署。

### 部署前置設定

1. 在 GitHub repository 建立 `staging` 與 `production` Environment。已設定 staging 僅接受 `master`；production 僅接受 `v*` tag、必須由 `albertkingdom` 核准，且不允許 administrator bypass。staging 不設 approval gate，以便 master 驗證成功後自動部署。
2. 在 GitHub repository 設定允許 workflow 使用 `GITHUB_TOKEN` 寫入與讀取 GitHub Packages，讓 workflow 不需要保存長期 GHCR token。
3. 在 Mac mini 註冊單一 GitHub Actions self-hosted runner，標籤為 `self-hosted`、`macOS`、`arm64`、`shopping-deploy`。runner 只能接受 deploy workflow，PR workflow 一律留在 GitHub-hosted runner。
4. 在 Mac mini 建立 staging/prod 的未提交 env file，填入各自不同的 MySQL、JWT credential，並暫時填入共用 Cloudinary credential；各 environment-specific upload folder 由 profile properties 固定。部署 job 只從 release manifest 暫時注入 image digest，不把 digest 寫回 Git。
5. 建立兩條 Cloudflare Tunnel。staging Tunnel 只能轉送至 `http://127.0.0.1:8081`，production Tunnel 只能轉送至 `http://127.0.0.1:8082`；先替 staging 設定 Cloudflare Access，再公開 production hostname。

目前 staging 已建立 locally-managed `shopping-staging` Tunnel，並在 Mac mini 以使用者 LaunchAgent `com.albertkingdom.shopping-staging-tunnel` 常駐。它的 ingress 僅允許 `staging-shop.albertkingdom.com -> http://127.0.0.1:8081`，credentials 檔位於 Mac mini 的私有 `.cloudflared` 目錄。`staging-shop.albertkingdom.com` 已建立 CNAME 指向該 Tunnel，且 Cloudflare Access 的 `Shopping staging` application 已限制為 GitHub 登入、唯一允許 `albertkingdom@gmail.com` 的 policy；未登入請求會先被導向 Access。staging 已成功拉取 CI 發布的 image，並通過 loopback frontend 與 API smoke test；production Tunnel、DNS 與 Access 尚未建立。

### 已決定的部署觸發策略

1. Pull request 不部署；它只執行驗證與 image build。
2. Pull request 合併至 `master` 後，CI verify 與 image publish 成功即自動部署到 staging。staging 必須使用該 commit 已發布的兩個 immutable image digest。
3. staging smoke test 成功後，維持該版本供驗收；它不會因此自動進入 production。
4. 要發布 production 時，維護者在已通過 CI 的 `master` commit 建立 annotated tag，例如 `v1.2.3`。tag 只標記要發布的版本，不重新建置 image。
5. tag 觸發 production deploy workflow；GitHub `production` Environment 會暫停 job，直到 required reviewer 手動核准。核准後才以該 tag commit 對應、已由 `master` CI 發布的同一對 image digest 部署 production。

因此，`master` 是持續更新 staging 的主線；release tag 是明確指定 production 版本的開關；GitHub Environment approval 是 production 的人工安全閘門。

### 每次部署流程

1. 合併至 `master` 的 CI verify 與 image publish 成功後，取得該 commit 的 release manifest；其 backend/frontend digest 必須來自同一個 git SHA。
2. workflow 自動啟動 staging deploy。Mac mini runner 使用短期 `GITHUB_TOKEN` 登入 GHCR，pull manifest 指定的 image，並以 `podman compose` 更新 `shopping-staging`。
3. 在 Mac mini loopback 執行 smoke test：frontend `http://127.0.0.1:8081/`、Nginx `/api/` proxy、Flyway migration 與登入流程。任何失敗都停止，禁止建立或核准 production release。
4. staging 驗收成功後，在該 `master` commit 建立 annotated release tag。tag 觸發 production deploy；GitHub `production` Environment 的 required reviewer 必須先批准。
5. 核准後，production 以**該 tag commit 對應的同一份 manifest**部署；production 在 `http://127.0.0.1:8082/` 完成相同 smoke test 後，Cloudflare Tunnel 才可對外提供 `shop.albertkingdom.com`。

### Rollback

1. 選擇上一個已通過 staging smoke test 的 release manifest；不得以 tag 或重新 build 推測舊版本。
2. 以該 manifest 的兩個 digest 重新執行目標環境 deploy workflow。
3. 重跑 loopback smoke test；若涉及 schema migration，rollback 前先確認 migration 是否可向下相容，禁止以刪除 volume 作為 rollback。

## 情境與驗收條件

1. Given 開發者以 `dev` 設定啟動，When MySQL 初始化，Then 僅使用 `shopping_dev` 與 dev volume，且自動載入 demo seed。
2. Given staging 部署，When 應用程式啟動，Then 使用專屬 staging DB、JWT issuer/audience、Cloudinary test 帳號或隔離 namespace，且不載入 dev demo seed。
3. Given production 部署，When 應用程式啟動，Then 使用專屬 production DB、secret 與 CORS origin，且 MySQL 與 phpMyAdmin 不對公網暴露。
4. Given 任何作為部署來源的 CI pipeline，When 執行測試，Then 必須實際執行 Maven test/verify，並以空的 MySQL service 或 Testcontainers 驗證 migration 與必要 integration test；失敗不得 publish 或 deploy。
5. Given staging 已驗證 release manifest，When 進行 production 部署，Then backend 與 frontend 都使用完全相同的 image digest，而非重新 build。
6. Given staging 與 production 暫時共用 Cloudinary credential，When 管理員上傳商品圖片，Then 圖片的 Cloudinary `public_id` 分別位於 `shopping-website/staging` 與 `shopping-website/prod`，不再使用寫死的共用 folder。

## API 影響

No impact。API endpoint、request/response 格式、HTTP status 與授權規則不變。各環境僅以設定限制不同的 CORS origin 與 JWT issuer/audience。

## 資料影響

- 每個環境使用不同資料庫與 MySQL 帳號；不得共享 named volume 或 production connection string。
- 正式與 staging 只套用正式 Flyway migration；dev-only seed 依 `dev-only-demo-seed.md` 限制於 `dev` profile。
- staging 不使用可識別的 production 資料。

## 商業與安全規則

- 開發者工作站不得取得 production database credential、JWT secret 或 production Cloudinary credential。
- 不得把 MySQL `3306`、Spring `8080`、phpMyAdmin `8888` 對 production 公網發布；公開入口僅為 HTTPS reverse proxy。
- staging 與 prod 的 JWT secret、issuer、audience、資料庫帳號與 Cloudinary upload folder 必須不同；目前 Cloudinary 帳號與 credential 暫時共用，這是已知的非強隔離例外，不得誤寫成已隔離 credential。
- 所有部署環境以最小權限的非 root MySQL application user 連線。

## 錯誤與邊界情況

- 若 Compose 未指定 environment file、必要 secret 或 image tag，部署必須失敗，不能回退到預設 credential。
- 若 staging 或 production 偵測到錯誤的 Spring profile、資料庫名稱或 Compose project name，部署程序必須中止。
- 若 `app.cloudinary.upload-folder` 缺漏、空白或不符合目前環境的預期 folder，圖片上傳必須失敗，不能回退為通用 `shopping-website` folder。
- 資料庫 volume 重建只影響對應環境；不得以 `down -v` 操作其他環境。
- CI MySQL service 建立或 migration 失敗時，pipeline 必須失敗且不得發布／部署 image。

## 測試策略

- Service unit test：維持並執行既有 unit test。
- Controller/security test：維持並執行既有 endpoint 與 authorization test。
- Integration test：在 CI 的乾淨 MySQL 驗證 Flyway、JPA mapping 與 dev seed profile 的隔離行為。
- Cloudinary Service unit test：mock Cloudinary uploader，驗證每個 profile 注入的 upload folder 會傳入 upload options；不得使用真實 Cloudinary 帳號。
- Deployment smoke test：staging 部署後檢查 frontend、`/api/` proxy、health endpoint（若提供）與登入流程；production 僅在 staging 成功後部署相同 image digest。

## 實作 Todo

- [x] 已移除正式 Flyway location 中未提交的 V5 seed；不再允許 application build 或部署掃描該檔案。
- [ ] 盤點已受影響的 disposable dev DB，若 history 含 V5 則依 `dev-only-demo-seed.md` 重建，不得執行 `flyway:repair`。
- [ ] 在 Mac mini 建立僅供部署使用的目錄 `/Users/yklin/services/shopping-website/{staging,prod}`，權限限於部署帳號；建立彼此完全不同的 database、application user、secret 與 Cloudinary namespace。
- [ ] 為 staging/prod 使用 Compose-managed MySQL，建立各自 named volume 的 backup/restore 操作；不得讓任何 stack 掛載另一個環境的 volume。
- [x] 已將 root Compose 拆為 deployment-safe base 與 dev、staging、prod override：base/staging/prod 不得含 `build:` 或 phpMyAdmin，僅 dev 可 build 並公開開發 ports；三種組合均已通過 `docker compose config --quiet` 靜態驗證。
- [x] 已為 backend/frontend 分別發布 private GHCR `linux/arm64` image，並以不可變 digest 建立 release manifest；staging 已使用 manifest 部署，production workflow 會使用同一份 digest，PR 不會發布 image。
- [ ] 建立兩個 Cloudflare Tunnel 與公開 hostname：`staging-shop.albertkingdom.com`、`shop.albertkingdom.com`；分別以常駐 `cloudflared` service 轉送到 staging/prod 的 loopback frontend port；不設定 router port forwarding。
- [x] 已建立並驗證 staging `shopping-staging` Tunnel 的 Mac mini connector、使用者 LaunchAgent、DNS record 與 Cloudflare Access application；Access 僅允許 GitHub 登入且 email 為 `albertkingdom@gmail.com`，未登入請求會被導向 Access。staging origin 已完成第一次 CI image deploy 與 loopback smoke test；production Tunnel、DNS 與 Access 尚未建立。
- [x] 已在 Mac mini 安裝 `shopping-deploy-mac-mini` GitHub Actions self-hosted runner（`self-hosted`、`macOS`、`ARM64`、`shopping-deploy`），並以使用者 LaunchAgent 常駐；PR job 仍只使用 GitHub-hosted runner。
- [x] 已透過 SSH 確認 Mac mini 為 `arm64`；CI 必須發布 `linux/arm64` 或 multi-architecture backend/frontend image。
- [x] 已將 Mac mini 的 `podman-machine-default` 調整為 6 GiB RAM，並確認既有 container 已恢復且 backend health check 為 `healthy`。
- [x] 已新增 `application-dev.properties`、`application-staging.properties`、`application-prod.properties`，只放非敏感 CORS 與 proxy 行為設定。
- [x] 已將 `CloudinaryService` 寫死的 `shopping-website` folder 改為必要的 `app.cloudinary.upload-folder` 設定；dev、staging、prod 分別固定為 `shopping-website/dev`、`shopping-website/staging`、`shopping-website/prod`，並以 mock-based Service unit test 驗證 upload option 與非法 folder 拒絕。已更新 env example 說明暫時共用的 Cloudinary credential。
- [ ] 完成 `dev-only-demo-seed.md`，確保 demo 資料只能在 dev profile 建立。
- [x] 已更新 `.env.example` 與新增 `deploy/env/{staging,prod}.env.example` 純 placeholder 範本。
- [x] 已新增 `docker-compose.test.yml` 與 README 測試命令；已以 Docker Java 21 和 temporary MySQL 執行 `./mvnw test` 與 `./mvnw verify`，最近一次 verify 為 38 tests、0 failures、0 errors，並已清除 test project 資源。
- [x] 已在 Mac mini 依範本建立 Git ignore 的 staging env file，使用獨立 MySQL/JWT secret、暫時共用 Cloudinary credential，且權限為 `600`；image digest 由 deploy workflow 的 release manifest 暫時注入。
- [ ] 在 Mac mini 建立 production env file，必須使用與 staging 不同的 MySQL/JWT secret；Cloudinary credential 目前可暫時共用，但 production upload folder 必須維持 profile 強制隔離。
- [ ] 停止 production 對外發布 MySQL、Spring 與 phpMyAdmin ports，改由 Nginx/HTTPS 作唯一入口。
- [ ] 以 GitHub Actions 作為唯一正式 CI：執行 Maven test/verify、補齊 MySQL integration test、失敗阻擋 image publish 與部署；將 GitLab CI 移除或改為不具部署權限的鏡像流程。
- [x] 已使用不可變 image digest，建立 staging deploy、loopback smoke test 與 production promote workflow；staging 已完成實際部署驗證。
- [x] 已決定 deployment trigger policy：合併至 `master` 後自動部署 staging；annotated release tag 觸發 production deploy，且必須通過 GitHub Environment approval。
- [x] 已在 GitHub 建立 `staging`、`production` Environment：staging 限制 `master`；production 限制 `v*` tag、設定 required reviewer，並停用 administrator bypass。
- [x] 已確認 GitHub Actions 預設 `GITHUB_TOKEN` 為 read/write；workflow 另明確宣告所需的最小 `contents: read`、`packages: write` 或 `packages: read` 權限。
- [x] 已加入 deploy workflow source：只接受 release manifest 的兩個 digest，runner 必須使用 `shopping-deploy` 標籤，production job 綁定 GitHub `production` Environment；staging image publish、env file 與 smoke test 均已就緒並通過。
- [x] 已建立並在 staging 驗證 loopback smoke test；檢查 frontend 與 `/api/products`，不得使用浮動 tag。
- [ ] 建立 manifest-based rollback workflow；不得刪除 volume 作為 rollback 手段。
- [ ] 撰寫部署與 rollback 操作文件；執行 `./mvnw test`、`./mvnw verify` 與 staging smoke test 後更新本規格。
