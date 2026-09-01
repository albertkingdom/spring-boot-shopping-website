# Database Migration with Flyway

專案內部知識庫，說明本專案如何用 Flyway 管理 MySQL schema，以及為什麼要放棄 `ddl-auto=update`。

## 一句話

**Flyway 是資料庫的 Git**：手寫版本化的 SQL 檔，工具照順序執行、記錄哪些跑過、拒絕事後改動；env 之間 schema 保證一致。

## 為什麼放棄 `ddl-auto=update`

Hibernate 的 `ddl-auto=update` 讓 app 啟動時自動比對 `@Entity` 與 DB，缺欄位就加。看似方便，實際上 7 個致命問題：

1. **沒紀錄** —— 誰在何時加了哪個欄位，Git 找不到，DB 也沒 log。
2. **只加不刪** —— entity 刪的欄位，DB 永遠留著佔空間、帶著舊資料。
3. **沒原子性** —— 一次改多個欄位，中間掛掉留下半改狀態。
4. **沒 rollback** —— 版本回退了，DB 欄位不會跟著回。
5. **環境不一致** —— dev/staging/prod 各自跑了不同版次的 update，schema 累積不同的歷史遺留。
6. **型別改動危險** —— `Float → BigDecimal` 這種轉型 Hibernate 可能什麼都不做、可能失敗、可能崩，行為不可預測。
7. **`create-drop` 意外用在 prod** —— 一秒清空整個 DB，發生過的血淋淋災難。

## Flyway 是什麼

Spring Boot 啟動時，Flyway 掃 `src/main/resources/db/migration/`，依版本順序執行還沒跑過的 migration，並在 DB 記一張 `flyway_schema_history` 表追蹤狀態。

### 檔案命名

```
V<version>__<description>.sql
```

- `V` 大寫開頭
- `version`：`1`、`1.1`、`2026.09.01.1200`（字典序排）
- 雙底線 `__` 分隔
- description 用底線分單字

範例：`V2__seed_roles.sql`、`V3__product_price_to_decimal.sql`

### `flyway_schema_history` 表

Flyway 自動建立，記錄每次執行：

| version | description | checksum | installed_on | success |
|---|---|---|---|---|
| 1 | baseline | -123456789 | 2026-09-01 08:03 | true |
| 2 | seed roles | 987654321 | 2026-09-05 10:15 | true |

**Checksum** 是 migration 檔內容的 hash。啟動時 Flyway 重新計算並比對；不同 = `Validate failed`，應用起不來。這是「migration 一旦 merge 不能改」的執行機制。

### Baseline

「Baseline」用於既有 DB 已經有 tables、但 Flyway 沒紀錄的情境。

專案設定：
```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
```

行為分三種：
1. **空 DB**：Flyway 執行 V1 建 tables，寫入 history 記為 v1。
2. **已有 tables + 已有 history**：跳過 V1，接續 V2 以上。
3. **已有 tables + 沒有 history**（第一次上 Flyway 的 prod）：Flyway **不執行** V1，只把 history 打上「v1 baseline」，之後從 V2 開始。

## 本地開發流程

### 第一次開發（乾淨 DB）

`docker compose up mysqldb` → 起 MySQL → `./mvnw spring-boot:run` → Flyway 執行 V1 建 schema → app 起來。

### 加一個新欄位

例：`Product` 加 `description`。

1. **先改 entity**：`Product.java` 加 `private String description;`
2. **寫 migration**：`src/main/resources/db/migration/V2__add_product_description.sql`
   ```sql
   ALTER TABLE product ADD COLUMN description VARCHAR(1000) DEFAULT NULL;
   ```
3. **重啟 app**：Flyway 看到 V2 沒跑過，執行。
4. **`ddl-auto=validate`** 驗證 `Product.description` 對到 `product.description` column → 通過。
5. **Commit + PR**：entity 改動 + migration 檔同一個 commit（AGENTS.md #108「一個 commit 應是一個可理解、可回復的邏輯單位」）。

### Merge 進 master 後

**該 migration 檔永遠不能改**。理由：

- Prod 的 DB 已經跑過原版 V2，checksum 存進 history
- 你事後改 V2 → 下次啟動 checksum mismatch → **app 起不來**
- 修正方式：寫 **V3** 補救，不動 V2

## 兩階段 migration 與 zero-downtime deploy

改型別或改欄位名時，**同一個 migration 內做完** 會讓「舊版程式碼 + 新 schema」相衝，deploy 期間會出錯。

正確做法：拆兩階段。

例：`Product.price Float → BigDecimal`。

### V3 —— 加新欄位、搬資料、切換

```sql
-- 加新欄位（先加不改）
ALTER TABLE product ADD COLUMN price_decimal DECIMAL(10,2);

-- 資料搬運
UPDATE product SET price_decimal = CAST(price AS DECIMAL(10,2));

-- 舊欄位改名保留（回滾用）
ALTER TABLE product CHANGE COLUMN price price_old FLOAT;

-- 新欄位改名成正式名
ALTER TABLE product CHANGE COLUMN price_decimal price DECIMAL(10,2) NOT NULL;
```

Deploy V3 + 對應 entity 改動。此時舊版程式碼會失敗（讀 price 型別不對），所以 V3 必須跟新程式碼一起上。

### V4 —— 確認穩定後清舊欄位（下個版本或幾週後）

```sql
ALTER TABLE product DROP COLUMN price_old;
```

**中間期間內，你有時間 rollback**：如果 V3 上線後幾小時內發現嚴重問題，程式碼回退到舊版讀 `price_old`（先手動 rename 回 price），資料還在。

大公司 deploy 都走這種模式：**expand → migrate → contract**。

## 命名建議與範疇

- **DDL migration**（`CREATE`、`ALTER`）：schema 變動
- **DML migration**（`INSERT`、`UPDATE`）：資料變動，例：seed role、批次資料轉換

**不要混一個檔**。MySQL 不支援 DDL rollback，混合的 migration 中途掛掉留下部分結果最難救。

版本號建議用 **timestamp 格式** 避免多人並行時撞號：

```
V2026_09_01_1200__add_product_description.sql
V2026_09_02_0930__seed_roles.sql
```

字典序仍然對。

## 什麼時候寫 UNDO migration

Flyway 付費版有 `U<version>__xxx.sql` undo 檔。Community 版沒有，主流做法：**永不刪，只往前**。

- 錯了寫下一個 migration 修正（V4 修 V3）
- 真的要「回退」→ 手寫 rollback SQL 當作新 migration

## 專案設定

`pom.xml`：
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

Spring Boot 2.6 bundles Flyway 8.0.5，`flyway-core` 已含 MySQL 支援，不需要 `flyway-mysql`（那是 Flyway 9+ 才拆出來的模組）。

`application.properties`：
```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
```

- `ddl-auto=validate`：Hibernate 只驗證 entity 對到 table，**永遠不改 schema**
- `baseline-on-migrate=true`：允許在既有 DB 上首次啟用 Flyway

## CI 與 Testcontainers

CI 用 GitHub Actions 內建的 MySQL service，每次都是乾淨 DB → Flyway 執行 V1 → 測試。

之後 Batch 8（Java 21 升級）完成後，會導入 Testcontainers 讓本地 `./mvnw verify` 也自動起 MySQL container，不再需要手動 `docker compose up mysqldb`。目前 Java 8 + Testcontainers 1.19 對 Docker Desktop for Mac 相容有問題，延後處理。

## 常見錯誤

### `Validate failed: Migration checksum mismatch for migration version X`

有人事後改了已 merge 的 migration 檔。解法：**寫下一個 migration 補救**，不要動已 merge 的檔。緊急情況可 `mvn flyway:repair` 更新 checksum，但這繞過保護機制，只當救命用。

### `Detected resolved migration not applied to database: X`

DB 上有比目前程式碼**更新版**的 migration（例：DB 是 V5，程式碼只到 V4）。多發生在部署了新版又要回退舊版的場景。解法通常是把程式碼升到有 V5 的版本，或走 rollback 流程。

### `Schema-validation: missing table [X]`

`ddl-auto=validate` 找不到 table。表示 Flyway migration 沒建這個 table，或 entity 的 `@Table(name=...)` 跟 SQL 不符。檢查兩邊命名。

### `Schema-validation: wrong column type in table [X], column [Y]`

Column 型別跟 entity 不符。例：entity 用 `BigDecimal`，SQL 建 `FLOAT`。修 migration。

## Seed data

`V4__seed_roles.sql` inserts the two roles the application code references:

```sql
INSERT IGNORE INTO roles (name) VALUES ('ROLE_USER');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN');
```

`INSERT IGNORE` keeps it safe against databases that were manually seeded with the same names before Flyway was introduced. `UserService.register` looks up `ROLE_USER` by name (not id), so the actual id doesn't matter.

The two seed rows are the only seed the application must have to boot. Everything else (products, admin accounts) is bootstrapped separately.

## Admin bootstrap

Roles are seeded, but no admin *user* is auto-created. Admins are created out-of-band, per environment.

### Local / dev

```bash
# 1. Compute a bcrypt hash for the desired password:
htpasswd -bnBC 12 "" 'YourStrongLocalPassword!' | tr -d ':\n' | sed 's/$2y/$2a/'
# → prints something like $2a$12$...

# 2. Insert into MySQL:
mysql -h 127.0.0.1 -u root -p shopping <<SQL
INSERT INTO users (email, password, name) VALUES ('admin@example.com', '$2a$12$...', 'Local Admin');
INSERT INTO users_roles (users_id, roles_id)
    SELECT u.id, r.id FROM users u JOIN roles r ON r.name='ROLE_ADMIN' WHERE u.email='admin@example.com';
SQL
```

### Staging / prod

Prefer one of:
- **Cloud provider secrets + one-off admin CLI**: a small `main` class inside the app (behind a build flag or run mode) that reads `ADMIN_EMAIL` and `ADMIN_PASSWORD` from env, hashes with the same `BCryptPasswordEncoder`, and INSERTs via `UserService.register` + `addRoleToUser`. Run once from the deploy job, then leave the env vars unset.
- **Manual SQL through the platform's DB console** using the same INSERT pattern above.

**Never** commit an admin password (plain or hashed) or wire it into a Flyway migration. Migrations are source-controlled and identical across environments — a seed-in-migration admin means every environment shares the same credential and it lives in Git forever.

## 參考

- [Flyway docs — Command line](https://documentation.red-gate.com/fd/command-line-184127407.html)
- [Spring Boot Flyway auto-configuration](https://docs.spring.io/spring-boot/docs/2.6.x/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)
- `ARCHITECTURE_TODO.md` P1「使用 Flyway 或 Liquibase 建立資料庫 schema migration」與 P1「透過 migration 或僅限開發環境的 seed 建立 `ROLE_USER` 與 `ROLE_ADMIN`」
