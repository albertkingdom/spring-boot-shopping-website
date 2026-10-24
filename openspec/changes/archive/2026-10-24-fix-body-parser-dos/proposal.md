## Why

Dependabot #160（GHSA-27r3-jxmh-p828 / CVE-2026-111181）指出 body-parser 1.20.8 處理壓縮請求尾端資料時可能產生未捕捉例外並終止 Node 程序，嚴重度 High。

## What Changes

- 將 frontend lockfile 中的 body-parser 升級至 1.20.9，沿用 express 的 ~1.20.5 範圍。
- 記錄依賴治理契約、安裝／測試／建置與壓縮請求驗證證據。
- 不修改 UI、API、資料模型或後端框架；其他 Dependabot 警告不在本次範圍。

## Capabilities

### New Capabilities

- `build-dependency-security`: 本次前端工具鏈 body-parser 安全版本與壓縮請求處理契約。

### Modified Capabilities

無。

## Impact

僅影響 frontend/package-lock.json 與 OpenSpec 文件。開發伺服器透過 react-scripts → webpack-dev-server → express 使用 body-parser；production 靜態產物不執行此 Node middleware。沿用 1.x patch 相容性，不新增 override。以 npm ci 重現修補；回復 lockfile 會重新引入漏洞，僅限必要時暫停開發服務後進行。無部署、commit、push 或 PR 操作。
