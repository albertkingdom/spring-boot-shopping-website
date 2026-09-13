# Proposal: Migrate the project specification architecture to OpenSpec

## Intent

目前 feature spec、目前系統描述、驗收流程與 project-local SDD skill 混在 `docs/specs/` 與單一文件規則中。下一個功能開發時，工程師需要從多份歷史文件猜測目前行為，也無法清楚區分 living source of truth 與一次性的變更提案。

本次 migration 將 repository 改為 OpenSpec 的 change-folder 模型：`openspec/specs/` 保存目前系統契約，`openspec/changes/` 保存提案與 delta，完成後保存於 archive。這是文件與 agent workflow 的架構變更，不修改產品 runtime、API、資料庫 schema 或部署行為。

## Scope

包含：

- 以官方 OpenSpec CLI 初始化 repository，產生 Codex skills。
- 建立 `openspec/config.yaml`、living capability specs 與 OpenSpec 使用說明。
- 把目前系統的角色、商品、訂單、後台 UI 與環境邊界整理成 `openspec/specs/`。
- 建立本 migration 的 proposal、architecture delta、design、tasks 與驗收記錄；這個 delta 描述 repository 的規格治理契約，不是產品 runtime feature spec。
- 更新 `AGENTS.md`、README 與 legacy `docs/specs/` 入口，避免未來新增單一檔案 feature spec。
- 移除會與官方 OpenSpec workflow 互相衝突的舊 project-local SDD skill。
- 建立符合本專案分層、安全、測試與驗收規則的 code review skill，將 review findings 與修復進度獨立記錄在每個 change 的 `code-review.md`。

不包含：

- 修改 Spring Boot、React、Flyway、Compose、CI 或 deployment runtime。
- 把所有歷史 feature spec 逐字重寫成單一巨大文件。
- 刪除歷史文件；legacy 文件在確認 archive 與 living specs 完整承接前保留。
- 安裝全域 OpenSpec CLI 或改變開發者的使用者設定。

## Decisions

- 使用 OpenSpec 預設 `spec-driven` schema：`proposal → specs → design → tasks`。
- 目前系統規格按 capability 拆檔，讓下一個 feature 只需讀取受影響的 capability。
- Migration change 不建立舊格式的 product feature spec；為符合 OpenSpec change 的驗證契約，只建立描述規格治理本身的 architecture delta。
- 驗收條文放在 living／delta spec，實際測試與人工結果放在 change 的 `verification.md`。
- Code review 是實作後、sync／archive／PR 前的專案品質 gate；若 review 顯示規劃錯誤，必須先更新 artifacts 並重新 review，再繼續修正。
