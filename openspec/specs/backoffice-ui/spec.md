# Capability: 後台 UI

## Purpose

定義目前已實作的 admin／seller 後台工作空間、導覽、頁面狀態、主題與窄螢幕行為。尚未完成的 UI redesign 不在本 living spec 中宣稱為既有行為，應放在 active change 的 delta spec。

## Requirements

### Requirement: Admin 與 seller workspace 明確標示 scope

系統 SHALL 提供共用的後台導覽，清楚標示目前工作空間與資料範圍。

#### Scenario: Admin enters the platform workspace

- GIVEN 使用者已通過驗證且角色為 `ROLE_ADMIN`
- WHEN 使用者開啟 `/admin`
- THEN UI 顯示平台管理工作台，以及全站商品、全站訂單與商家權限入口，並標示正在查看全站資料

#### Scenario: Seller enters the seller workspace

- GIVEN 使用者已通過驗證且角色為 `ROLE_SELLER`
- WHEN 使用者開啟 `/seller`
- THEN UI 顯示商家中心、我的商品與我的訂單入口，並說明只顯示本店資料

### Requirement: Navigation 由共用 module metadata 驅動

系統 SHALL 使用共用的後台 shell 與 module registry 產生桌面／窄螢幕導覽、active state 與 breadcrumb。Route table 仍須在 `frontend/src/App.js` 明確註冊；module registry 不負責自動產生 route fallback。

#### Scenario: A new module uses the existing shell

- GIVEN 未來 module 宣告 stable key、route、workspace、required role、group 與 sort order
- WHEN 工程師將該 module 加入 registry 並在 App route table 註冊 route
- THEN 桌面與窄螢幕導覽可以共用既有 shell、workspace context、active state 與 breadcrumb，而不需複製 shell 或 workspace switcher

### Requirement: 已實作的後台流程提供支援的狀態回饋

已實作的後台流程 SHALL 對各自支援的操作提供可理解的狀態回饋；本規格不宣稱所有資源頁面已完成一致的 loading、empty、error、`401`、`403` 與 `409` 狀態矩陣。

#### Scenario: Seller revocation conflict is shown accurately

- GIVEN API 因 seller 仍擁有商品而回傳 `409`
- WHEN admin 確認撤銷 seller role
- THEN UI 保留 seller role 的原本狀態，說明衝突原因，不顯示錯誤的成功訊息，並提供前往全站商品的下一步入口

#### Scenario: Seller product list has an actionable empty state

- GIVEN seller 商品 API 成功回傳空清單
- WHEN seller 開啟我的商品頁
- THEN UI 顯示目前沒有商品的狀態，並提供新增第一項商品的 CTA

#### Scenario: Seller deletion requires confirmation

- GIVEN seller 在商品列表選擇刪除商品
- WHEN 確認對話框出現且使用者取消
- THEN UI 不送出 DELETE request，原商品列表維持不變

### Requirement: Theme 與 responsive behavior 不改變 authorization

系統 SHALL 支援 system、light 與 dark presentation mode，以及窄螢幕後台導覽；這些 presentation 行為不得改變 route、role、API request 或資料範圍。

#### Scenario: Theme preference persists locally

- GIVEN 使用者選擇 light 或 dark mode
- WHEN browser reload 後重新開啟後台
- THEN UI 從 local preference 還原選定的 presentation mode，workspace 與 authorization 不變

#### Scenario: Narrow-screen navigation remains operable

- GIVEN viewport 約為 375px 寬
- WHEN 使用者開啟後台導覽與 seller 資源列表
- THEN 導覽、控制項、文字、focus state 與 table actions 仍可操作，不需要 desktop-only layout，且不產生不必要的水平捲動

## 目前限制

- Admin 商品／訂單資源頁目前只有基本載入與錯誤處理，尚未全面提供與 seller 頁面一致的 empty、confirmation、401／403／409 與 retry 狀態。
- `system` mode 目前透過 `color-scheme` 跟隨瀏覽器偏好；system preference 動態變更與完整 contrast／keyboard smoke test 尚待後續 UI change 補齊。
