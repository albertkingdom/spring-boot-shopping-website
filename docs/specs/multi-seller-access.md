# 多商家商品與訂單權限

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

- **公開商店**：既有商品瀏覽、註冊、登入、購物車與下單畫面維持不變；不顯示全站商家管理入口。
- **平台管理後台**：`ROLE_ADMIN` 登入後可在使用者列表搜尋已註冊帳號、查看目前角色，並執行「授予商家」或「撤銷商家」。撤銷因仍有商品而被拒絕時，顯示後端回傳的可行處理說明。
- **商家中心**：`ROLE_SELLER` 登入後顯示「我的商品」與「我的訂單」入口。只有 seller 看得到此入口；一般 user 與純平台 admin 看不到也無法以 URL 繞過後端檢查。平台 admin 的跨商家工作留在平台管理後台，不能假裝成任一商家讀取 seller-scoped API。
- **我的商品**：顯示目前商家自己的商品，提供新增、修改與刪除。表單不顯示或提交 `sellerId`；新增後由後端歸屬於登入商家。
- **我的訂單**：只列出含有目前商家商品的訂單。詳情只顯示買家 email、此商家的商品項目、數量、單價與「本店小計」；不顯示其他商家項目或整張訂單總額。
- **平台管理者的 seller 操作**：平台管理者在商品與訂單後台維持全站視角，並可切換檢視商家歸屬；這不等同商家的「我的」視角。

第一版以既有設計系統與桌面後台流程為優先，畫面需能在窄螢幕完成基本查詢與操作；不在本功能加入商店頁、品牌樣式或賣家自訂外觀。

## API 影響

既有公開 `GET /api/products/**` 與一般使用者 `POST /api/order` 維持相容。

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
