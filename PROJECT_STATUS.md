# Project Status

本文件用於跨工作階段交接目前進度。開始新工作前，先閱讀 `AGENTS.md`、本文件與 `ARCHITECTURE_TODO.md`，再以實際 Git 與 GitHub 狀態核對；外部狀態可能在本文件更新後改變。

最後更新：2026-08-30（PR #8 合併後）

## Current Baseline

- 主要版本庫：GitHub `albertkingdom/spring-boot-shopping-website`
- 主分支：`master`
- 本機 `master` 追蹤：`origin/master`
- 建立本文件前的主分支 commit：`b2b67f3 chore: migrate project setup and secure configuration (#2)`
- 工作目錄在建立本文件前為乾淨狀態。
- 本機 `.env` 已保留並由 `.gitignore` 忽略；不得提交或輸出其中內容。
- GitLab CI 設定與 remote 暫時保留，尚未決定移除時間。
- 部署平台與部署流程延後決定。

## Completed

- [x] 將本機有效變更以不含新增密鑰的分支同步到 GitHub。
- [x] 新增 GitHub Actions CI：Java 8、Maven verify、MySQL 8 service 與 Docker image build。
- [x] 新增 Dependabot 的 GitHub Actions 更新設定。
- [x] 將 MySQL 與 Cloudinary 設定改由環境變數注入。
- [x] 新增安全的 `.env.example`、`.dockerignore` 與環境檔忽略規則。
- [x] PR #2 已通過 CI 並以 squash merge 合併。
- [x] 本機 `master` 已同步並改為追蹤 GitHub `origin/master`。
- [x] 已合併的工作分支已從本機與 GitHub 刪除。
- [x] [PR #5](https://github.com/albertkingdom/spring-boot-shopping-website/pull/5)：將 `JwtUtil` 的硬編碼 JWT secret 改由 `JWT_SECRET` 環境變數注入，並在啟動時驗證長度。
- [x] [PR #6](https://github.com/albertkingdom/spring-boot-shopping-website/pull/6)：加入專案交接文件（`PROJECT_STATUS.md`、`docs/jwt-basics.md`）並擴充 `ARCHITECTURE_TODO.md` P2 整合測試待辦。
- [x] [PR #7](https://github.com/albertkingdom/spring-boot-shopping-website/pull/7)：`SecurityConfig` 加入 `/api/user/**` 需要 `ROLE_ADMIN` 的規則，並補上 `UserControllerSecurityTest` 驗證匿名／一般使用者／管理員三種情境。
- [x] [PR #8](https://github.com/albertkingdom/spring-boot-shopping-website/pull/8)：將全部 `System.out.println` 與 `printStackTrace` 改為 SLF4J logger，並刪除未使用的 `CustomAuthenticationFilter`、`LoginInterceptor`、`TestInterceptor`、`WebConfig` 與其他註解掉的舊程式碼。

## Next Actions

請由上而下處理，完成後同步更新核選框與「最後更新」日期。

- [ ] 輪替所有曾提交到 Git 歷史的 MySQL 與 Cloudinary 憑證；只在本機 `.env` 或部署平台 secrets 更新，不得寫入 repository。
- [ ] 審查並處理 [PR #3：setup-java 4.9.1 → 6.0.0](https://github.com/albertkingdom/spring-boot-shopping-website/pull/3)，合併後等待 `master` CI 通過。
- [ ] 在 PR #3 完成後重新確認並處理 [PR #4：checkout 6.1.0 → 7.0.1](https://github.com/albertkingdom/spring-boot-shopping-website/pull/4)，再等待 `master` CI 通過。
- [ ] 重新登入 GitHub CLI：`gh auth login -h github.com`，之後以 `gh auth status` 驗證。
- [ ] 決定是否使用 `git filter-repo` 重寫歷史以清除舊密鑰；執行前必須先確認輪替完成、備份與協作者重新 clone 計畫。
- [ ] GitHub 流程穩定後，決定是否移除 `.gitlab-ci.yml` 與 GitLab remote。
- [ ] 選擇部署方案後，再設計 CD workflow；目前 CI 不負責部署。

## Security Notes

- 移除目前檔案中的明文值，不代表歷史中的憑證已失效。
- 在完成憑證輪替前，安全工作優先於依賴版本更新與部署。
- 不在 issue、PR、commit message、log 或交接文件中貼出任何真實 secret。
- 重寫 Git 歷史會改變 commit SHA 並需要 force push，必須視為獨立且高風險的維護工作。

## Resume Checklist

開始後先執行唯讀檢查：

```bash
git status --short --branch
git log -3 --oneline --decorate
git remote -v
gh auth status
gh pr list --repo albertkingdom/spring-boot-shopping-website
```

如果 GitHub CLI 尚未重新登入，改由已登入的 GitHub 網頁確認 PR 與 Actions 狀態。

建議的新工作階段開場指示：

```text
請先讀 AGENTS.md、PROJECT_STATUS.md、ARCHITECTURE_TODO.md。
先不要修改檔案，檢查 Git 狀態、最近三個 commit 與 GitHub 未關閉的 PR，
摘要目前狀態後，從 PROJECT_STATUS.md 第一個未完成項目繼續。
```

## References

- [PR #2：GitHub CI 與安全設定遷移](https://github.com/albertkingdom/spring-boot-shopping-website/pull/2)
- [Architecture improvement checklist](ARCHITECTURE_TODO.md)
- [Project rules](AGENTS.md)
