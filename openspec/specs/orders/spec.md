# Capability: 訂單

## Purpose

定義 checkout calculation、order history snapshots、seller-scoped views、admin views 與訂單時間欄位行為。

## Requirements

### Requirement: Server 計算訂單總額

系統 SHALL 根據目前 database prices 與 authenticated buyer 計算訂單總額，不信任 client-provided totals 或 user ids。

#### Scenario: 已驗證使用者建立訂單

- GIVEN authenticated `ROLE_USER` 送出非空 items 且 quantity 為正數
- WHEN `POST /api/order` 成功
- THEN server 使用 `BigDecimal` 計算總額，將訂單關聯至 authenticated user，並回傳 `201`

#### Scenario: 無效訂單輸入會被拒絕

- GIVEN item list 為空、quantity 非正數或 product id 不存在
- WHEN server 處理 request
- THEN server 視情況回傳 `400` 或 `404`，且不建立有效訂單

### Requirement: Order item 保留歷史 snapshot

系統 SHALL 在 checkout 時於每個 order item 儲存 product name、unit price 與 seller ownership snapshot。

#### Scenario: Checkout 儲存 seller snapshot

- GIVEN 商品目前由 seller A 擁有
- WHEN buyer 訂購該商品
- THEN order item 儲存 seller A 的 id、checkout 當時的 product name 與 unit price

#### Scenario: 後續 catalog 變更不會改寫歷史

- GIVEN 既有 order item
- WHEN 商品被改名、改價、轉移 ownership 或刪除
- THEN order item 保留原始 name、unit price 與 seller snapshot

### Requirement: Seller order view 受 item scope 限制

系統 SHALL 提供 seller order list 與 detail endpoint，且只揭露 authenticated seller 的 order items 與 seller subtotal。

#### Scenario: Seller 讀取跨 seller 訂單

- GIVEN 訂單包含 seller A、seller B 與平台的 items
- WHEN seller A 呼叫 `GET /api/seller/orders` 或 `GET /api/seller/orders/{id}`
- THEN response 只包含 seller A 的 items 與 subtotal，不包含其他 items 或 full order total

#### Scenario: Seller 讀取無關訂單

- GIVEN 訂單不包含 seller A 的 item
- WHEN seller A request 該訂單 detail
- THEN server 回傳 scoped denial response，不揭露無關訂單資料

### Requirement: Admin order view 保持完整

系統 SHALL 允許 `ROLE_ADMIN` 讀取完整 order list 與 detail，包含所有 items 與 full order total。

#### Scenario: Admin 讀取跨 seller 訂單

- GIVEN admin 與一筆跨 seller 訂單
- WHEN admin request order list 或 detail
- THEN 回傳所有 order items、snapshots、DTO 允許的 buyer information 與 full total

### Requirement: 訂單 timestamp 會被持久化

系統 SHALL 為新訂單初始化 `orders.created_at`，並在套用 non-null database constraint 前 migrate 既有 null timestamps。

#### Scenario: 既有 null timestamp 會被 migrate

- GIVEN pre-V6 order 的 `created_at = NULL`
- WHEN Flyway V6 執行
- THEN timestamp 被 backfill，column 變成具有 database default 的 `NOT NULL`
