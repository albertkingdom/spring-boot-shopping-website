# Money Types

專案內部知識庫，為什麼金額欄位一律用 `BigDecimal` + MySQL `DECIMAL`，不用 `Float`/`Double`。

## 一句話

**浮點數是近似值；錢必須是精確值。任何一次「差 0.01」在會計/金流上都是紅字事件。**

## 為什麼 Float / Double 不行

`float` 和 `double` 是 IEEE 754 二進位浮點數。可以精確表示 `0.5`、`0.25`、`0.125` 這種二進位分數，但無法精確表示 `0.1`、`0.2`、`0.3` 這種十進位分數。

實測：

```java
System.out.println(0.1 + 0.2);
// 0.30000000000000004
```

Bug 場景：

```java
float totalPrice = 0F;
for (int i = 0; i < 10; i++) totalPrice += 0.1F;
System.out.println(totalPrice);
// 1.0000001  ← 應該是 1.0
```

一百筆金額累加 → 誤差可能到分位、元位。訂單總額對不上明細，客服接電話、對帳單紅字、稽核來訪。

## 為什麼 BigDecimal 可以

`java.math.BigDecimal` 用**任意精度整數 + scale** 儲存：

```
BigDecimal price = new BigDecimal("199.99");
// 內部：unscaledValue=19999, scale=2
// 對外表現：199.99 精確不失真
```

`BigDecimal("0.1").add(BigDecimal("0.2"))` = `BigDecimal("0.3")` —— **完全精確**。

**代價**：BigDecimal 是物件，運算比 primitive 慢約 100x；記憶體多幾倍。但對每秒幾筆訂單的購物網站，這開銷可忽略。

## 使用規則

### 建立 BigDecimal 一律用 String 建構子

```java
// 對
new BigDecimal("0.1")

// 錯 —— double 已經失真
new BigDecimal(0.1)   // → 0.1000000000000000055511151231257827021181583404541015625
```

**永遠不要**把 `double` / `float` literal 丟進 `new BigDecimal(...)`。要嘛用 String，要嘛用 `BigDecimal.valueOf(double)`（後者內部走 `Double.toString` 轉字串再解析，值符合直覺）。

### 常用值有 constant

```java
BigDecimal.ZERO
BigDecimal.ONE
BigDecimal.TEN
```

用這些避免重複建立物件。

### 運算是 immutable

```java
BigDecimal a = new BigDecimal("100");
a.add(new BigDecimal("50"));    // 沒改到 a
BigDecimal b = a.add(new BigDecimal("50"));  // b = 150, a = 100
```

跟 `String` 一樣是 immutable。忘了接回傳值 = 運算沒生效。

### 除法必須指定 scale 和 rounding

```java
new BigDecimal("10").divide(new BigDecimal("3"))
// → ArithmeticException: Non-terminating decimal expansion; no exact representable decimal result
```

除法可能無限小數（10/3=3.333...）。BigDecimal 拒絕產生近似值，必須明講怎麼捨入：

```java
new BigDecimal("10").divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP)
// → 3.33
```

**RoundingMode 常見選擇**：
- `HALF_UP`：0.5 進位（一般四捨五入）
- `HALF_EVEN`：Banker's rounding（0.5 進到偶數；金融產業預設，統計上更公平）
- `DOWN`：無條件捨去
- `UP`：無條件進位

大部分金流用 `HALF_UP`；投資 / 交易系統偏好 `HALF_EVEN`。

### 比較用 `compareTo`，不用 `equals`

```java
new BigDecimal("1.0").equals(new BigDecimal("1.00"))  // false，scale 不同
new BigDecimal("1.0").compareTo(new BigDecimal("1.00")) == 0  // true
```

`equals` 同時比對 value 和 scale，`1.0` 跟 `1.00` 不相等。要比「數值」永遠用 `compareTo`。

## MySQL 側：DECIMAL

對應到 MySQL 的 `DECIMAL(precision, scale)`：

- `precision`：總位數
- `scale`：小數位數
- 整數位數 = precision - scale

例：`DECIMAL(10, 2)` → 最大 `99,999,999.99`（10 位總長，2 位小數）。

### 不要用 FLOAT / DOUBLE 存錢

MySQL `FLOAT` 是 IEEE 754 單精度、`DOUBLE` 是雙精度，跟 Java 的一樣有精度問題。存進 DB 讀出來已經失真。

### precision 選多大

看業務上限：

| 場景 | 建議 |
|---|---|
| 消費品價格 | `DECIMAL(10, 2)`（上限 $99M，遠超正常商品） |
| 訂單總額 | `DECIMAL(12, 2)`（大量商品也夠） |
| 加密貨幣 | `DECIMAL(30, 18)`（BTC 精度到 satoshi 為 8 位；ETH 到 18 位） |
| 匯率 | `DECIMAL(10, 6)` 或更高 |

過大 → 浪費空間。過小 → 值溢出後 MySQL 拋錯（strict mode）或截斷（非 strict）。

### JDBC 自動對應

Java 端 `BigDecimal` ↔ MySQL `DECIMAL`：JDBC 驅動自動轉換。**不需要自己手動格式化**。

## 專案內的實作

### Product.price

```java
@Column(precision = 10, scale = 2)
private BigDecimal price;
```

對應 MySQL：`price DECIMAL(10, 2)`。

### Order.priceSum

```java
@Column(name = "price_sum", precision = 10, scale = 2)
private BigDecimal priceSum;
```

**注意**：目前 `OrderController.saveOrder` 用相加：

```java
BigDecimal orderTotalPrice = BigDecimal.ZERO;
for (OrderRequestItem i : items) {
    BigDecimal unitPrice = productServiceImpl.getProductById(i.getProductId()).getPrice();
    BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(i.getProductCount()));
    orderTotalPrice = orderTotalPrice.add(lineTotal);
}
```

用 `add()`, `multiply()`, `BigDecimal.ZERO` —— 全精確運算，不會失真。

### 為什麼不採信前端傳的 `totalPrice`

前端有本事偽造 `totalPrice=1`：後端信了就用 1 塊錢賣掉 $10,000 商品。**訂單金額永遠由後端根據當下商品單價 × 數量重算**。這是專案 `AGENTS.md` 第 28 行金額規則的直接體現。

## Migration 策略

V2 把兩個欄位型別改掉：

```sql
ALTER TABLE product  MODIFY COLUMN price     DECIMAL(10, 2) DEFAULT NULL;
ALTER TABLE orders   MODIFY COLUMN price_sum DECIMAL(10, 2) DEFAULT NULL;
```

MySQL `MODIFY COLUMN` 對現有資料做自動轉型。`FLOAT → DECIMAL(10,2)` 是**降精度但更精確**的方向：FLOAT 的近似值會被四捨五入到 2 位小數。目前 DB 沒有 prod 資料，這個 lossy 是可接受的。

**如果之後遇到「線上有大量歷史資料要遷型別」的情境**，走 `docs/database-migration.md` 提到的兩階段模式：加新欄位 → 拷資料 → 換名 → 下一版清舊欄位。

## 訂單快照（下一個 PR 會做）

現在 `OrderItem` 只存 `productId` + `quantity`。如果商品之後改名或改價，歷史訂單顯示會跟著變 —— **不能接受**（客服對帳、稅務、法規）。

下一個 PR (`fix/order-item-snapshot`) 會在 `OrderItem` 加 `productName` + `unitPrice` 快照欄位，下單當下就存進去，之後商品怎麼改都不影響歷史訂單。

## 相關

- `ARCHITECTURE_TODO.md` P1「金額與訂單正確性」
- `docs/database-migration.md` 兩階段 migration 模式
- `AGENTS.md` 第 28 行「新增或修改金額欄位時使用 `BigDecimal`」

## 常見錯誤

### `ArithmeticException: Non-terminating decimal expansion`

除法沒指定 scale/rounding。修：`divide(divisor, scale, RoundingMode.HALF_UP)`。

### 序列化到 JSON 變成字串而不是數字

Jackson 預設把 `BigDecimal` 序列化成 JSON number，不是 string。如果前端拿到字串，檢查是否手動加了 `@JsonSerialize(using=ToStringSerializer.class)`。有些 JS 客戶端因為 JavaScript number 精度問題偏好字串，這是設計選擇。

### `Value out of range for column`

值超過 `DECIMAL(precision, scale)` 上限。修：放大 precision（別放大 scale，那會影響對外 API 精度）。

### 前端加減出現 0.01 誤差

前端用 JavaScript number（也是 IEEE 754 double）加減，會失真。前端展示金額用**字串傳遞**、必要時用 `decimal.js` / `big.js` 這類函式庫算，或就讓後端算好再送。
