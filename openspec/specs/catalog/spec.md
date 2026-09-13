# Capability: 商品目錄

## Purpose

定義 public catalog access、product ownership、seller-scoped catalog operations 與商品圖片行為。

## Requirements

### Requirement: 公開使用者可以瀏覽商品

系統 SHALL 提供不需 authentication 的 paginated public product listing 與 product detail endpoint。

#### Scenario: 公開商品列表

- GIVEN visitor request `GET /api/products?page=0`
- WHEN 系統有可提供的商品
- THEN server 回傳包含 product id、name、price 與 public image URL 的 paginated response

#### Scenario: 找不到商品詳情

- GIVEN visitor request 的 product id 不存在
- WHEN server 查詢商品
- THEN server 以標準 error shape 回傳 `404`

### Requirement: Product ownership 由 server 推導

系統 SHALL 將 seller 建立的商品指派給 authenticated seller，不信任 client-provided owner 或 `sellerId`。

#### Scenario: Seller 建立所屬商品

- GIVEN authenticated seller 送出有效 multipart product data
- WHEN `POST /api/products` 成功
- THEN 商品指派給該 seller，response 回傳 `201`

#### Scenario: Admin 建立平台商品

- GIVEN authenticated admin 送出平台商品
- WHEN `POST /api/admin/products` 成功
- THEN 商品的 `seller_id = NULL`，response 回傳 `201`

### Requirement: Seller catalog view 受 scope 限制

系統 SHALL 提供 seller-scoped product listing，並在 seller update 與 delete operation 執行 owner checks。

#### Scenario: Seller 只看到自己的商品

- GIVEN seller A 擁有商品，seller B 擁有其他商品
- WHEN seller A request `GET /api/seller/products?page=0`
- THEN response 只包含 seller A 的商品

#### Scenario: Admin 可以管理所有商品

- GIVEN authenticated admin
- WHEN admin 讀取、更新或刪除任一 seller 擁有的商品
- THEN operation 在一般 validation 與 resource existence checks 通過後允許執行

### Requirement: 商品圖片處理會保留既有圖片

系統 SHALL 透過 Cloudinary service 驗證 uploaded files；沒有新圖片的 update SHALL 保留既有 image information。

#### Scenario: 圖片上傳成功

- GIVEN image file 符合設定的大小與 MIME limits
- WHEN 系統處理 product create 或 update
- THEN 儲存 image URL，並將 external resource 與商品關聯

#### Scenario: Update 未提供新圖片

- GIVEN 商品已有 image metadata
- WHEN authorized update 只修改 name 或 price
- THEN 原有 image metadata 維持不變
