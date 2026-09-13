# OpenSpec 文件與工作流程

本 repository 使用 OpenSpec 管理「目前系統契約」與「進行中的變更」。OpenSpec 的 change 以資料夾保存 proposal、delta specs、design 與 tasks；完成後將變更歸檔，並把已實作的 delta 同步回 living specs。

## 目錄責任

```text
openspec/
├── config.yaml                         # 專案 context、artifact rules 與 operation guidance
├── specs/                              # 目前系統的 living source of truth
│   ├── README.md
│   └── <capability>/spec.md
└── changes/
    ├── <change-name>/                  # 進行中的變更
    │   ├── proposal.md                 # 為什麼做、範圍與非目標
    │   ├── specs/<capability>/spec.md  # 相對於 living specs 的 delta
    │   ├── design.md                   # 如何做、取捨、風險與 rollout
    │   ├── tasks.md                    # 可勾選的實作清單
    │   ├── verification.md             # 本專案的驗收證據記錄（完成實作後建立）
    │   └── code-review.md              # 本專案的獨立 code review 與 finding 修復紀錄
    └── archive/YYYY-MM-DD-<name>/      # 已完成且保留歷史脈絡的 change
```

`openspec/specs/` 描述現在系統應該提供什麼；`openspec/changes/` 描述一次變更預計如何改變它。兩者不能互相取代，也不應把同一份需求複製到多個 active change。

## 一般新功能流程

```text
Discuss / Explore
      ↓
`$openspec-new-change` or `$openspec-ff-change`
      ↓
proposal.md → specs/ + design.md → tasks.md
      ↓
Pre-implementation review
      ↓
`$openspec-apply-change`
      ↓
test / browser acceptance / verification.md
      ↓
project-code-review / code-review.md
      ↓
official openspec-verify-change skill
      ↓
`$openspec-sync-specs` → `openspec archive <name>`
```

CLI 指令（由目前安裝的 OpenSpec CLI 執行）：

```bash
openspec new change <change-name>
openspec status --change <change-name>
openspec instructions apply --change <change-name> --json
openspec validate --all --strict
openspec archive <change-name>
```

工作流程 skill（由 Codex 依序執行）：

```text
$openspec-new-change <change-name>
$openspec-continue-change <change-name>
$openspec-apply-change <change-name>
$openspec-verify-change <change-name>
$openspec-sync-specs <change-name>
```

若要一次建立所有規劃 artifacts，可使用 `$openspec-ff-change`；若只需查看或思考問題，可使用 `$openspec-explore`。`apply` 與 `sync` 在目前 CLI 中不是 top-level command，應使用上方列出的 project-local workflow skills。

若 OpenSpec CLI 尚未提供某個 wrapper，仍須依同樣的 artifact 順序手動完成，不得跳過 proposal、可驗證的 requirements、tasks 或驗收證據。驗收分析由 `.agents/skills/openspec-verify-change` 執行；它是 report-only，不會代替測試，也不會自動修改或歸檔 change。CLI `validate` 只負責 artifact 結構驗證。

## 驗收規則

- `specs/<capability>/spec.md` 的 Requirement 是行為契約，Scenario 是可執行或可明確人工驗證的情境。
- `tasks.md` 是進度清單，不是需求文件；只有實作與相應證據完成後才能勾選。
- `verification.md` 記錄測試命令、測試資料、操作步驟、預期與實際結果、環境、日期及已知限制。
- 涉及程式碼的 feature 在 sync、archive 或 PR 前必須完成 project code review；結果記錄於 `code-review.md`，P0/P1 finding 必須關閉。
- 若 code review 發現原本規劃有問題，先更新 proposal、delta specs、design 與 tasks，重新進行 pre-implementation review，再修正程式碼。
- 驗收條文應能回溯到 Requirement／Scenario；測試名稱或人工步驟應能回指對應條文。
- 需求、API、資料模型、權限、環境或 UI 流程改變時，先修改 active change artifacts，再修改程式碼。

## 舊文件相容與遷移

`docs/specs/` 是 migration 前的 legacy 位置：

- `docs/specs/current-system.md` 已改為入口轉址，新的目前系統描述在 `openspec/specs/`。
- `docs/specs/multi-seller-access.md`、`environment-separation.md` 與 `dev-only-demo-seed.md` 保留作歷史參考，不再作為新變更的 source of truth。
- 新 feature 不應再建立 `docs/specs/<feature-name>.md`；技術主題文件可繼續放在 `docs/`。
- 完成 migration 後，legacy 文件只有在已被 living specs 與 archive 完整承接後才可移動或刪除。
