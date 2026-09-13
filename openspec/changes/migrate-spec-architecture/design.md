# Design: Migrate the project specification architecture to OpenSpec

## Context

本 repository 已有 `docs/specs/current-system.md` 草稿、`docs/specs/multi-seller-access.md` 與 `docs/specs/environment-separation.md`，也有一個自訂 `.codex/skills/project-sdd-workflow/`。這些文件可作為 migration 的來源，但其單一 feature spec 規則與 OpenSpec 的 change folder 模型不同。

## Goals / Non-Goals

Goals：

- 讓新工程師能從 `openspec/specs/README.md` 找到目前系統的能力邊界。
- 讓一個變更的 why、what、how、tasks 與驗收證據集中在同一個 change folder。
- 讓 Codex 使用 repository-local OpenSpec skills 時遵循本專案的繁中、測試、權限與文件規則。
- 保留舊文件的可追溯性，並清楚標示它們不再是 source of truth。

Non-goals：

- 不把 OpenSpec artifact 當成 runtime configuration。
- 不以文件 migration 順便修改既有應用程式缺陷或 deployment infrastructure。

## Decisions

### Decision: Use `openspec/specs/` as living source of truth

Capability specs 描述目前已實作且可由程式或測試確認的行為；active change 的 delta 只描述相對於它的預計變更。這避免把 `current-system.md` 與每份 feature spec 重複維護。這次 migration 的唯一 delta 是規格治理架構本身，不是產品 API 或 UI 行為。

### Decision: Use `openspec/changes/<name>/` for all new work

proposal、delta specs、design、tasks 與 verification 同一個資料夾，方便 review、平行工作與 archive。沒有產品行為的 workflow／架構 migration 仍以 architecture delta 記錄其治理契約，不另外建立舊格式的 feature spec。

### Decision: Keep acceptance evidence separate from requirements

Requirement／Scenario 定義「什麼算完成」；`verification.md` 記錄在什麼環境、用什麼資料、執行什麼命令與實際結果。這能避免把測試日誌塞回需求條文，也保留可稽核的驗收記錄。

### Decision: Keep code review findings separate from acceptance evidence

`code-review.md` 只記錄 reviewer 發現的問題、嚴重度、修正狀態與重新驗證結果；`verification.md` 只記錄功能測試與人工驗收證據。這讓 review 可以追蹤修復，而不會把需求、執行清單與 review 意見混在一起。

### Decision: Replan before fixing a planning defect

如果 review 顯示原本的 scope、API、資料模型、授權規則或 UI 流程不足，先更新 active change artifacts 並重新做 pre-implementation review，再改 code。若意圖已不同，建立新的 change，避免用 review 修正掩蓋需求變更。

### Decision: Prefer official generated skills over the legacy custom skill

`openspec init --tools codex` 產生的 skills 與 CLI schema 同步；舊 `project-sdd-workflow` 會要求所有需求放在 `docs/specs/<feature>.md`，與新架構衝突，因此移除。

## Risks / Trade-offs

- [Risk] 舊文件仍被直接連結或修改 → `docs/specs/README.md` 與 `current-system.md` 改為 migration notice，並在新入口列出 legacy mapping。
- [Risk] living specs 與程式行為漂移 → 每個 change 的 archive 前必須 sync delta，官方 `openspec-verify-change` skill 與測試／人工驗收列為完成條件。
- [Risk] OpenSpec CLI 版本差異 → `openspec/config.yaml` 保持最小且使用官方 `spec-driven` schema；README 明確區分 CLI inspection／validation commands 與 project-local workflow skills，避免把 `apply` 或 `sync` 當成不存在的 top-level CLI command。
- [Risk] code review findings 被口頭修正後遺失 → 每個 change 強制保留獨立 `code-review.md`，以 finding checkbox 與證據追蹤狀態。
- [Risk] deployment 現況與文件不一致 → deployment capability 明確要求 environment-specific target binding；實際 runner label 與機器對應仍需在部署文件／環境設定完成後驗證。

## Migration Plan

1. 在 migration branch 初始化 OpenSpec 與官方 Codex skills。
2. 建立 project config、living capability specs、入口文件與本 change artifacts。
3. 更新 `AGENTS.md` 與 README，改用 OpenSpec 目錄、實際 CLI commands 與 project-local workflow skills。
4. 將舊 `docs/specs` 標示為 legacy，不在本次刪除歷史內容。
5. 執行 OpenSpec validation、Markdown/link／YAML／stale-command 檢查，確認產品 source diff 只有文件與 skill。
6. 後續功能以 OpenSpec change 實際走完一次，再視需要 archive 舊文件。

## Open Questions

- 未來是否要在 CI 強制執行 OpenSpec CLI validation，待 repository 使用一次完整 feature change 後決定；本次先保留可手動執行的 validation command。
