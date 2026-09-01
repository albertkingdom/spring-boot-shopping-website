# Order Immutability & Historical Snapshots

專案內部知識庫，為什麼 `OrderItem` 存 product 名稱與單價的**快照**，而不是每次讀單時去 join `product` 表。

## 一句話

**歷史訂單是事實紀錄，不是動態視圖**。訂單成立當下的商品名稱、單價、規格必須永久凍結，之後商品怎麼變都不影響已成立的訂單。

## 問題場景

假設 `order_item` 只存 `product_id` + `quantity`（沒有 snapshot）：

1. **11 月**：使用者買了 3 件 `T-Shirt`，單價 `$199`，總額 `$597`
2. **12 月**：行銷把商品改名成 `T-Shirt (Winter Sale)`、價格改成 `$99`
3. **1 月**：使用者打客服問「11 月的訂單為什麼是這個金額」→ 後端 join 現在的 product → 客服看到 `T-Shirt (Winter Sale) @ $99`，跟訂單總額 `$597` 對不起來 → **信任崩塌**

或更糟：
4. **2 月**：商品下架、`product` 表 row 被刪 → 讀舊訂單 `productRepository.findById(...)` 拋例外 → 使用者永遠看不到自己買了什麼

## 解法：Snapshot pattern

下單當下把 product 的關鍵欄位**拷貝**到 order_item：

| 欄位 | 型別 | 意義 |
|---|---|---|
| `product_id` | BIGINT | 指向 product（給後續分析用，不影響顯示） |
| `product_name` | VARCHAR(255) | 下單當下的商品名稱 |
| `unit_price` | DECIMAL(10, 2) | 下單當下的單價 |
| `quantity` | INT | 買了幾件 |

顯示訂單時：
```java
new OrderItemDetail(item.getProductName(), item.getUnitPrice(), item.getQuantity())
```

**不 join product 表**。歷史資料完全自足。

## 什麼欄位需要 snapshot、什麼不用

**要 snapshot**（會影響帳目、法律、對帳的欄位）：
- 商品名稱、單價
- 規格（size, color, sku）
- 稅率、折扣率
- 使用者當下的地址（配送地址是「當時」的地址，不是「現在」的）
- 使用者當下的姓名（發票用）

**不要 snapshot**（只是連結，追蹤用）：
- `product_id`：讓行銷後台可以查「這個商品賣了幾單」
- `user_id`：連到帳號
- 圖片 URL：如果商品被刪，圖片可能也沒了；可以 snapshot 但不是必要

**判斷準則**：問「未來這個欄位改了，這張訂單的顯示應該跟著改嗎？」答否 → snapshot。

## 專案內的實作

### Entity

```java
@Entity
@Table(name = "order_item")
public class OrderItem {
    private Long id;
    @ManyToOne @JoinColumn(name = "order_id")
    private Order order;

    private Long productId;
    private Integer quantity;

    @Column(name = "product_name")
    private String productName;      // ← snapshot

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;    // ← snapshot

    public static OrderItem snapshotOf(Product product, Integer quantity) {
        OrderItem item = new OrderItem(product.getId(), quantity);
        item.productName = product.getName();
        item.unitPrice = product.getPrice();
        return item;
    }
}
```

Static factory `snapshotOf` 讓「拷貝當下值」這件事有一個明確入口，未來加新的 snapshot 欄位只改一個地方。

### 下單時使用

```java
Product product = productServiceImpl.getProductById(i.getProductId());
OrderItem orderItem = OrderItem.snapshotOf(product, i.getProductCount());
newOrder.addOrderItem(orderItem);
```

### 讀單時使用

以前：
```java
Product product = productRepository.findById(item.getProductId()).orElseThrow(...);
new OrderItemDetail(product.getName(), product.getPrice(), item.getQuantity())
```

現在：
```java
new OrderItemDetail(item.getProductName(), item.getUnitPrice(), item.getQuantity())
```

**副作用好處**：
- 少了對 `product` 表的 N+1 查詢（100 項訂單以前 100 次 product 查詢，現在 0 次）
- `OrderServiceImpl` 不再依賴 `ProductRepository`，可以刪掉這個依賴
- 商品被刪也不會讓訂單讀取失敗

## 遷移策略

V3 只加欄位、不 backfill：

```sql
ALTER TABLE order_item
    ADD COLUMN product_name VARCHAR(255) DEFAULT NULL,
    ADD COLUMN unit_price   DECIMAL(10, 2) DEFAULT NULL;
```

**新訂單**永遠有 snapshot（controller 現在強制填）。**舊訂單** snapshot 為 NULL —— 這是可接受的，因為：
- 現在 prod 沒資料
- 之後若有真的 prod 資料，可寫 backfill migration（拿目前 product 值填入 NULL 欄位），並在文件註明「這些訂單的 snapshot 是事後補的，反映的是補的當下的商品狀態，不是原始下單狀態」

**backfill migration 範例**（現在不需要，未來需要時參考）：

```sql
-- V4__backfill_order_item_snapshots.sql（假設）
UPDATE order_item oi
INNER JOIN product p ON p.id = oi.product_id
SET oi.product_name = p.name,
    oi.unit_price = p.price
WHERE oi.product_name IS NULL;
```

## 進階：不可變 vs 完全鎖死

現在 snapshot 是**下單時凍結**，但**技術上仍可用 SQL 直接改**（`UPDATE order_item SET unit_price = 0`）。真要「絕對不可改」，常見做法：

1. **DB trigger**：`BEFORE UPDATE` 拒絕改任何 snapshot 欄位
2. **Application 層**：不要有 setter，或 setter 檢查 order 已 confirmed
3. **Append-only 設計**：訂單狀態改用事件流（events table）記錄

對這種規模的專案是 over-engineering。但如果涉及金融、稅務、監理，DB trigger 或 audit table 常見。

## 為什麼不用 database view 或 report table

- **View**：join 現在的 product → 一樣有前面說的問題，view 只是把邏輯藏起來
- **Report / analytics table**：適合做「昨天賣了幾件」這種彙總，不適合作為單筆訂單顯示的來源
- **Data warehouse ETL**：勤跑 ETL 從 prod 抽 snapshot 到 warehouse。可以，但那是 warehouse 用，OLTP 端訂單顯示還是該自己有 snapshot

## 常見反例

- **只存 product_id + join** → 前面所有問題
- **snapshot 錯欄位**（例如漏掉稅率）→ 後來稅率改了，舊訂單稅金對不上
- **snapshot 但用 mutable 欄位存 reference**（例如 snapshot Product 整個物件序列化到 blob）→ 很難查詢、schema 演化痛苦
- **snapshot 但寫入時 race condition**（例如兩個並發下單一個看到舊價、一個看到新價）→ 不影響 immutability，但意外可能出現非預期價格。用 `@Transactional` + 讀取商品當下鎖行可避免

## 相關

- `docs/money-types.md`：unit_price 用 BigDecimal 的理由
- `docs/database-migration.md`：新增欄位的 migration 慣例
- `ARCHITECTURE_TODO.md` P1「在 `OrderItem` 保存下單當下的商品名稱與單價快照」（本 PR 完成）
- `AGENTS.md` 第 29 行「Order item 必須保存下單當時所需的歷史資料，不可只依賴商品目前的名稱與價格」
