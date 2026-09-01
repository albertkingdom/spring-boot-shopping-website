# File Upload Safety

專案內部知識庫，商品圖片上傳流程的安全考量、驗證位置、暫存檔生命週期，以及 DB × 外部服務一致性。

## 一句話

**檔案上傳同時碰四個危險面**：路徑穿越、暴力大檔、偽裝格式、DB × 外部儲存不同步。每個都要獨立擋。

## 攻擊面 1：路徑穿越（path traversal）

### 錯誤實作（我們原本的樣子）

```java
Path tempFilePath = Paths.get(UPLOADED_FOLDER + file.getOriginalFilename());
Files.write(tempFilePath, bytes);
```

`file.getOriginalFilename()` 是**客戶端提供的字串**。攻擊者送 `productImage` 但 filename 是 `../../etc/passwd` 或 `../../.env`：

- 拼出來變 `./upload/../../etc/passwd`
- `Files.write` 順著 `..` 上溯到系統敏感檔案
- 覆蓋 `/etc/passwd` 或 `.env` → 提權 / 憑證外洩

Java `Path` 不會自動 normalize；`getOriginalFilename` 可以含 `/`、`\`、`..`、null byte，全部沒過濾。

### 正確做法

**永遠不要**把外部字串拼進本機路徑。用 `Files.createTempFile` 產生**隨機命名的檔名**：

```java
Path tempFile = Files.createTempFile("shopping-upload-", suffixFor(detected));
```

- 檔名由 OS 給隨機 UUID-like
- 位置在 OS temp 目錄（`java.io.tmpdir`）
- 攻擊者提供的原始檔名**完全不影響**磁碟操作

原始檔名如果要留給後續（例如 Cloudinary metadata），存進 DB 欄位而不是拼進 path。

## 攻擊面 2：暴力大檔（denial of service via size）

### 錯誤

沒有 size 限制。攻擊者傳 5GB 檔 → 磁碟塞爆 → 服務崩潰。

### 兩層 defense

**Layer 1 — Spring 層邊界**（`application.properties`）：

```properties
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=6MB
```

超過 → Spring 直接拒收 request，controller 都不會被呼叫到。

**Layer 2 — Service 層邊界**（`CloudinaryService`）：

```java
if (file.getSize() > maxUploadBytes) {
    throw new IllegalArgumentException(...);
}
```

`app.upload.max-bytes` env 可覆蓋。這一層是保險 — 萬一 Spring 設定被誤改、或有其他上傳路徑繞過 Spring 限制，service 這關擋得住。

**兩層設不同值是刻意的**：Spring 5MB / service 也 5MB 目前一致，但可調整 spring 略寬（例如 6MB）讓 service 給出更具體的錯誤訊息。

## 攻擊面 3：偽裝檔案格式（MIME spoofing）

### 錯誤

只信 `MultipartFile.getContentType()`。**這是 client 送的 header，可任意偽造**：

```
Content-Type: image/png    ← 攻擊者說是 png
但實際內容是 .exe / .php / .html + <script>
```

上傳到 Cloudinary → Cloudinary 也不見得檢查 → CDN 回傳給其他使用者 → XSS / 惡意執行檔散布。

### 正確做法

**檢查檔案 magic bytes**（前幾個 byte 的 signature）：

```java
try (InputStream in = new BufferedInputStream(file.getInputStream())) {
    String detected = URLConnection.guessContentTypeFromStream(in);
    if (detected != null) return detected;
}
// fallback: 用 client header
```

`URLConnection.guessContentTypeFromStream` 用 JDK 內建 magic byte 表判斷。
- PNG magic：`89 50 4E 47 0D 0A 1A 0A`
- JPEG：`FF D8 FF`
- GIF：`GIF87a` / `GIF89a`

如果 magic bytes 說是 image/png，就是 image/png，client header 說什麼不算。

**Whitelist 允許的類型**：

```java
private static final Set<String> ALLOWED_CONTENT_TYPES = ...
    "image/jpeg", "image/png", "image/webp", "image/gif";
```

**Whitelist 比 blacklist 好** —— blacklist 永遠有你沒想到的類型漏過（`.svg` 可以帶 script 就是經典）。

### 更嚴格的方案（未來考慮）

- **Apache Tika**：更完整的 magic byte 資料庫，能偵測更多偽造
- **ImageMagick / libjpeg 實際解碼**：能過 header 不代表 image 本身沒藏 payload（stegosploit）；解碼一次證明是有效圖片
- **Content Security Policy** on frontend：即使惡意 .html 上傳成功，CSP 讓 script 執行不起來

目前用 URLConnection + whitelist 對 side project 足夠。

## 攻擊面 4：DB × 外部服務一致性

### 場景

新增商品流程：
1. 收 form
2. 上傳圖到 Cloudinary → 拿 url + public_id
3. 存 Product 到 DB（含 url + public_id）

如果 (2) 成功 (3) 失敗 → Cloudinary 有孤兒圖檔，DB 沒紀錄。

如果 (3) 成功 (2) 失敗 → 沒發生因為 order 是 2 先 3 後。

反向：刪除商品
1. 讀 DB 拿 public_id
2. 刪 DB row
3. 刪 Cloudinary 圖

如果 (2) 成功 (3) 失敗 → DB 沒了但 Cloudinary 圖還在（孤兒圖檔）。

### 目前策略

**上傳流程**：
- Cloudinary 先，DB 後。DB 失敗 → 少數 Cloudinary 孤兒。
- **可接受**：孤兒圖不影響功能，只是浪費儲存。定期用 script 掃「Cloudinary 有但 DB 沒指到的 public_id」清理即可（未做）。

**刪除流程**：
- DB 先，Cloudinary 後
- Cloudinary 失敗只 `log.warn`，不 propagate 給客戶端
- **理由**：使用者的意圖是「刪掉商品」，DB 刪成功他就滿意；Cloudinary 那邊晚點清理沒影響 UX
- Log 是給運維端未來寫 reconciliation job 的線索

### 更嚴格的方案（未來考慮）

- **Outbox pattern**：每次 DB 變更同時寫一筆「待做的外部動作」到 outbox 表，另一個 worker 處理 outbox、可 retry。DB 與外部動作最終一致
- **Saga**：多步驟業務流程每步定義 compensation action
- **Two-phase commit**：跨 DB 與 Cloudinary（Cloudinary 不支援；沒實用價值）

對 side project 過度。目前「盡力清理 + log 讓運維知道」已經是合理起點。

## 暫存檔生命週期

**Rule: try-finally 保證清理**

```java
Path tempFile = Files.createTempFile(...);
try {
    file.transferTo(tempFile.toFile());
    Map<?,?> result = cloudinary.uploader().upload(tempFile.toFile(), ...);
    return new UploadedImage(...);
} finally {
    try {
        Files.deleteIfExists(tempFile);
    } catch (IOException cleanup) {
        log.warn("failed to delete temp upload file {}", tempFile, cleanup);
    }
}
```

- 成功走 return 之前先執行 finally → 刪
- 失敗走 throw 之前先執行 finally → 刪
- 刪除失敗自己 log warn，**不要覆蓋原本的 exception**（不要在 finally 內 throw，會 shadow 掉真正的錯誤）

## 「更新但沒上傳新圖」的正確行為

前端更新商品，只想改名字/價格，`productImage` 欄位空 → 後端**保留原本圖片**，不覆寫成 null：

```java
// ProductServiceImpl.updateProduct
if (product.getImgUrl() != null) {
    existedProduct.setImgUrl(product.getImgUrl());
}
if (product.getImgName() != null) {
    existedProduct.setImgName(product.getImgName());
}
```

`product` 是從 controller 收到的變更包，null 表示「沒帶」而不是「要清空」。

**這對應 P2 acceptance「更新商品未上傳新圖片時，保留原本的 `imgUrl` 與 `imgName`」**。

## 專案內的完整流程

### 上傳
```
Controller
  └─ file 為空？→ 不動 imgUrl/imgName
  └─ file 有？→ CloudinaryService.uploadImage(file)
                    └─ 檔大小檢查
                    └─ MIME detection (magic bytes)
                    └─ Files.createTempFile 亂數命名
                    └─ file.transferTo(temp)
                    └─ cloudinary.uploader().upload(temp, ...)
                    └─ finally: Files.deleteIfExists(temp)
                    └─ 回 UploadedImage(url, publicId)
Controller 建 Product entity → productService.saveProduct
```

### 刪除
```
ProductController.deleteProduct
  └─ getProductById → 拿到 imgName（public_id）
  └─ productService.deleteProduct（DB row 刪除）
  └─ cloudinaryService.deleteFile(imgName)
        └─ cloudinary.uploader().destroy(publicId)
        └─ result 非 "ok" / "not found" → log.warn
        └─ IOException → log.warn + rethrow
```

## 測試覆蓋

`CloudinaryServiceTest`（5 case）：
- 成功上傳，temp 檔被刪 ✅
- Cloudinary 抛例外，temp 檔仍被刪 ✅
- 空檔 → `IllegalArgumentException` ✅
- 超過大小 → `IllegalArgumentException` ✅
- 非圖片 content-type → `IllegalArgumentException` ✅

## 相關

- `docs/error-handling.md`：`IllegalArgumentException` 被 handler 映射到 400
- `docs/layered-architecture.md`：Cloudinary 是「外部服務」，其失敗處理走 service 層
- `ARCHITECTURE_TODO.md` P2「檔案上傳與 Cloudinary」

## 常見錯誤

### 用 `MultipartFile.transferTo(new File(userSuppliedName))`
路徑穿越同款。任何以外部字串命名檔案的操作都危險。

### 用 blacklist 排除危險副檔名
```java
if (filename.endsWith(".exe") || filename.endsWith(".php")) reject();  // ← 缺 .html, .svg, .htaccess, ...
```
用 whitelist 檢查允許的 content-type。

### 信 `MultipartFile.getContentType()`
Client header 可偽造。至少要 magic byte 確認。

### 沒有 size limit
攻擊者 5GB 檔灌爆磁碟。永遠設兩層。

### try 沒有 finally 清理 temp file
只在 happy path 刪除 → catch 分支洩漏一堆 temp 檔。用 try-finally 或 try-with-resources。

### DB 用 `@Transactional`，但 Cloudinary 在 transaction 內
`@Transactional` 只管 JDBC；Cloudinary 呼叫成功後 DB commit 失敗 → 需要 compensating action。這種跨系統動作**不應該**期待 `@Transactional` 幫你，要自己設計 fail-safe。
