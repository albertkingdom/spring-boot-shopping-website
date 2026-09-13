# Capability: 存取控制

## Purpose

定義購物網站目前提供的 authentication、role、seller ownership 與 server-side authorization 行為。

## Requirements

### Requirement: 註冊會建立一般使用者

系統 SHALL 以 `ROLE_USER` 建立新註冊帳號，且不得接受 registration request 指定的角色。

#### Scenario: 新帳號取得預設角色

- GIVEN public registration request 包含 email、name 與 password
- WHEN `POST /api/register` 成功
- THEN 帳號以 `ROLE_USER` 建立，response 不暴露 password 或 password hash

#### Scenario: 註冊不能自行取得 seller 權限

- GIVEN registration request 包含 roles 欄位或 seller 相關欄位
- WHEN server 處理 request
- THEN server 忽略或拒絕 client-supplied role elevation，且不授予 `ROLE_SELLER`

### Requirement: 角色控制受保護的功能

系統 SHALL 在每個 protected endpoint 由 server 強制執行 authentication 與 role checks；frontend route visibility 不是 security boundary。

#### Scenario: 未驗證的存取會被拒絕

- GIVEN request 沒有有效 access token
- WHEN request 呼叫 protected endpoint
- THEN server 回傳 `401`

#### Scenario: 權限不足會被拒絕

- GIVEN 已驗證的 `ROLE_USER` 或其他權限不足的帳號
- WHEN request 呼叫 admin-only 或 seller-only endpoint
- THEN server 回傳 `403`

### Requirement: Admin 控制 seller role membership

系統 SHALL 只允許 `ROLE_ADMIN` 對既有 user grant 或 revoke `ROLE_SELLER`。

#### Scenario: Admin 授予 seller 權限

- GIVEN admin 與既有 target user
- WHEN admin 呼叫 `POST /api/admin/users/{userId}/roles/seller`
- THEN target 取得 `ROLE_SELLER`，endpoint 回傳 `204`

#### Scenario: 使用者仍擁有商品時不得撤銷角色

- GIVEN seller 仍擁有一項以上商品
- WHEN admin 呼叫 `DELETE /api/admin/users/{userId}/roles/seller`
- THEN endpoint 回傳 `409`，role 與商品 ownership 維持不變，UI 說明衝突原因

### Requirement: Seller access 受 ownership scope 限制

系統 SHALL 從 authenticated principal 推導 seller ownership，並在 service layer 強制 owner scope。

#### Scenario: Seller 不能操作其他 seller 的商品

- GIVEN seller A 擁有商品，且 seller B 已通過驗證
- WHEN seller B 嘗試更新或刪除 seller A 的商品
- THEN server 回傳 `403`，商品不被修改

#### Scenario: Multi-role account 切換 workspace 不改變 authorization

- GIVEN account 同時具有 `ROLE_ADMIN` 與 `ROLE_SELLER`
- WHEN user 在 admin 與 seller workspace 間切換
- THEN UI 只改變 navigation 與 data scope，所有 API request 仍受 server-side role 與 ownership checks 保護
