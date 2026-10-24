## 1. 依賴修補

- [x] 1.1 更新 lockfile 至 body-parser 1.20.9，確認 v2 雙份記錄一致、無無關版本更新，npm ci 與 npm ls 成功

## 2. 驗證與交付

- [x] 2.1 執行正常與尾端異常 gzip 請求驗收，確認後續請求仍成功；確認 dev server 啟動
- [x] 2.2 執行前端完整測試、production build 與 Compose Maven verify，清理測試資源並記錄 verification.md
- [x] 2.3 完成 project-code-review、OpenSpec verify、strict validate 與 living spec 同步；保留完整驗收紀錄，歸檔由完成後 workflow 執行
