## Context

見 proposal.md。express 4.22.2 的 body-parser 範圍為 ~1.20.5，可容納 1.20.9。lockfile 使用 v2，包含 packages 與 legacy dependencies 兩份記錄。frontend/Dockerfile 使用 Node 20；production 使用靜態資產伺服器。

## Goals / Non-Goals

保持 lockfile 格式與其他套件版本，驗證安裝、既有測試、build 及壓縮輸入。無 API、schema、授權或 Java 變更；不處理其他警告。

## Decisions

使用 npm 8（保留 lockfile v2）只更新 body-parser，無需額外 override；必要的傳遞依賴更新須逐項檢視。採用 Node 20 容器在暫存工作區安裝，避免改動使用者 node_modules。以獨立程序執行 loopback middleware 驗收，觀察回應與後續正常請求，避免 uncaughtException handler 掩蓋程序崩潰。使用既有 Compose 執行 Maven verify 並清理測試資源。

替代方案：全量 npm update 會擴張範圍；升級 body-parser 2.x 不符合 express 既有相容範圍；停用 inflate 會改變功能且需修改工具鏈。

## Risks / Trade-offs

- npm 可能更新無關節點 → 檢查完整 lockfile 差異並縮小更新。
- middleware 驗收未涵蓋所有 dev-server route → 同時驗證 dev server 啟動與既有前端測試/build。
- 其他既有安全警告仍存在 → 不宣稱整個 npm audit 無漏洞。

## Migration Plan

npm ci 後重啟開發服務。無資料 migration 或本次部署操作。必要回滾可還原本次 lockfile，但會重新引入漏洞，應停止受影響服務並優先重新修補。

## Pre-implementation review

2026-10-24：已核對官方 advisory、express 版本範圍、容器 Node 版本、lockfile v2 與驗收可追溯性，計畫可實作。無 ARCHITECTURE_TODO.md 對應項目。
