# 多商家商品與訂單權限

> Legacy feature spec：目前系統契約已整理至 [`openspec/specs/`](../../openspec/specs/)，本檔案保留作歷史參考，不再作為新變更的 source of truth。

## 背景與目標

目前系統只有 `ROLE_USER` 與 `ROLE_ADMIN`。`ROLE_ADMIN` 可管理全站商品、訂單與使用者，適合單一商店的後台，但不適合多商家平台：若直接把商家設為 admin，他們能看見其他商家的訂單與商品。

本功能新增受平台管理員審核的 `ROLE_SELLER`。商家只能管理自己上架的商品，並只能檢視含有自己商品的訂單資料；`ROLE_ADMIN` 保留為平台的全站管理權限。

## 範圍

包含：

- 新增 `ROLE_SELLER`，只由 `ROLE_ADMIN` 授予或撤銷。
- 為商品建立賣家歸屬；新商品的賣家由登入者決定，不能由 request 指定。
- 為訂單項目保存賣家快照，讓歷史訂單不會因商品日後轉移或刪除失去歸屬。
- 提供商家專用的商品與訂單查詢／管理 API，並確保所有權檢查在 Service 層執行。
- 保留平台管理員的全站管理能力。
- 在前端提供平台管理員的商家審核入口，以及商家的商品與訂單後台；公開商店維持原有購物流程。
- 重新設計與多商家權限直接相關的後台工作空間、導覽與操作流程，讓平台管理員與商家能清楚辨識目前的資料視角。

不包含：

- 商家自行註冊、申請或自行升級為 `ROLE_SELLER`。
- 商家公開頁面、抽成、分帳、物流、款項撥付或多店結帳拆單。
- 將既有未歸屬商品自動指派給任一商家。

## 實作理由與指引

角色檢查只能回答「此人是不是商家」，不能回答「這筆商品是不是他的」。因此商品需要 `seller_id`，每次新增、修改、刪除都要在 Service 層以目前登入者比對 owner；Controller 只取得 `Principal` 並呼叫 Service。

訂單會保存商品名稱與單價的快照，同一原則也適用賣家：在建立 `OrderItem` 時保存 `seller_id`。不能在日後靠 `product_id` 回查 owner，因為商品可能已被刪除或轉讓。商家讀訂單時只能取得自己名下的 item，不能讀取其他商家的 item 或整張訂單總金額。

初期以既有 `User` 作為商家主體，不另建 `Seller` entity。這可減少 migration 與帳號流程複雜度；若未來需要商店名稱、統編、結算帳戶或多位店員，再獨立建立 merchant domain model。

前端只負責依登入者角色顯示正確入口與操作流程；它不是安全邊界。即使使用者手動呼叫 API，後端仍必須在 Service 層驗證角色與商品／訂單的 owner。

## 情境與驗收條件

1. 平台管理員可將已註冊的使用者授予 `ROLE_SELLER`；一般使用者無法呼叫該操作或在註冊 payload 指定角色。
2. 商家 A 新增商品後，商品 owner 為商家 A；request 不得覆寫 owner。
3. 商家 A 可以修改或刪除自己的商品；商家 B 對同一商品收到 `403 Forbidden`；平台管理員可以管理任何商品。
4. 公開商品瀏覽與一般使用者下單流程維持可用。
5. 訂單建立時，每個 item 都保存該商品當時的 `seller_id`。
6. 商家 A 的訂單列表與詳情只包含含有商家 A 商品的訂單項目，且不含其他商家的 item、整張訂單總金額或全站訂單列表。
7. 平台管理員仍可取得完整訂單詳情與全站訂單列表。
8. 賣家角色被撤銷前，若仍有商品歸屬於該使用者，系統拒絕撤銷並要求先轉移或下架商品。

## 前端畫面與操作設計

前端位於本 repository 的 `frontend/`；本規格同時定義前後端的畫面行為與 API contract，實作須在同一個 feature branch／PR 一起驗證。

### 設計目標與邊界

- 將「公開商店」與「後台工作空間」分成清楚的產品區域；後台頁面固定顯示目前視角，例如「平台管理」或「商家中心」。
- 平台管理員看到跨商家的全站資料；商家只看到自己的商品與訂單。UI 只協助降低誤操作，後端授權仍是唯一安全邊界。
- 這次只改善多商家功能相關的資訊架構、導覽、表單、操作確認與回饋狀態；不加入商店頁、品牌樣式、賣家自訂外觀、抽成或分帳。
- 不為了製作 dashboard 指標而新增統計 API；首頁使用工作入口、權限說明與待處理操作導引，實際資料仍來自既有 scoped API。
- 視覺語氣採實用型電商／B2B 後台：中性底色、緊湊表格、少量圓角與陰影；不使用漸層、裝飾性大卡片、過多 badge 或 AI 助理式文案。

### 業界參考與採用原則

- **Shopify Admin** 將訂單、商品、客戶等核心資源放在持續可見的側欄，並在 top bar 提供搜尋與 store switcher；本專案採用相同的「固定後台 shell + 明確工作空間切換」概念，但將 store switcher 簡化為平台管理／商家中心切換。[Shopify Admin 導覽](https://help.shopify.com/en/manual/shopify-admin/shopify-admin-overview)
- **Shopify order detail** 讓訂單詳情集中呈現商品、subtotal 與歷史資訊；本專案採用「列表 → 詳情」的工作流程，但 seller 詳情只呈現自己的 item 與本店小計，不能照搬全站資料視角。[Shopify 訂單詳情](https://help.shopify.com/en/manual/fulfillment/managing-orders/managing-order-details)
- **Stripe Dashboard** 把資源導覽、搜尋、團隊／帳號操作與可重複的 dashboard shell 組合在一起；本專案先採用清楚的資源導覽與頁面上下文，global search 與 keyboard shortcut 暫列為後續能力，避免在沒有 API contract 時虛構搜尋功能。[Stripe Dashboard](https://docs.stripe.com/dashboard/basics)
- **Amazon Seller Central** 的 Manage Orders 以訂單狀態、訂單編號、買家與商品作為日常處理入口；本專案先採用清楚的訂單列表、scope 提示與詳情入口，狀態分頁與進階篩選要等後端提供對應欄位與查詢 contract 後再加入。[Amazon Manage Orders](https://sell.amazon.com/blog/amazon-order-management)
- 參考的是資訊架構與操作模式，不複製任何品牌色、文案、商業流程或受限制的畫面資產；所有 UI 仍以本專案既有 Bootstrap／React 組件與多商家資料隔離規則為準。

### 後台工作空間與導覽

- **公開商店**：既有商品瀏覽、註冊、登入、購物車與下單流程維持不變，不顯示全站商家管理入口。
- **平台管理工作空間**：`ROLE_ADMIN` 使用 `/admin`，共用一個後台 shell，導覽為「概覽」、「商家權限」、「全站商品」、「全站訂單」。
- **商家工作空間**：`ROLE_SELLER` 使用 `/seller`，共用同樣的後台 shell，導覽為「概覽」、「我的商品」、「我的訂單」。畫面固定顯示「只顯示本店資料」的 scope 提示。
- **多角色帳號**：同時具有 `ROLE_ADMIN` 與 `ROLE_SELLER` 的使用者，在 header 顯示明確的工作空間切換入口；切換到平台管理或商家中心後，導覽、標題與資料視角同步切換，不自動把 admin 的全站資料混入 seller 畫面。
- **未授權狀態**：一般 user、純 admin 或純 seller 直接輸入不適用 URL 時，顯示一致的權限說明或導向登入；不能只因前端隱藏連結就視為授權完成。

### 可擴充的後台架構

- 後台採用一個共用的 `BackofficeShell` 概念，統一承擔 header、工作空間切換、breadcrumb、scope 提示、側欄、頁面標題、載入／空白／錯誤狀態與 responsive layout；admin 與 seller 不各自複製一套 shell。
- 導覽以單一 module registry／設定描述驅動，而不是把 admin 與 seller 的側欄 JSX 分散在各頁面。每個模組至少宣告穩定的 `key`、顯示名稱、route、適用 workspace、required role／capability、導覽群組與排序；同一份 metadata 同時驅動桌面側欄、窄螢幕導覽、active state、breadcrumb 與 route fallback。
- 導覽群組先分為「工作台」、「營運」、「管理」；目前模組依角色放入對應群組。未來新增報表、促銷、履約或設定等功能時，只需新增符合 registry contract 的 module metadata 與頁面，不能為每個新功能重寫 shell、workspace switcher 或 mobile nav。
- module registry 只負責入口顯示與前端 route guard；後端 endpoint 的 authentication、authorization、資料 scope 與 `403`／`404` 仍是最終安全邊界。即使使用者手動輸入 route 或前端設定錯誤，也不得因此取得未授權資料。
- 頁面實作優先重用共用的 `PageHeader`、`ResourceTable`、`EmptyState`、`ErrorState`、`ConfirmDialog` 等 UI primitives，讓未來模組遵循相同的資料列表、確認操作、錯誤回饋與可及性模式。
- 新增任何後台模組前，必須先在同一份 feature spec 定義其 workspace／role／scope、route 與 breadcrumb、API／資料影響、loading／empty／error／401／403 行為、窄螢幕操作，以及對應的 acceptance criteria 與 scenarios；未定義的功能不得直接塞入既有側欄。

### 深色／淺色主題

- 共用後台 shell 支援「跟隨系統」、「淺色」、「深色」三種模式；預設跟隨使用者的作業系統偏好，使用者手動選擇後以 browser-local preference 保存，不增加後端帳號設定或 API。
- 主題切換套用到 header、側欄、頁面背景、列表／表單、對話框、toast、狀態 badge、輸入控制項與 focus indicator；不得只替換背景色而讓文字、邊框或錯誤／成功狀態失去可讀性。
- 深色與淺色只改變 presentation，不改變 workspace、role、資料 scope、route、功能可見性或 API request；同一份 module registry 與驗收流程在兩種主題都必須成立。
- 顏色不是唯一的狀態訊號；成功、警告、錯誤、目前選取與 disabled 狀態仍需搭配文字、位置、icon 或 native control state 表達。主題 token 應集中管理，未來模組不得自行散落硬編碼色碼。
- 若 browser 不允許讀寫 local preference，介面仍須能切換主題，但可退回只在目前頁面生效並以 system preference 作為初始值；不得阻塞登入或後台主要操作。

### 平台管理員流程

- **概覽**：提供「管理商家權限」、「管理全站商品」、「查看全站訂單」三個主要入口與目前平台管理視角說明，不新增統計 API。
- **商家權限**：使用搜尋欄依 email／名稱篩選，列表顯示帳號、目前角色與商家狀態；角色以易讀的 badge 呈現，不直接把 `ROLE_*` 當作主要文案。
- **授予商家**：點擊「授予商家」後先開啟確認對話框，說明此操作會影響該帳號下一次登入後可見的功能；成功後更新該列並顯示結果。
- **撤銷商家**：點擊「撤銷商家」後先確認；若後端回傳 `409`，保留列表狀態並以可理解的警告說明仍有商品歸屬，提供回到「全站商品」處理的入口，不假裝撤銷成功。
- **全站商品／訂單**：維持平台 admin 的完整視角；商品列表可辨識平台自營或 seller 歸屬，訂單列表與詳情保留整單資訊。刪除或高風險操作要有確認與失敗回饋。

### 商家流程

- **概覽**：顯示商家 scope 說明與「新增商品」、「查看我的商品」、「查看我的訂單」主要操作；沒有跨商家統計或平台管理入口。
- **我的商品**：列表顯示圖片、商品名稱、價格與操作；提供清楚的新增、編輯、刪除操作。新增／編輯表單不顯示、不接受或提交 `sellerId`，並在送出期間鎖定按鈕、顯示驗證與上傳錯誤。
- **刪除商品**：先確認再送出；成功後依最新分頁結果留在有效頁面，失敗時保留原列表並顯示可重試訊息。
- **我的訂單**：列表顯示訂單編號、買家 email、本店小計、成立時間與詳情入口；不顯示全單總額。詳情只顯示該商家的商品項目、數量、單價與本店小計。
- **空白／載入／錯誤**：沒有商品或訂單時提供下一步 CTA；載入中不可重複送出操作；`401` refresh 失敗導向登入，`403` 顯示權限說明，其他錯誤提供保留資料與重試選項。

## 主要操作流程

以下流程是驗收時的實際操作腳本；每個流程都定義前置條件、使用者操作、預期 API／授權行為、成功結果與例外結果。實作測試與人工驗收應以流程中的 `AC-*`／`SCN-*` 對照，不只確認畫面上有按鈕。

### FLOW-001：登入與後台工作空間進入

**前置條件**

- 測試帳號分別只有 `ROLE_ADMIN`、只有 `ROLE_SELLER`，以及同時具有兩種角色。
- 一般 `ROLE_USER` 帳號也可用來驗證未授權狀態。

**操作與預期結果**

1. 使用者登入，前端取得登入 response／JWT 中的 role。
2. `ROLE_ADMIN` 導向 `/admin`，顯示「平台管理」、全站商品與全站訂單入口。
3. `ROLE_SELLER` 導向 `/seller`，顯示「商家中心」、我的商品與我的訂單，並顯示本店 scope 提示。
4. 多角色帳號從 header 切換工作空間；側欄、breadcrumb、標題與 API scope 一起切換。
5. `ROLE_USER` 或角色不足者直接輸入不適用 route；前端顯示登入／權限說明，後端仍回傳 `401` 或 `403`，不可取得資料。

**驗收對應**：`AC-UI-001`、`AC-UI-005`；`SCN-UI-001`～`SCN-UI-003`、`SCN-UI-010`。

### FLOW-002：平台管理員授予／撤銷商家角色

**前置條件**

- 操作者已登入且具有 `ROLE_ADMIN`。
- 授予測試目標只有 `ROLE_USER`；撤銷測試目標具有 `ROLE_SELLER` 且仍擁有商品。

**操作與預期結果**

1. 進入「平台管理 → 商家權限」，依 email／名稱搜尋目標帳號。
2. 點擊「授予商家」或「撤銷商家」；畫面先顯示確認對話框，不直接送出。
3. 確認授予時呼叫 `POST /api/admin/users/{userId}/roles/seller`；成功 `204` 後更新列表並提示目標帳號下次登入可使用商家中心。
4. 確認撤銷時呼叫 `DELETE /api/admin/users/{userId}/roles/seller`；若目標仍有商品，收到 `409` 後保留原 seller 狀態，顯示原因並提供前往全站商品的入口。
5. 非 admin 直接呼叫 endpoint 時回傳 `403`；目標不存在回傳 `404`；重複授予或不適用撤銷回傳 `409`，畫面不得宣稱成功。

**驗收對應**：核心驗收條件 1、8；`AC-UI-002`；`SCN-UI-004`、`SCN-UI-005`。

### FLOW-003：商品列表與商品 owner 控制

**前置條件**

- 準備 seller A、seller B、admin，以及 seller A 建立的商品。
- 商品表單不包含 `sellerId`、owner ID 或可任意指定歸屬的欄位。

**操作與預期結果**

1. seller A 進入「商家中心 → 我的商品」，列表只呼叫 seller-scoped API，點擊「新增商品」並送出表單。
2. seller 新增使用 `POST /api/products`；Service 從已驗證身份設定 owner，成功回傳 `201`，列表顯示新商品。
3. admin 進入「平台管理 → 全站商品」並新增平台商品；使用 `/api/admin/products`，伺服器固定保存 `seller_id = NULL`，成功回傳 `201`。
4. seller A 編輯／刪除自己的商品；送出期間鎖定控制項，成功後以最新列表與有效頁碼更新畫面，刪除失敗則保留原列表並提供重試。
5. seller B 嘗試透過畫面或直接 request 編輯／刪除 seller A 商品，Service 回傳 `403`；不能靠前端隱藏按鈕作為唯一防護。
6. 輸入驗證錯誤回傳 `400`，找不到商品回傳 `404`；上傳成功但 DB 寫入失敗時執行 Cloudinary 補償刪除並記錄不含敏感資料的 log。

**驗收對應**：核心驗收條件 2、3；`AC-UI-003`、`AC-UI-005`；`SCN-UI-006`、`SCN-UI-007`。

### FLOW-004：公開瀏覽、下單與 seller snapshot

**前置條件**

- 公開商品中同時存在 seller A 商品與平台自營／歷史商品。
- 一般使用者未登入或已登入皆可走既有公開購物流程。

**操作與預期結果**

1. 使用者從公開商店瀏覽商品、加入購物車並送出訂單；公開流程不顯示後台角色或 seller 管理入口。
2. 下單 Service 依每個商品當下的 owner 寫入 `order_item.seller_id`，並保存商品名稱與單價快照。
3. seller 商品的 item 保存對應 seller ID；平台自營／歷史商品可保存 `NULL` seller snapshot，整筆訂單仍成功建立。
4. 商品後續修改或刪除後，既有訂單的名稱、單價與 seller snapshot 不被改寫。
5. 下單輸入錯誤回傳 `400`；商品不存在回傳 `404`；公開購物流程不可因 seller snapshot 為 `NULL` 而拋出未處理例外。

**驗收對應**：核心驗收條件 4、5；Review remediation 驗收條件 3；公開 checkout 與 order service integration test。

### FLOW-005：Seller／Admin 訂單列表與詳情

**前置條件**

- 建立一筆同時包含 seller A、seller B 與平台自營 item 的跨商家訂單。
- 準備 seller A 與 `ROLE_ADMIN` 登入 session。

**操作與預期結果**

1. seller A 進入「商家中心 → 我的訂單」，呼叫 `GET /api/seller/orders`，列表只顯示含有 seller A item 的訂單、本店小計、買家 email、時間與詳情入口。
2. seller A 開啟訂單詳情，呼叫 `GET /api/seller/orders/{id}`；只顯示 seller A 的 item、數量、單價與本店小計，不顯示 seller B item、平台 item、整單總額或全站訂單列表。
3. admin 進入「平台管理 → 全站訂單」，查看同一筆訂單的列表與詳情；可看到所有 item、完整訂單與整單總額。
4. seller 直接輸入不屬於自己的 order route 或 request 時，回傳 `403`／適當的 scoped 結果，不洩漏其他 seller 資料。
5. seller 訂單查詢不可對每張訂單額外查詢買家；access token 過期時先依既有 refresh 流程處理，refresh 失敗則導向登入並保留可理解的錯誤。

**驗收對應**：核心驗收條件 6、7；Review remediation 驗收條件 5；`AC-UI-004`；`SCN-UI-008`、`SCN-UI-009`。

### FLOW-006：共用狀態、RWD 與鍵盤操作

**前置條件**

- 以 admin 與 seller 各操作一次主要列表流程。
- 分別準備 loading、empty、`400`、`401`、`403`、`409`、外部服務失敗與最後一頁刪除情境。

**操作與預期結果**

1. 載入資料時顯示 loading，避免重複送出；無資料時保留 shell／導覽並提供下一步 CTA。
2. 取得 `401` 時依 refresh 結果重新請求或導向登入；`403` 顯示權限說明；`409` 顯示後端衝突原因；其他錯誤保留可用資料並提供重試。
3. 將 viewport 縮小至窄螢幕，側欄依同一份 module registry 轉為可操作的水平／收合導覽，列表與操作不被裁切。
4. 只使用鍵盤完成 workspace 切換、導覽、搜尋、表單送出、確認對話框與返回列表；所有控制項有可辨識名稱與 focus indicator。

**驗收對應**：`AC-UI-005`；`SCN-UI-010`、`SCN-UI-011`。

### FLOW-007：深色／淺色主題切換

**前置條件**

- 使用者已登入任一後台 workspace，browser 可能允許或拒絕 local preference 讀寫。

**操作與預期結果**

1. 從 header 的顏色模式選單選擇「跟隨系統」、「淺色」或「深色」。
2. 切換後 header、側欄、頁面背景、列表、輸入框、按鈕、對話框、toast、狀態與 focus indicator 同步更新，文字與狀態仍可辨識。
3. 手動選擇時寫入 browser-local preference；重新載入後仍使用該選擇。選擇跟隨系統時，系統明暗偏好變更後更新主題。
4. 確認主題切換前後 workspace、route、module registry、API request、角色權限與資料 scope 完全不變。
5. local preference 讀寫失敗時，主題仍可在目前頁面切換，並退回 system preference；不得阻塞登入、導覽或主要操作。

**驗收對應**：`AC-UI-008`；`SCN-UI-015`、`SCN-UI-016`。

### FLOW-008：新增後台模組

**前置條件**

- 假設未來註冊只允許 `ROLE_ADMIN` 的 `reports` module。
- module metadata 已包含 `key`、label、route、workspace、required role／capability、導覽群組、排序與 breadcrumb。

**操作與預期結果**

1. 將 module metadata 加入 registry，頁面使用既有 `BackofficeShell` 與共用狀態元件，不新增另一套 admin／seller shell。
2. admin 登入後在指定導覽群組看到 `reports`，點擊後 active state、breadcrumb、頁面標題與 route 一致；窄螢幕仍由同一份 registry 產生可操作導覽。
3. seller 登入後看不到 `reports` 入口；seller 直接輸入 route 時前端 route guard 顯示一致的權限結果，後端 API 仍回傳 `403` 且不提供資料。
4. 新模組若需要 API、資料模型、狀態或新的商業規則，先更新同一份 feature spec 與驗收對照，再開始實作，不以新增側欄項目取代需求定義。

**驗收對應**：`AC-UI-007`；`SCN-UI-013`、`SCN-UI-014`。

### UI/UX 驗收條件

#### AC-UI-001：角色工作空間與導覽一致

- Requirement: 多商家角色的後台入口與資料視角必須清楚分離。
- Must be true: admin、seller、同時具有兩種角色的帳號，各自只能看到適用的工作空間導覽；多角色帳號可以明確切換但不會混用 scope。
- Validation scenarios: `SCN-UI-001`、`SCN-UI-002`、`SCN-UI-003`

#### AC-UI-002：平台管理員可安全處理商家權限

- Requirement: 商家角色授予／撤銷是可確認、可回饋且不誤報成功的操作。
- Must be true: 成功操作更新列表；`409` 撤銷衝突保留原狀態、顯示後端原因並引導處理；一般 user 無法看到或執行該功能。
- Validation scenarios: `SCN-UI-004`、`SCN-UI-005`

#### AC-UI-003：商家商品流程支援日常操作

- Requirement: seller 能在自己的工作空間完成商品查詢、新增、編輯與刪除。
- Must be true: 列表與表單不暴露 owner 欄位；刪除與上傳失敗不會清空或誤更新既有列表；分頁會停留在有效頁碼。
- Validation scenarios: `SCN-UI-006`、`SCN-UI-007`

#### AC-UI-004：商家訂單流程維持資料隔離

- Requirement: seller 能查看自己的訂單，但不能從畫面取得其他 seller item 或整單金額。
- Must be true: 列表與詳情只顯示 seller-scoped response 的欄位；admin 仍保留完整訂單視角。
- Validation scenarios: `SCN-UI-008`、`SCN-UI-009`

#### AC-UI-005：各狀態與窄螢幕操作可理解

- Requirement: 後台在主要非正常狀態仍能讓使用者理解下一步。
- Must be true: loading、empty、error、401、403、409 都有明確文案與適當操作；窄螢幕不需要水平捲動才能完成主要查詢與操作，互動控制項有可辨識的名稱。
- Validation scenarios: `SCN-UI-010`、`SCN-UI-011`

#### AC-UI-006：視覺呈現符合日常營運後台

- Requirement: 後台需要讓使用者快速掃描資料與執行操作，而不是呈現行銷型 dashboard。
- Must be true: 主要畫面以資源列表、欄位、狀態與明確操作為主；色彩、圓角、陰影與摘要區塊保持克制，不出現與功能無關的裝飾性元件。
- Validation scenarios: `SCN-UI-012`

#### AC-UI-007：後台模組可擴充

- Requirement: 未來增加後台功能時，能沿用既有 shell、導覽與狀態元件，不因 admin／seller 或桌面／窄螢幕而複製多套流程。
- Must be true: 新模組只要提供 registry metadata 與頁面即可出現在正確 workspace／導覽群組；同一份 metadata 驅動 desktop nav、mobile nav、active state、breadcrumb 與 route guard；不符合角色的使用者看不到入口，直接輸入 URL 也不會取得資料。
- Validation scenarios: `SCN-UI-013`、`SCN-UI-014`

#### AC-UI-008：深色／淺色主題一致且可讀

- Requirement: 後台可依系統偏好或使用者選擇使用深色／淺色主題，且不影響功能與資料隔離。
- Must be true: header、側欄、列表、表單、對話框、toast、狀態與 focus indicator 在兩種主題都有足夠對比與可辨識狀態；重新載入後保留手動選擇，選擇跟隨系統時會回應系統偏好變更；主題切換不改變 role、workspace、route 或 API scope。
- Validation scenarios: `SCN-UI-015`、`SCN-UI-016`

#### SCN-UI-001：純平台管理員登入

- Given 帳號只有 `ROLE_ADMIN`
- When 登入並進入後台
- Then 看到平台管理工作空間與全站商品／訂單入口，不看到商家中心入口

#### SCN-UI-002：純商家登入

- Given 帳號只有 `ROLE_SELLER`
- When 登入並進入後台
- Then 看到商家中心、我的商品與我的訂單，且頁面顯示只讀取本店資料的提示

#### SCN-UI-003：多角色工作空間切換

- Given 帳號同時具有 `ROLE_ADMIN` 與 `ROLE_SELLER`
- When 從 header 切換工作空間
- Then 導覽、標題與資料 API scope 一起切換，seller 畫面不顯示 admin 全站資料

#### SCN-UI-004：授予商家角色

- Given admin 在商家權限列表找到一般使用者
- When 確認「授予商家」
- Then 呼叫既有 admin endpoint，成功後該列顯示 seller 狀態與重新登入提示

#### SCN-UI-005：撤銷商家被商品阻擋

- Given seller 仍擁有商品
- When admin 確認「撤銷商家」
- Then 後端回傳 `409` 時保留 seller 狀態，顯示阻擋原因與前往全站商品的處理入口

#### SCN-UI-006：商家商品日常操作

- Given seller 進入我的商品
- When 查詢、新增、編輯或刪除商品
- Then 只使用 seller-scoped／owner-checked API，表單不含 `sellerId`，成功或失敗都有對應結果

#### SCN-UI-007：商家商品列表邊界狀態

- Given seller 商品為空、刪除最後一筆或 access token 過期
- When 載入或操作列表
- Then 顯示 empty／重新登入／錯誤狀態，分頁保持有效且不清除既有資料

#### SCN-UI-008：商家查看訂單

- Given 一筆訂單含有多家 seller 的商品
- When seller A 查看訂單列表與詳情
- Then 只顯示 seller A 的 item、買家 email、本店小計與必要欄位，不顯示其他 seller item 或整單總額

#### SCN-UI-009：平台管理員查看完整訂單

- Given admin 進入全站訂單
- When 查看列表與詳情
- Then 可看到完整訂單、整單總額與所有 item，且不被 seller UI 的 scope 限制

#### SCN-UI-010：權限與錯誤回饋

- Given 使用者未登入、角色不足、操作衝突或外部請求失敗
- When 後台請求失敗
- Then 分別顯示登入、權限、409 衝突或可重試的錯誤，不宣稱操作成功

#### SCN-UI-011：窄螢幕與鍵盤操作

- Given 瀏覽器寬度縮小或使用鍵盤操作
- When 使用主要後台流程
- Then 導覽可收合、表格／卡片仍可閱讀，按鈕與表單欄位有可辨識名稱，主要操作不被裁切

#### SCN-UI-012：日常營運視覺

- Given 使用者每天需要處理商家、商品或訂單
- When 開啟任一後台主要列表
- Then 可以直接辨識頁面、資料欄位、目前 scope 與下一步操作，不被大型摘要卡、裝飾性動畫或過度品牌化視覺干擾

#### SCN-UI-013：新增模組不重寫後台 shell

- Given 後續功能註冊一個只允許 `ROLE_ADMIN` 的 `reports` module，並提供 route、導覽群組與頁面標題 metadata
- When module 被啟用並從平台管理工作空間進入
- Then 它出現在正確的導覽群組，桌面／窄螢幕導覽、active state 與 breadcrumb 保持一致，且不需要新增另一套 shell 或複製 seller／admin 側欄；seller 不會看到該入口

#### SCN-UI-014：新模組的 route 與 responsive fallback

- Given 使用者直接輸入未授權的新模組 route，或在窄螢幕開啟該模組
- When 前端 route guard 與後端 API 授權執行
- Then 未授權使用者看到一致的 `403`／導向結果且不取得資料；已授權使用者仍使用同一份 registry 產生可操作的窄螢幕導覽，不需要水平捲動才能完成主要操作

#### SCN-UI-015：切換深色／淺色主題

- Given 使用者已登入後台，且目前位於任一 admin／seller 資源列表
- When 從 header 選擇淺色或深色主題
- Then header、側欄、列表、按鈕、輸入框、狀態與錯誤／成功回饋同步換色，文字與控制項仍可辨識；目前 workspace、頁面、資料內容與 API scope 不變

#### SCN-UI-016：主題偏好與瀏覽器限制

- Given 使用者選擇跟隨系統或手動選擇主題，且 browser local preference 可能可用或不可用
- When 重新載入頁面、切換系統明暗偏好，或遇到 local preference 讀寫失敗
- Then 跟隨系統模式會反映目前系統偏好，手動選擇會在可用時保留；讀寫失敗時仍能使用目前頁面的主題，不阻塞登入、導覽與主要操作

第一版仍以既有設計系統與桌面後台流程為基礎，這次 redesign 不引入新的視覺品牌或賣家自訂外觀。

## API 影響

既有公開 `GET /api/products/**` 與一般使用者 `POST /api/order` 維持相容。

本次 UI/UX redesign 不新增或修改後端 HTTP endpoint、request、response、status code 或授權規則；只重新編排既有路由與既有 API 的前端使用流程。深色／淺色主題與 browser-local preference 完全由前端處理，不進入 API contract。若實作需要新增 dashboard 統計或訂單篩選 API，必須先回到 Propose 更新本 spec，不得在 UI 工作中默默擴大 contract。

預計新增：

- `POST /api/admin/users/{userId}/roles/seller`：僅 `ROLE_ADMIN`，授予賣家角色；成功 `204 No Content`、使用者不存在 `404`、已是商家 `409`。
- `DELETE /api/admin/users/{userId}/roles/seller`：僅 `ROLE_ADMIN`，撤銷賣家角色；仍持有商品時 `409`。
- `GET /api/seller/products`：僅 `ROLE_SELLER`，列出目前商家的商品。
- `GET /api/seller/orders` 與 `GET /api/seller/orders/{id}`：僅 `ROLE_SELLER`，回傳僅屬於目前商家的訂單項目。

既有 `POST`、`PUT`、`DELETE /api/products/**` 將允許 `ROLE_SELLER` 或 `ROLE_ADMIN`，但所有權仍由 Service 層強制驗證。既有 admin order API 保持僅 `ROLE_ADMIN`；不直接開放給 seller。

商家訂單 response 可回傳買家 email，讓商家履約時能聯絡買家；僅回傳目前商家的訂單項目，不回傳其他商家資料或整單總額。

前端需要依登入 response／JWT 中的 role 顯示入口，但不可將 role 判斷作為授權保護。seller 後台必須使用新增的 seller-scoped API，不能從公開商品或 admin 訂單 API 在 client-side 過濾資料。

## 資料影響

- 新增 Flyway migration `V5__add_seller_ownership.sql`，以 `INSERT IGNORE` 建立 `ROLE_SELLER`。
- `product` 新增可索引的 `seller_id` 外鍵，連至 `users.id`。
- `order_item` 新增可索引且有外鍵的 `seller_id`，作為下單當時的賣家快照。
- 既有資料 migration 時允許 `seller_id` 為 `NULL`：舊商品僅由 `ROLE_ADMIN` 管理，不能被商家認領。seller 新增的商品必須有非空 seller；平台 admin 新增的平台商品可維持 `NULL`。此策略不會任意將既有商品分配給錯誤商家。
- production 目前尚未建立商品或訂單，但 migration 必須同樣能安全套用到有既有資料的環境。

本次 UI/UX redesign 無資料模型、schema、migration、seed data 或既有資料處理變更；所有畫面仍使用既有角色、商品 owner 與訂單 seller snapshot。主題偏好若需要保存，只存於 browser local preference，不寫入使用者、商家或訂單資料。

## 商業與安全規則

- `ROLE_ADMIN` 是平台全站管理員；`ROLE_SELLER` 是受審核商家；一般註冊者只有 `ROLE_USER`。
- 只有 `ROLE_ADMIN` 可以授予或撤銷 `ROLE_SELLER`；公開註冊與任何 client request 不得自行指定或變更角色。
- 商品 owner 由伺服器從已驗證的登入者設定，不能相信 request body 的 user/seller ID。
- `ROLE_SELLER` 只能操作 owner 為自己的商品，並只能讀取自己的訂單項目；越權操作回傳 `403`。
- 商家不可修改、刪除或查閱其他商家資源；平台 admin 可跨商家管理。
- 商家訂單 response 只可包含買家 email 與該商家的訂單項目；不得包含買家密碼、其他使用者資料、其他商家 item 或整單總額。
- 前端不可接受或送出商品 owner／seller ID；後端是唯一可決定資源歸屬的地方。
- 訂單的商品名稱、單價與 seller 均為歷史快照，商品後續變更不得改寫已下單資料。平台自營／歷史商品的 seller snapshot 可以是 `NULL`，其訂單只由平台 admin 處理，不會出現在任何 seller 的訂單結果中。
- 授予與撤銷賣家角色的操作須記錄不含敏感值的 audit log（操作者、目標 user ID、時間、動作）。

## 錯誤與邊界情況

- 未登入存取 seller/admin API：`401`；角色不足：`403`。
- 目標使用者、商品或訂單不存在：`404`。
- 對已是 seller 的使用者再次授予角色，或對非 seller 撤銷角色：`409`。
- 商家嘗試操作不屬於自己的商品或讀取不含其商品的訂單：`403`。
- 訂單含多家商商品時，商家 response 只包含自己的 item；不得洩漏其他 item 與全單 `priceSum`。
- 建立訂單時，seller 商品必須寫入其 seller snapshot；平台自營／歷史商品可寫入 `NULL` seller snapshot，公開結帳流程仍須成功。

## 測試策略

- Service unit test：商品 owner 指派、跨商家修改／刪除拒絕、seller role 授予／撤銷、撤銷前商品檢查、下單 seller snapshot。
- Controller/security test：seller/admin/user 各角色的 endpoint 授權、公開註冊不得指定角色、越權回傳 `403`、response 不含其他商家 item 與全單總額。
- MySQL integration test：Flyway V5 可由既有 schema 套用、外鍵／index 正確、舊商品為 `NULL` owner 時的 admin-only 行為，以及多商家訂單的查詢隔離。
- 前端測試：各角色看到的入口正確、seller 商品／訂單頁只使用 scoped API、role 變更後重新登入可看到正確功能；端對端驗證商家 A 無法透過畫面或直接 request 取得商家 B 資料。
- UI redesign 測試：以 React Testing Library 覆蓋工作空間切換、module registry 的 role／workspace filtering、route metadata 的 active state／breadcrumb、主題模式切換與 preference fallback、商家角色授予／撤銷確認與 `409` 回饋、商品 empty／error／分頁狀態、訂單 scope 欄位、窄螢幕導覽、可辨識的互動名稱與實用型視覺檢查；每項測試需對應 `AC-UI-*`／`SCN-UI-*`，並確認 desktop 與 mobile 導覽使用同一份 registry。
- 主題驗收：至少在淺色、深色與跟隨系統三種模式檢查主要頁面，確認文字／背景／邊框／focus／錯誤與成功狀態的對比、鍵盤操作與資料 scope 不變；若專案採用 automated accessibility tooling，將兩種固定主題納入 contrast smoke test。
- 執行 `./mvnw test` 與 `./mvnw verify`；staging 建立至少兩個 seller 與跨商家訂單驗收資料後，手動驗證隔離。

## 實作 Todo

- [x] 已決定商家訂單 response 可包含買家 email，但只包含該商家的訂單項目，且不包含整單總額或其他商家資料。
- [x] 在 `frontend/` 定義並實作 seller/admin 後台畫面與 API 對照；公開商店、平台管理與商家中心有分開導覽，窄螢幕可切換為直向版面。
- [x] 新增 V5 migration：建立 `ROLE_SELLER`、`product.seller_id`、`order_item.seller_id`、外鍵與 index；以 MySQL integration test 驗證 schema。
- [x] 更新 `Product`、`OrderItem` mapping；既有 `User`／`Role` mapping 繼續提供角色集合，不以 EAGER 解決商品 owner 查詢。
- [x] 新增 admin-only seller role 授予／撤銷 use case、Service transaction 與不含敏感資料的 audit log。
- [x] 調整商品 Service／Repository，使新增商品強制套用登入 seller，seller 只能管理自己的商品，admin 可跨商家管理。
- [x] 新增 seller 商品列表 API 與 controller/security test。
- [x] 在下單 Service 寫入 seller snapshot；seller 商品保存 seller ID，平台自營／歷史商品可結帳並保存 `NULL` snapshot。
- [x] 新增 seller 專用訂單查詢與 response DTO，確保多商家訂單不洩漏其他 item 或整單金額。
- [x] 補齊 unit、controller/security 與 MySQL integration tests。
- [x] 在 `frontend/` 實作平台管理員的 seller 授予／撤銷頁、商家中心、我的商品與我的訂單頁，並加入對應測試。
- [x] 更新 API、角色、上架與 migration 文件；以 Java 21 + MySQL 執行 `./mvnw verify`，並完成前端測試與 production build。
- [ ] 部署至 staging 後，以兩個 seller 與一筆跨商家訂單手動驗收商品及訂單隔離，再決定是否發 production release。

## UI/UX redesign Todo（2026-09-12）

- [x] 建立第一段共用後台 shell、header workspace／theme controls、breadcrumb、scope 提示與一致的 responsive layout，並讓 admin／seller 側欄由同一份 module registry 產生；涵蓋 `AC-UI-001`／`AC-UI-005`／`AC-UI-007`。完整頁面流程仍待後續段落完成。
- [ ] 將視覺調整為中性、緊湊、以列表操作為主的商務後台，移除裝飾性 dashboard 卡片與 AI 式文案；涵蓋 `AC-UI-006`／`SCN-UI-012`。
- [ ] 重整 admin 概覽、商家權限、全站商品與全站訂單的資訊架構與操作確認；涵蓋 `AC-UI-001`／`AC-UI-002`／`AC-UI-005`。
- [ ] 重整 seller 概覽、我的商品與我的訂單流程；保留 owner／seller-scoped API contract，不新增 `sellerId` 欄位；涵蓋 `AC-UI-003`／`AC-UI-004`。
- [ ] 補齊 loading、empty、error、401、403、409、refresh 失敗與刪除後分頁回退的畫面狀態；涵蓋 `SCN-UI-005`／`SCN-UI-007`／`SCN-UI-010`。
- [ ] 補 React Testing Library 與必要的 route／manual validation，逐項填寫 Verification and Acceptance 的實際證據；涵蓋 `SCN-UI-001`–`SCN-UI-011`。
- [ ] 在窄螢幕與鍵盤操作下完成人工驗收，確認主要查詢、授權與商品／訂單操作不被裁切；涵蓋 `AC-UI-005`／`SCN-UI-011`。
- [ ] 建立後續模組的 extension checklist：module key、workspace／role／scope、route／breadcrumb、導覽群組／排序、API／資料影響、各狀態與 responsive 操作；涵蓋 `AC-UI-007`／`SCN-UI-013`／`SCN-UI-014`。
- [x] 建立共用主題 token 與 header theme selector，支援 system／light／dark，並先套用到共用 shell 與 module registry 導覽；既有資源頁面的狀態元件全面套用、contrast 與瀏覽器偏好人工驗收仍待後續完成。
- [ ] 補主題 preference persistence、system preference change 與 local preference 失敗 fallback 測試；涵蓋 `SCN-UI-016`。
- [ ] 在淺色／深色主題下完成 keyboard、responsive 與 contrast manual／automated smoke check，將實際證據填入驗收對照表；涵蓋 `AC-UI-008`。

## 第一段實作進度（2026-09-12）

- [x] 新增共用 `BackofficeShell`、module registry、admin／seller 導覽群組、active state、breadcrumb、scope 提示與窄螢幕版面。
- [x] 新增 `ThemeProvider` 與 header icon theme menu，支援 system／light／dark、local preference 與 storage 失敗 fallback；窄螢幕以緊湊按鈕開啟文字選單。
- [x] 補上 module registry、shell、theme context 與 App header component tests。
- [x] `frontend/npm test -- --watchAll=false`：9 suites、28 tests passed。
- [x] `frontend/npm run build`：production build compiled；僅保留既有頁面的 React Hook dependency warnings；同時補上 Bootstrap surface／text theme token，避免深色主題商品卡片低對比。
- [x] 補上 Bootstrap table striped／hover 的文字與背景 theme token，避免深色主題偶數列 hover 時商品名稱對比不足；深色全站商品頁 smoke check 通過。
- [x] 以 Chrome 實際操作 `localhost:3001`：主題 selector 可切換淺色／深色、公開商品卡片在深色下可讀、375px 窄螢幕可收合／展開 header 導覽、未登入進入 `/admin` 會導向 `/login`。
- [x] 以 Chrome 嘗試文件中的 admin／user 帳號；兩組帳號均已失效，已更新 `frontend/README.md` 移除過時固定 credential，並在本機 feature preview test DB 建立 disposable admin fixture（帳號與密碼不提交到 repository）完成後台人工驗證。
- [x] Chrome admin smoke：登入後驗證 `/admin` 平台管理 scope、工作台／營運／管理三組側欄、active state、breadcrumb，以及全站商品、全站訂單、商家權限三個模組的 route 與資料載入。
- [x] Chrome admin responsive smoke：375px 下後台導覽分組仍可操作，header toggle icon 具備足夠對比、可展開工作空間／主題／帳號控制，長帳號以單行省略呈現，且沒有水平捲動；淺色主題在 `/admin/sellers` reload 後仍保留。
- [ ] 尚未完成商品／訂單頁面全面套用新 shell、所有流程的人工驗收，以及完整 contrast／RWD／鍵盤驗收。

## 本次驗收結果（2026-09-12）

### 已通過

- [x] Backend Java 21 + MySQL acceptance suite：`docker-compose.test.yml` 執行 85 tests，0 failures、0 errors；Flyway V5/V6 migration 與 seller security／scope integration tests 通過。
- [x] Frontend acceptance suite：9 suites、28 tests passed。
- [x] 純 admin 登入後顯示平台管理與全站商品／訂單／商家權限入口；純 seller 登入後只顯示商家中心與本店商品／訂單入口，直接進入 `/admin` 會顯示無權限訊息。
- [x] 以 seller A 建立商品、以 seller B 建立另一商品；資料庫確認 owner 分別為目前登入者，表單未提供 `sellerId`。
- [x] 一般 buyer 從公開商品頁加入兩個不同 seller 的商品並成功建立跨商家訂單；admin 可看到訂單總額 `358.01` 與兩個 item。
- [x] 同一筆跨商家訂單由 seller A 查看時只顯示本店 item／小計 `123.45`，seller B 只顯示本店 item／小計 `234.56`，未顯示其他 seller item 或整單總額。
- [x] 嘗試撤銷仍擁有商品的既有 seller，後端回傳 `409`，seller 狀態保留，畫面顯示阻擋原因。
- [x] Chrome 驗證後台深色／淺色切換、訂單詳情資料不變，以及手動選擇淺色重新載入後仍保留；驗收結束後恢復深色。
- [x] 角色變更現在會先顯示確認對話框；撤銷商品持有者收到 `409` 時，畫面保留原狀態並提供「前往全站商品」入口。
- [x] 商家商品列表新增圖片欄位與無圖片 fallback；商品清單為空時顯示下一步 CTA。
- [x] 商家刪除商品前新增確認，取消不會送出 DELETE；刪除失敗仍保留原列表並顯示錯誤。
- [x] 商家訂單列表新增空狀態與下一步 CTA；`created_at` 由 V6 migration 回填既有 NULL、設為 NOT NULL 並提供資料庫 default，新訂單 API 不再產生 NULL 時間。
- [x] Chrome 驗證 seller 商品頁的圖片欄位與訂單時間，訂單 #2／#3 顯示實際建立時間，不再顯示 1970 日期。
- [x] 本次驗收重跑：Frontend `npm test -- --watchAll=false` 為 9 suites、28 tests passed；Chrome seller 訂單詳情只顯示 seller A item／小計 `123.45`，admin 訂單 #3 顯示兩個 item／總額 `358.01`。
- [x] 本次驗收重跑：Chrome 驗證角色變更確認對話框、淺色主題 reload persistence，並於結束時恢復深色主題。
- [x] 本次補充驗收：臨時授予 disposable admin fixture `ROLE_SELLER` 後，重新登入可切換 seller／admin workspace；驗收結束已撤銷該角色並重新登入恢復純 admin session。
- [x] 本次補充驗收：seller 直接進入 `/admin` 顯示無權限；主題選單可用鍵盤 Enter 開啟、Escape 關閉；local preference 讀取／寫入失敗仍可維持 system／目前頁面主題切換。

### 未通過或仍需修正

- [ ] 完整 keyboard、system preference change、商品圖片實際上傳／Cloudinary failure 與完整 401／外部錯誤流程尚未完成簽核；403、409、刪除失敗與 local preference fallback 已驗證。

## UI/UX redesign 驗收對照

| Acceptance | Scenario | 測試／驗證方式 | Result | Evidence |
|---|---|---|---|---|
| `AC-UI-001` | `SCN-UI-001`、`SCN-UI-002`、`SCN-UI-003` | route／navigation component test + Chrome authenticated／unauthenticated smoke check | passed | `BackofficeShell.test.js`、`backofficeModules.test.js`、純 admin／純 seller 與雙角色 fixture Chrome smoke；雙角色可切換 seller／admin workspace |
| `AC-UI-002` | `SCN-UI-004`、`SCN-UI-005` | seller role management component test + API mock + Chrome 409 check | passed | `SellerRoleManagement.test.js` 驗證確認／取消與 409 link；Chrome 驗證阻擋原因、原狀態與「前往全站商品」入口 |
| `AC-UI-003` | `SCN-UI-006`、`SCN-UI-007` | product page component test + Chrome seller create／owner check | in progress | seller A／B 建立商品與 owner snapshot、圖片欄位、空商品 CTA、刪除確認／失敗保留列表已通過；完整上傳人工流程仍待補 |
| `AC-UI-004` | `SCN-UI-008`、`SCN-UI-009` | seller/admin order component test + Chrome cross-seller order check | passed | 訂單 #3 seller A／B scope、admin 完整詳情與實際建立時間通過；`OrderListPageForSeller.test.js` 覆蓋 NULL fallback |
| `AC-UI-005` | `SCN-UI-010`、`SCN-UI-011` | responsive manual check + accessibility-oriented component test | in progress | 409、商品／訂單 empty state 與 seller 403 文案已補；完整 keyboard、401／外部錯誤流程待補 |
| `AC-UI-006` | `SCN-UI-012` | visual review + compact resource-list usability check | in progress | Chrome 後台 shell／列表視覺 smoke 通過；完整頁面流程與日常操作 review 待補 |
| `AC-UI-007` | `SCN-UI-013`、`SCN-UI-014` | module registry contract test + route/role filtering + responsive navigation check | in progress | `backofficeModules.test.js`、`BackofficeShell.test.js`、Chrome admin 三組模組 route 與 375px 導覽；完整新增模組流程待補 |
| `AC-UI-008` | `SCN-UI-015`、`SCN-UI-016` | theme selector component test + preference fallback + light/dark contrast smoke check | in progress | `ThemeContext.test.js`、`App.test.js`、Chrome admin order detail light/dark、preference reload、keyboard menu 與 storage failure fallback；system preference change 與完整資源頁 contrast 待補 |

## Review remediation（2026-09-09）

### 背景與決策

未提交變更的 review 發現：平台 admin 原有的全站後台流程被 seller route 取代；admin 在 URL 授權上可新增商品、Service 卻要求 seller；既有無 `seller_id` 商品會在結帳時拋出未處理例外。為了不任意將歷史商品分配給錯誤商家，`seller_id = NULL` 明確定義為平台自營／歷史商品，而非無效資料。它們由 `ROLE_ADMIN` 管理，仍可公開結帳；其 `order_item.seller_id` 維持 `NULL`，因此不會錯誤出現在任何商家的待處理訂單。

本段只處理本次 review 已確認的回歸、效能、資料 migration、外部檔案一致性與測試缺口；不加入商家自行申請、商品移轉或商店設定。

### 驗收條件

1. 純 `ROLE_ADMIN` 可以使用獨立的平台後台，管理全站商品與完整訂單；純 `ROLE_SELLER` 只能使用自己的商家中心。
2. 純 `ROLE_ADMIN` 新增商品時建立平台自營商品（`seller_id = NULL`）；seller 新增商品時必須歸屬自己，兩者都不能指定任意 seller。
3. 既有或新建的平台自營商品可成功結帳；其訂單項目不會出現在 seller scoped API，admin 的全站訂單仍可見。
4. V5 即使遇到預先存在的 `ROLE_SELLER` 也不建立重複角色資料。
5. seller 訂單列表不會依每張訂單額外查詢買家；商家商品換頁在 access token 過期後能先 refresh 並正確處理失敗 response。
6. Cloudinary 圖片上傳成功、商品資料庫寫入失敗時，系統會嘗試刪除剛上傳的 asset 並記錄可診斷但不含敏感資料的 log。

### API 與資料影響

- 新增 `POST /api/admin/products`：僅 `ROLE_ADMIN`，建立平台自營商品，伺服器固定保存 `product.seller_id = NULL`；client 不可傳入或指定 seller。
- 既有 `POST /api/products`：`ROLE_SELLER` 建立自己的商品；純 `ROLE_ADMIN` 為相容既有全站商品管理流程，建立平台自營商品。若帳號同時具有兩個角色，應使用 `/api/admin/products` 明確建立平台商品。
- 不新增產品 owner request 欄位；seller 歸屬永遠由 endpoint 與已驗證身分決定。

### Review remediation Todo

- [x] 恢復純 admin 的全站商品／完整訂單管理 route 與導覽，保留 seller scoped route，並加入前端 route 測試。
- [x] 定義並實作 admin 建立平台自營商品、seller 建立自有商品的 Service／Controller 行為與 security test。
- [x] 允許平台自營／歷史商品結帳，保存 `NULL` seller snapshot，並補 service 與 controller 層的 checkout coverage。
- [x] 將 V5 seller role seed 改為可安全處理既有同名角色的寫法，並補 MySQL migration upgrade test。
  - [x] 使用獨立、暫時性的 MySQL test schema，先執行 V1–V4、預先插入一筆 `ROLE_SELLER`，再升級到 V5。
  - [x] 驗證升級後 `roles` 只有一筆 `ROLE_SELLER`，且 V5 的 product／order_item seller foreign key 與 index 均已建立。
  - [x] 將 upgrade database 納入 `docker-compose.test.yml`，使 CI 與本機的完整驗證使用相同的 MySQL 版本與步驟。
- [x] 將 seller 訂單買家資料改為批次讀取或 projection，避免 N+1，並補 service test。
- [x] 修正 seller 商品分頁的 token refresh 與失敗 response 處理，並補前端測試。
  - [x] 刪除商品成功後，依後端最新 `totalPages` 回到有效頁碼；不得停在已不存在的最後一頁。
  - [x] DELETE 非成功 response、網路失敗與 refresh token 失敗都必須保留既有列表並顯示可理解錯誤。
  - [x] 空商品清單的分頁維持第 1 頁且「下一頁」不可操作；修正 shared `Pagination` 的 React `className` warning。
  - [x] 新增前端測試：換頁 refresh、refresh 失敗、DELETE 失敗、刪除最後一筆後頁碼回退、空清單分頁。
- [x] 為商品 create/update 的 Cloudinary upload-to-database 流程加入失敗補償與測試。
  - [x] 商品圖片上傳成功後，若 seller 或 platform admin 的 create／update 寫入失敗，嘗試刪除剛上傳的 Cloudinary asset；不可覆蓋原本的業務錯誤 response。
  - [x] 補償刪除遇到 `IOException` 或 Cloudinary client 的 runtime failure 時，只記錄不含憑證的診斷 log，並保留原本的資料庫失敗結果。
  - [x] 補 controller test 覆蓋 seller create、seller update、platform admin create 的資料庫失敗，以及補償刪除的 checked／runtime failure 不改變原始錯誤。
    - [x] 回歸測試：platform admin 的補償刪除拋出 `IOException` 時，仍回傳原本的資料庫業務錯誤。
- [x] 重新執行受影響的前後端測試與 `./mvnw verify`；每一項修復經獨立 review 通過後才勾選。
