## 1. 調查與修補

- [x] 1.1 記錄目前版本、依賴來源與官方修補範圍，以 design.md 及 audit 前後比對驗證
- [x] 1.2 定向更新 lockfile，確認 diff 未包含無關升級，並通過 npm ci（V1）及 npm ls／audit（V2）

## 2. 驗證與交付

- [x] 2.1 執行既有前端測試及 production build，記錄結果（V3）
- [x] 2.2 執行 development server 編譯與 HTTP smoke test（V4）
- [x] 2.3 完成 verification.md、project code review 與 OpenSpec strict validation（V5）
