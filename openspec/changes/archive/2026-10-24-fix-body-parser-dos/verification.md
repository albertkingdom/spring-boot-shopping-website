# 驗收證據

日期：2026-10-24（Asia/Taipei）。基底 origin/master `fb56c71050745a9d3fb71450fa43c230ac5be37d`；分支 `fix/body-parser-dos`。macOS arm64、Docker 29.8.2、node:20-alpine（Node 20.20.2）、eclipse-temurin:21-jdk、MySQL 8.0。

## Requirement／Scenario 對應

Requirement「壓縮請求不得終止前端工具鏈程序」的兩個 Scenario 均通過：

| Scenario | 命令／資料與步驟 | 預期／實際結果 |
|---|---|---|
| 乾淨安裝修補版本 | 隔離目錄複製 frontend/package.json、package-lock.json、patches、src、public，以 node:20-alpine 執行 `npm ci --no-audit --no-fund`、`npm ls body-parser` | 安裝成功；react-scripts@5.0.1 → webpack-dev-server@5.2.6 → express@4.22.2 → body-parser@1.20.9，無其他版本 |
| 乾淨安裝修補版本 | 同一容器工作目錄執行 `CI=true npm test -- --watchAll=false --runInBand` | 9 suites、28 tests 通過，0 failed |
| 乾淨安裝修補版本 | `npm run build` | exit 0，產生 production build；有既有 ESLint warnings，詳見限制 |
| 正常與異常壓縮輸入 | 於前端安裝工作目錄以 Node 20 執行 `node <change>/checks/compressed-body.cjs`；JSON 為 `{"probe":"body-parser"}`，gzip 後附加 `invalid trailing bytes`，僅 loopback、chunked transfer | 正常 200／異常 400／後續正常 200；body-parser 1.20.9；exit 0；無 uncaughtException handler 掩蓋錯誤 |
| 正常與異常壓縮輸入（工具鏈相容性） | `BROWSER=none HOST=127.0.0.1 npm start`，使用 Node http GET 輪詢 `http://127.0.0.1:3000` | dev server 成功啟動，HTTP 200，驗收後終止容器 |

## 執行方式

依賴更新於 `/private/tmp/body-parser-fix` 執行：

```sh
docker run --rm -v "$PWD/frontend:/source:ro" -v /private/tmp/body-parser-fix:/work -w /work node:20-alpine sh -ec 'cp /source/package.json /source/package-lock.json .; npx --yes npm@8 update body-parser --package-lock-only --ignore-scripts --no-audit --no-fund'
```

檢查差異後只複製 package-lock.json 回 repository。packages 與 legacy dependencies 的 version／resolved／integrity 各三行更新，lockfileVersion 維持 2，package.json 不變。使用上述暫存安裝目錄，驗收命令可重現如下（change 歸檔後改用 archive 路徑）：

```sh
docker run --rm -v /private/tmp/body-parser-fix:/work -v "$PWD/openspec/changes/fix-body-parser-dos/checks:/checks:ro" -w /work node:20-alpine node /checks/compressed-body.cjs
```

## 後端完整驗證

```sh
MAVEN_GOAL=verify docker compose -p shopping-test -f docker-compose.test.yml up --abort-on-container-exit --exit-code-from test
docker compose -p shopping-test -f docker-compose.test.yml down --volumes --remove-orphans
```

實際 exit 0，87 tests、0 failures／errors／skips，BUILD SUCCESS；verify 包含 test lifecycle。容器、兩個 MySQL 測試資料環境及 network 已清理。未操作真實 Cloudinary。

## 靜態檢查與限制

- `git diff --check` 通過；完整 lockfile diff 僅 body-parser 的兩份紀錄；無 API、schema、授權、金額或後端程式變更，無新 secret 或敏感 log。
- npm 安裝顯示既有 @testing-library/jest-dom 7.0.1 要求 Node >=22 的 engine warning；本專案現有 Node 20 build/test 均成功，本次未調整 runtime。
- CRA/Babel maintenance 警告與五個既有頁面的 useEffect dependency ESLint warnings 未在本次擴大修正。
- middleware 測試覆蓋本次輸入情境，不代表涵蓋所有壓縮格式或所有 dev-server route。
- 其他 Dependabot alerts 不在本次範圍，未宣稱 npm audit 無漏洞。
- 未執行 staging／production 部署或遠端更新；GitHub #160 須修補合併預設分支並重新掃描後才會關閉。

## OpenSpec verify report

- Completeness：4/4 tasks、1 Requirement、2 Scenarios 有實作及證據。
- Correctness：lockfile 1.20.9；乾淨安裝／測試／build 與 gzip 200/400/200 對應兩個 Scenarios。
- Coherence：沿用相容範圍、lockfile v2、Node 20 隔離安裝與既有 Compose，符合 design。
- CRITICAL／WARNING／SUGGESTION：無本次變更問題；既有工具鏈限制見上文。
- project-code-review 結果 approved；living spec 已與 delta 逐段比對一致，README 已加入索引。
- `openspec validate --all --strict`：8 passed、0 failed（包含既有 migrate-spec-architecture change）；`git diff --check` 通過。
- 可歸檔；未修改其他 active change。
