# 規格治理

## ADDED Requirements

### Requirement: Repository 使用 OpenSpec artifacts 作為規格 source of truth

Repository SHALL 將目前系統行為保存在 `openspec/specs/` 下的 capability specs，並將每個 planned change 的 proposal、delta、design、tasks 與 verification evidence 一起保存在 `openspec/changes/<change-name>/`。

#### Scenario: 規劃新 feature

- **WHEN** change 修改 product behavior、API、data、authorization、UI 或 deployment behavior
- **THEN** change 從 `openspec/changes/<change-name>/` 開始，delta 以受影響的 capability spec 為基礎撰寫，且 proposal 與 tasks 可 review 前不得開始 implementation

### Requirement: Completed change 更新 living specifications

Repository SHALL 在 archive change 前將 accepted behavior change sync 至受影響的 living capability specs，同時保留 archived change 作為歷史追蹤。

#### Scenario: Change 通過 verification

- **WHEN** implementation 與 acceptance evidence 完成
- **THEN** accepted delta 同步至 `openspec/specs/`，verification record 標示 evidence，完整 change 歸檔至 dated archive directory

### Requirement: Verification evidence 可追溯至 requirements

Repository SHALL 記錄 automated 或 manual acceptance evidence，使每個完成的 task 與 requirement 都能追溯至 command、test data、operation、expected result、actual result、environment 與 date。

#### Scenario: Reviewer 檢查完成狀態

- **WHEN** reviewer 讀取 change 的 tasks 與 verification record
- **THEN** 每個已勾選 task 都有對應 evidence，不得只因規劃或 partial implementation 就勾選 task

### Requirement: Completed feature change 接受 independent code review

Repository SHALL 要求修改 application code 的 completed feature，在 delta sync、change archive 或開啟 pull request 前接受 independent code review。

#### Scenario: Feature 準備 review

- **WHEN** implementation 與受影響 tests 完成
- **THEN** reviewer 依 architecture、security、API、data、frontend、test 與 acceptance rules 檢查 change，並將結果記錄在 `code-review.md`

### Requirement: Code review findings 透過已驗證的 remediation 追蹤

Repository SHALL 以 stable id、severity、fix status 與 verification evidence 追蹤每個 code review finding；blocking findings 在對應 fix 驗證前必須保持 open。

#### Scenario: Blocking finding 被修正

- **WHEN** reviewer 找到 P0 或 P1 defect，且 implementation 已修正
- **THEN** 只有在相關 tests 或 manual checks 通過，且 review record 包含 fix evidence 後，才可勾選該 finding

### Requirement: Review 發現的 planning changes 必須明確記錄

當 review finding 顯示原始 intent、behavior contract、architecture 或 task plan 不足時，Repository SHALL 在套用 fix 前更新 active change artifacts。

#### Scenario: Review 使原始規劃失效

- **WHEN** finding 需要變更 scope、API、data、authorization、UI flow 或 design decisions
- **THEN** 更新 active proposal、delta specs、design 與 tasks，將 revision 記錄在 `code-review.md`，並在繼續 code changes 前重新執行 pre-implementation review
