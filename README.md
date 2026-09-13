## 購物網站 side project

此 repository 同時包含 Spring Boot API 與 `frontend/` React 前端；舊前端 repository 僅保留為歷史來源。

舊的展示連結：https://reurl.cc/85DVxy（歷史展示資訊，不代表本專案目前的部署環境）

## 功能
### 前台
- 註冊帳號
- 登入
- 購物車、下訂單
### 後台
- 平台管理員授予與撤銷商家角色
- 商家上架、下架與修改自己的商品
- 商家只檢視含有自己商品的訂單項目與本店小計

### 權限管理
- `ROLE_ADMIN`：平台全站管理員，可管理商家資格與既有全站管理功能。
- `ROLE_SELLER`：經平台管理員授予後，可使用商家中心；只能管理自己商品與讀取自己訂單項目。
- `ROLE_USER`：一般買家；不能進入平台或商家後台。

商家新增／更新／刪除商品使用既有 `/api/products/**`；後端從登入者決定商品 owner。商家清單與訂單必須使用 `/api/seller/products`、`/api/seller/orders`，不能以公開或 admin API 在前端過濾。目前行為以 [Living specifications](openspec/specs/README.md) 為準；[多商家舊規格](docs/specs/multi-seller-access.md) 僅供歷史參考。

## 開發文件入口

本專案使用 OpenSpec 管理目前系統規格與進行中的變更。開始新功能前，先閱讀 [OpenSpec 工作流程](openspec/README.md) 與 [Living specifications](openspec/specs/README.md)，再建立 `openspec/changes/<change-name>/`。不要再建立 `docs/specs/<feature-name>.md`；完成驗收後將 delta 同步回 living specs 並 archive change。

## 使用技術
- java spring boot框架
- 前後端使用RESTful API溝通
- MySQL資料庫存取
- spring security進行權限管理，使用jwt進行身份驗證(Authorization)

## 環境與啟動方式

staging 與 production 均由 GitHub Actions 發布 private GHCR arm64 image，並在 Mac mini 以獨立資料庫、私密環境檔與 Cloudflare Tunnel 部署。請勿將本機 `.env` 或 Docker volume 當作正式環境。

### 目前：本機整合環境

先依 `.env.example` 建立未提交的 `.env.dev`，填入本機 MySQL、Cloudinary 與 JWT 設定後，於 repository 根目錄執行：

```bash
docker compose --env-file .env.dev -p shopping-dev \
  -f docker-compose.yml -f docker-compose.dev.yml up --build
```

這會啟動 MySQL、Spring Boot、phpMyAdmin 與前端 Nginx，且 ports 都只綁定在本機。MySQL 資料保存在 `shopping-dev` Compose project 的 named volume；執行相同參數的 `docker compose down` 會保留資料，加入 `-v` 才會刪除這個本機資料庫。

### 使用 Docker 執行 Maven 測試

本機不需要切換系統預設 Java。下列指令會以 Java 21 container 和暫存 MySQL 執行 `./mvnw test`；測試資料不會使用 dev、staging 或 production database。

```bash
docker compose -p shopping-test -f docker-compose.test.yml \
  up --abort-on-container-exit --exit-code-from test
docker compose -p shopping-test -f docker-compose.test.yml \
  down --volumes --remove-orphans
```

若修改啟動、JPA mapping、Flyway migration 或 Spring context，改用 `MAVEN_GOAL=verify` 執行同一組指令。

### 目標：三個隔離環境

| 環境 | 用途 | Spring profile | 資料與憑證 |
| --- | --- | --- | --- |
| `dev` | 本機開發與 demo | `dev` | `shopping_dev`、本機 volume；demo seed 將在後續實作 |
| `staging` | Mac mini 上的部署前驗收 | `staging` | 專屬 MySQL database、volume 與 secret |
| `prod` | Mac mini 上的正式服務 | `prod` | 專屬 MySQL database、volume 與 production secret |

profile 只控制 Spring 行為；資料庫隔離另由不同 MySQL、資料庫名稱、帳號與 Docker Compose project/volume 達成。三個環境絕不可共用 JWT secret、MySQL credential 或 Docker volume。Cloudinary 目前是明確的暫時例外：staging/prod 共用既有帳號與 credential，但 profile 會將新上傳檔案固定到不同 upload folder（`shopping-website/staging`、`shopping-website/prod`）。

目前 dev profile 已建立，但 demo seed 尚未實作；因此新資料庫只會套用正式 schema 與必要角色，不會自動建立展示帳號或商品。

### 部署生命週期

Pull request 只會執行 CI，不會部署。合併至 `master` 後，通過 CI 的 image 會自動部署到 staging；驗收完成後，在同一個 `master` commit 建立 annotated tag（例如 `v1.2.3`）來指定 production 版本。tag 觸發的 production workflow 必須先通過 GitHub `production` Environment 的 required reviewer 核准，並使用該 commit 已發布的相同 image digest，不會重新 build。

用不同 Compose override 與 env file 選擇環境：

```bash
# 本機開發
docker compose --env-file .env.dev -p shopping-dev \
  -f docker-compose.yml -f docker-compose.dev.yml up --build

# staging 主機（Mac mini，以 Podman 執行）
PATH=/opt/homebrew/bin:$PATH podman compose \
  --env-file /Users/yklin/services/shopping-website/staging/staging.env -p shopping-staging \
  -f docker-compose.yml -f docker-compose.staging.yml up -d

# production 主機（Mac mini，以 Podman 執行）
PATH=/opt/homebrew/bin:$PATH podman compose \
  --env-file /Users/yklin/services/shopping-website/prod/prod.env -p shopping-prod \
  -f docker-compose.yml -f docker-compose.prod.yml up -d
```

將 `deploy/env/staging.env.example` 與 `deploy/env/prod.env.example` 分別複製到 Mac mini 文件路徑後再填值。staging/prod Compose 明確拒絕 `build:`，只能使用 CI 發布且以 digest 鎖定的 `SPRING_IMAGE`、`FRONTEND_IMAGE`。GitHub-hosted runner 驗證並發布 image；Mac mini 的 self-hosted runner 只負責拉取已驗證 image 與部署，禁止讓 PR workflow 在該 runner 執行。staging 與 prod 使用 Cloudflare Tunnel 提供 HTTPS，不需固定 IP 或 router port forwarding；先部署並驗收 staging，再升級 production。不得將 MySQL、Spring `8080` 或 phpMyAdmin 對公網發布。
