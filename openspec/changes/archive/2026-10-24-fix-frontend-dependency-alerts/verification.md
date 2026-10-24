# 驗收紀錄

- 日期：2026-10-24（Asia/Taipei）
- 環境：macOS Darwin arm64；Node v26.7.0、npm 11.19.0；frontend 工作目錄；branch `fix/frontend-dependency-alerts`
- 基準：`2f136e8201218b08a4ae24d44b89987f797d7f36`；開始時工作目錄乾淨。
- 測試資料：repository 既有 9 個 Jest suites／mock fixtures；無真實後端、帳號或第三方資源。
- 本次無行為 delta／Requirement／Scenario；以 design.md 的 V1–V5 作驗收追蹤，不修改 living specs。

## 調查與操作

1. `npm ls proxy-addr launch-editor brace-expansion --all` 確認 design.md 的來源與舊版本。
2. `npm audit --json --cache /tmp/shopping-npm-cache` 取得前後報告；`npm view proxy-addr version`、`npm view launch-editor version`、`npm view brace-expansion version` 查得 2.0.8、2.14.2、5.0.12，並以 GitHub Reviewed 公告交叉核對各 major 修補門檻（見 design.md）。
3. `npm update proxy-addr launch-editor brace-expansion --package-lock-only --ignore-scripts --cache /tmp/shopping-npm-cache` 成功。完整 lockfile diff 僅 5 個套件節點及 v2 相容 dependencies 記錄；launch-editor 的 shell-quote range 從 ^1.8.4 改為 ^1.10.0，但既有 shell-quote 版本未改。

## 預期／實際結果

| 項目 | 命令／步驟 | 預期 | 實際 |
| --- | --- | --- | --- |
| V1 | `npm ci --cache /tmp/shopping-npm-cache` | 可重現安裝、patch 成功 | exit 0；1503 packages；patch-package 8.0.1 套用 react-scripts@5.0.1 成功 |
| V2 | `npm ls proxy-addr launch-editor brace-expansion --all` | 所有副本修補 | exit 0；proxy-addr 2.0.8、launch-editor 2.14.2、brace-expansion 1.1.21／2.1.7／5.0.12 |
| V2 | `npm audit --json --cache /tmp/shopping-npm-cache`；比較 vulnerabilities keys | 三個目標均消失 | 三個 keys 皆不存在；無新增漏洞套件；audit exit 1 原因為其他漏洞仍存在 |
| V3 | `CI=true npm test -- --watchAll=false --runInBand` | 既有測試通過 | exit 0；9/9 suites、28/28 tests，0 snapshots |
| V3 | `npm run build` | production bundle 建置成功 | exit 0；Compiled with warnings，產生 build |
| V4 | `HOST=127.0.0.1 PORT=3107 BROWSER=none npm start` | development server 成功編譯 | webpack compiled with 1 warning；既有 CRA patch 與 webpack-dev-server 5 相容 |
| V4 | `curl --fail --silent --output /tmp/shopping-dev-index.html --write-out '%{http_code}\n' http://127.0.0.1:3107/` | HTTP 200 | exit 0、200；驗證後停止本次 server |
| V5 | `openspec validate fix-frontend-dependency-alerts --strict`、`openspec validate --all --strict` | 結構合法 | 目標通過；全 repo 7 passed／0 failed |
| V5 | `git diff --check` 與 project-code-review | 無格式錯誤、無未解 finding | 通過；詳見 code-review.md |

## Audit 前後

| | low | moderate | high | critical | total |
| --- | --- | --- | --- | --- | --- |
| 更新前 | 1 | 30 | 5 | 2 | 38 |
| 更新後 | 1 | 30 | 3 | 1 | 35 |

剩餘 high：body-parser、follow-redirects、source-map-js；critical：shell-quote。皆已存在於基準，不屬本次指定三個套件修補範圍，建議另案處理。其他 moderate／low 套件（含傳遞影響）如下：

@csstools/postcss-cascade-layers, @csstools/postcss-is-pseudo-class, @csstools/selector-specificity, css-blank-pseudo, css-has-pseudo, css-minimizer-webpack-plugin, cssnano, cssnano-preset-default, fast-uri, postcss-attribute-case-insensitive, postcss-calc, postcss-custom-selectors, postcss-dir-pseudo-class, postcss-focus-visible, postcss-focus-within, postcss-merge-longhand, postcss-merge-rules, postcss-minify-selectors, postcss-modules-local-by-default, postcss-modules-scope, postcss-nested, postcss-nesting, postcss-preset-env, postcss-pseudo-class-any-link, postcss-selector-not, postcss-selector-parser, postcss-unique-selectors, react-scripts, serialize-javascript, stylehacks, tailwindcss

## 已知限制

- npm registry／localhost listen 首次受 sandbox DNS／EPERM 限制，授權重試成功；不是套件失敗。
- 本機 Node 26；repository Dockerfile 使用 Node 20。本次未執行 Docker image／Node 20 CI 驗證，不能代替 CI。
- install 顯示 core-js、core-js-pure、fsevents 的 npm allowScripts 提示；本專案 postinstall patch 已實際成功。
- build／start 顯示既有來源的 5 個 react-hooks/exhaustive-deps 警告（AdminOrderList、AdminProductPage、OrderListPageForSeller、ProductPageForSeller、SellerRoleManagement）與 babel-preset-react-app 未宣告 plugin 警告。Jest 顯示 open-handle 提示，仍以 exit 0 結束。本次不修改其來源。
- 未重現 Windows exploit；以正式修補版本與 audit 驗證。未執行完整瀏覽器／後端／staging 驗收；未變更相關產品契約，故不執行 Maven。
- Dependabot 遠端警示需變更合併與 GitHub 重新掃描後才會反映；本次未 commit、push 或操作警示狀態。
- 原始測試 log／audit JSON 暫存於 /tmp/shopping-*，本文件保留必要結果，不提交 build output。

- 停止開發伺服器時 CRA start.js 呼叫 webpack-dev-server 5 不存在的 devServer.close()，顯示 TypeError 並以 130 結束；此呼叫與既有 webpack-dev-server override／patch 均未在本次改動，屬既有相容性限制。編譯與 HTTP 驗收已完成，server process 已退出；另案可將 shutdown API 適配至 stop。

## OpenSpec 最終驗收與歸檔（2026-10-24）

依 openspec-verify-change skill，重新讀取 status、apply context、proposal、design、tasks 與 code-review，並比對目前 lockfile；本輪只修改收尾文件，不重跑已通過且未受新變更影響的前端測試。

| Dimension | 結果 |
| --- | --- |
| Completeness | 5/5 tasks 完成；planning artifacts done／skipped |
| Correctness | 5 個 lockfile package nodes 的修補版本皆符合 design；V1–V5 有實際證據 |
| Coherence | 無其他套件節點或產品程式碼變更；保留既有 overrides 與 patch |

CRITICAL：0；規格／設計偏差 WARNING：0；SUGGESTION：0。因 skip_specs: true，略過 Requirement／Scenario delta coverage 及 sync；既有環境與工具鏈限制仍如上，沒有新增驗證承諾。code-review approved，無未解 finding，本次範圍可歸檔。

使用者要求「收尾」，將本 change 歸檔至 openspec/changes/archive/2026-10-24-fix-frontend-dependency-alerts/，保留所有規劃及驗收歷史；不執行 commit、push、PR 或部署。

## 提交前補充驗證（2026-10-24）

使用者授權 commit、push 與部署後，補跑 `MAVEN_GOAL=verify docker compose -p shopping-test -f docker-compose.test.yml up --abort-on-container-exit --exit-code-from test`：Java 21／MySQL 8.0，87 tests、0 failures、0 errors、0 skipped，BUILD SUCCESS、exit 0。此 verify lifecycle 包含 test；隨後以 `docker compose -p shopping-test -f docker-compose.test.yml down --volumes --remove-orphans` 成功清除暫時資源。

Gitleaks image digest：`sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f`。將本次 lockfile 複製為 /tmp/shopping-secret-scan-input/frontend-lock.txt（避開預設忽略 lockfile），加上完整 change 文件，執行 `docker run --rm --volume /tmp/shopping-secret-scan-input:/scan:ro zricethezav/gitleaks:latest dir /scan --redact --no-banner`，掃描約 1.32 MB，exit 0、no leaks found。

已 fetch origin/master，與本次分支基準相同（ahead 0／behind 0）。部署透過 PR 與 CI 進行；實際結果依 GitHub workflow 紀錄，不以本次本機驗證宣稱已部署。
