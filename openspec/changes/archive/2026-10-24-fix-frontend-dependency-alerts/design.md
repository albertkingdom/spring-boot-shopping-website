## Context

2026-10-24 查核 npm audit、npm view 與現有 npm ls。直接入口為 react-scripts@5.0.1。

| 套件 | 目前版本 | 修補目標 | 來源 |
| --- | --- | --- | --- |
| proxy-addr | 2.0.7 | 2.0.8 | react-scripts → webpack-dev-server@5.2.6 → express@4.22.2 |
| launch-editor | 2.14.1 | 2.14.2 | react-scripts → webpack-dev-server@5.2.6 |
| brace-expansion | 1.1.18 | 1.1.21 | react-scripts → eslint@8.23.1 → minimatch@3.1.5 |
| brace-expansion | 2.1.4 | 2.1.7 | react-scripts → workbox-webpack-plugin → workbox-build → @trickfilm400/rollup-plugin-off-main-thread → ejs → jake → filelist → minimatch@5.1.9 |
| brace-expansion | 5.0.9 | 5.0.12 | react-scripts → workbox-webpack-plugin → workbox-build → glob@11.1.0 → minimatch@10.2.6 |

公告來源：
- https://github.com/advisories/GHSA-jqcg-44mw-7w3h：critical，>=1.1.0 <2.0.8。
- https://github.com/advisories/GHSA-587h-7wmj-h5pm：high，launch-editor <=2.14.1；2.14.2 也處理 GHSA-698v-4r3x-3vcv 與 GHSA-r825-w8mm-5pxw。
- https://github.com/advisories/GHSA-qhr7-859c-m2p7：high，brace-expansion <1.1.20、>=2 <2.1.6、>=4 <5.0.11。
- https://github.com/advisories/GHSA-6j4f-fj2g-mc7p：high，<1.1.19、>=2 <2.1.5、>=4 <5.0.10。
- https://github.com/advisories/GHSA-q2hr-2g5m-vwhr：moderate，<1.1.21、>=2 <2.1.7、>=4 <5.0.12。

## Goals / Non-Goals

維持各 major 與既有 CRA patch 相容；不進行 CRA migration 或全面 npm audit fix。

## Decisions

使用 npm update proxy-addr launch-editor brace-expansion --package-lock-only，檢查 diff 僅含指定套件與必要依賴。不強制跨 major、不新增多餘 overrides。保留完整 audit 摘要與其他漏洞清單，避免將目標警示清除誤稱為整體安全。

驗收項目 V1：npm ci（包含 patch-package）成功；V2：npm ls 與 audit 確認所有目標副本修補且目標警示消失；V3：CI=true npm test -- --watchAll=false --runInBand 與 npm run build 成功；V4：development server 編譯成功且 HTTP / 回傳 200；V5：OpenSpec strict validation 與 project code review 完成。無新增業務邏輯，沿用既有前端測試。後端未變動，不需執行 Maven。

## Risks / Trade-offs

開發伺服器與 build 工具被影響 → 驗證 start、build 及 postinstall patch。Windows 漏洞由上游修補，本機 macOS 不宣稱 Windows exploit 重現。其餘 audit 漏洞另案處理。

## Migration Plan

以 npm ci 套用 lockfile；無 schema 或環境變數變更。回滾需還原本次 lockfile，但將重新引入漏洞。此次保留 active change，不執行 archive／PR／部署。

## Pre-implementation review

2026-10-24：已讀 AGENTS.md、相關 living specs、既有 patch、CI 與架構清單。檢查範圍、版本相容性與 V1–V5；無未決規劃問題。現有系統契約不變，skip_specs 合理。

## 收尾決議（2026-10-24）

使用者要求收尾，完成最終驗收後歸檔本 change；先前保留 active change 的規劃至此結束。無 delta specs，不需 sync；其餘範圍與已知限制不變。
