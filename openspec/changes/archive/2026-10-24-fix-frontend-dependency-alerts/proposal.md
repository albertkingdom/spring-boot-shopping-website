## Why

frontend/package-lock.json 鎖定的 proxy-addr、launch-editor 與 brace-expansion 命中 critical／high 公告，需更新開發與建置工具鏈的間接依賴。

## What Changes

- 依 npm audit 與 GitHub Reviewed 公告，確認來源、受影響範圍及各 major 修補版本。
- 僅更新指定間接套件及必要相依項目，保留 react-scripts 與既有 patch、overrides。
- 驗證乾淨安裝、前端測試、production build 與 development server，記錄剩餘風險。

## Capabilities

### New Capabilities

無。本次為 tooling dependency maintenance，使用 skip_specs: true；不新增 capability。

### Modified Capabilities

無。API、授權、資料模型、UI 流程及部署契約均未改變，living specs 不需同步。不是 architecture migration，不建立無行為變化的 architecture requirement。

## Impact

僅 frontend/package-lock.json 與本 change 文件；若現有 semver ranges 可容納修補版則不新增 overrides。不升級框架、不修補其他套件、不執行外部發布。更新後以 npm ci 使用；回滾 lockfile 會重新引入漏洞，應優先向前修正。無資料庫 migration。ARCHITECTURE_TODO.md 無本次可勾選的新項目。
