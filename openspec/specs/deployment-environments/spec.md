# Capability: 部署環境

## Purpose

定義 dev、staging 與 production 目前的 isolation、image promotion、migration、smoke-test 與 deployment-target 邊界。

## Requirements

### Requirement: 環境隔離資料與 credentials

系統 SHALL 分開 dev、staging、production 的 databases、application credentials、JWT issuer/audience、Compose project names 與 persistent volumes。

#### Scenario: Staging 啟動時不使用 production data

- GIVEN staging deployment 使用自己的 environment file 啟動
- WHEN Spring 與 MySQL ready
- THEN application 使用 staging database 與 credentials，不載入 dev-only demo data 或 production records

#### Scenario: Production 不公開 internal ports

- GIVEN production Compose stack 正在執行
- WHEN external client 連線至 host
- THEN 只有 HTTPS reverse-proxy 或 tunnel entry point 對外公開；MySQL、Spring 與 phpMyAdmin 不公開

### Requirement: Deployment 使用已驗證的 immutable images

系統 SHALL 在 CI build 與 verify backend/frontend images，以 digest 發布，並在 target host 使用相同 digest pair 部署而不重新 build。

#### Scenario: Master promotion 已驗證的 staging release

- GIVEN push 到 `master` 通過 verification 與 image publication
- WHEN staging deploy job 在 target self-hosted runner 執行
- THEN job 下載 release manifest、pull 兩個 digest-pinned images、啟動 staging stack，並執行 loopback smoke tests

#### Scenario: Production tag promotion 使用相同 release

- GIVEN `master` commit 已通過 staging acceptance
- WHEN 該 commit 的 annotated `v*` tag 通過 production environment approval
- THEN production 部署相同的 backend/frontend image digests，不重新 build

### Requirement: Database migrations 在 traffic acceptance 前執行

系統 SHALL 在 application startup 執行 versioned Flyway migrations，且 deployment smoke tests 通過後 release 才可視為 accepted。

#### Scenario: Migration 或 smoke test 失敗

- GIVEN Flyway migration 或 frontend/API loopback smoke test 失敗
- WHEN deployment job 評估 release
- THEN job 失敗，release 不 promotion 至下一個 environment

### Requirement: Deployment target 必須明確

系統 SHALL 透過 environment-specific runner labels 或等效 target binding，將 staging 與 production deployment jobs 路由至指定的 self-hosted machines。

#### Scenario: Staging 與 production 使用不同 machines

- GIVEN staging 與 production hosted on different machines
- WHEN 對應 deployment job 排入佇列
- THEN 每個 job 只在 designated machine 執行，不可指派至另一 environment 的 runner
