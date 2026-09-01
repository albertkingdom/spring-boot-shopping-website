# Request DTOs and Mass Assignment

專案內部知識庫，為什麼 API 的 `@RequestBody` 一律用專門的 Request DTO，不用 JPA Entity。

## 一句話

**Entity 是「資料庫怎麼存」；Request DTO 是「API 允許外面設什麼」**。混用 = 讓攻擊者填任何 entity 欄位，包括本不該外露的 `id`、`roles`、`admin` flag 等。

## Mass Assignment 攻擊

### 攻擊示範

原本 `UserController.register` 收 `@RequestBody User`：

```java
@PostMapping("/api/register")
public ResponseEntity<?> register(@RequestBody User user) {
    userServiceImpl.saveUser(user);
    userServiceImpl.addRoleToUser(user.getEmail(), "ROLE_USER");
    ...
}
```

`User` entity 有這些欄位：`id`, `email`, `password`, `name`, `roles`。

攻擊者送：

```json
{
    "email": "attacker@example.com",
    "password": "abc123",
    "name": "Attacker",
    "id": 42,
    "roles": [{"id": 1, "name": "ROLE_ADMIN"}]
}
```

Spring 用 Jackson 反序列化 JSON 到 `User` 物件，欄位對應直接填入。`user.getRoles()` 現在有 ADMIN，`user.getId()` 是 42（覆蓋 auto-generated）。

`saveUser` 呼叫 `userRepository.save(user)` → **prod DB 出現一個匿名註冊來的 ADMIN 帳號**。

`addRoleToUser` 之後補的 `ROLE_USER` 是「疊加」，不是「覆蓋」，攻擊者仍是 ADMIN。

**這是一行 JSON 換整個系統管理權**。

### 歷史知名案例

- **GitHub (2012)** —— Rails 早期版本 `mass_assignment` 預設允許所有欄位。研究員 Egor Homakov 送一段修改過的 form data 給 GitHub commit endpoint，把自己的 SSH 公鑰塞進 Rails on Rails project → 成為該 repo 的 collaborator。GitHub 因此改預設值並道歉。
- **Nissan (2020)** —— 一個 API 允許 mass assignment 讓客戶可以修改「訂閱等級」欄位，免費升級 premium。
- 大量 Node.js Express + Mongoose 應用因為忽略 schema-level whitelist 中招。

## 解法：Request DTO

**永遠不要**用 entity 當 `@RequestBody` 型別。**永遠**用一個只暴露「合法可設欄位」的 DTO。

### 專案內範例

`RegisterRequest`：

```java
public class RegisterRequest {
    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6)
    private String password;

    @NotBlank
    private String name;

    // 完全沒有 id、roles、createdAt、admin 等欄位
}
```

Controller：

```java
@PostMapping("/api/register")
public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
    User user = new User();
    user.setEmail(request.getEmail());
    user.setPassword(request.getPassword());
    user.setName(request.getName());

    userServiceImpl.saveUser(user);
    userServiceImpl.addRoleToUser(user.getEmail(), "ROLE_USER");
    ...
}
```

攻擊者再送 `"roles":[...]`，Jackson **找不到 RegisterRequest.roles 欄位** → 直接丟掉（或視 Jackson 設定拋錯）。**攻擊面消失**。

### 測試方式

`UserControllerSecurityTest.register_ignoresRolesInRequestBody`：

```java
String body = "{\"email\":\"climber@example.com\",\"password\":\"secret1\",\"name\":\"Climber\","
        + "\"roles\":[{\"name\":\"ROLE_ADMIN\"}]}";

mockMvc.perform(post("/api/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isOk());

ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
verify(userServiceImpl).saveUser(saved.capture());
assertRolesEmpty(saved.getValue());
verify(userServiceImpl).addRoleToUser("climber@example.com", "ROLE_USER");
```

驗證即使 payload 帶 `roles`，`saveUser` 收到的 User 也沒 roles。這是**安全性測試的正確姿態**：斷言「壞 payload 無法達成攻擊」，而不只是「好 payload 正常運作」。

## Bean Validation

Request DTO 順便享有 `javax.validation` 的 declarative 驗證：

```java
@NotBlank    // 不可為 null、不可為空白字串
@NotNull     // 不可為 null（可空字串）
@NotEmpty    // 不可為 null / 空集合 / 空字串
@Size(min=6, max=50)
@Email
@Pattern(regexp = "...")
@Positive    // > 0
@PositiveOrZero
@Min(0)  @Max(100)
```

Controller 加 `@Valid`：

```java
public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request)
```

驗證失敗 → Spring 拋 `MethodArgumentNotValidException` → 全域 `ApiExceptionHandler` 轉成 400 JSON：

```json
{
    "message": "Invalid parameter",
    "errors": [
        {
            "resource": "registerRequest",
            "field": "email",
            "code": "Email",
            "message": "Not a valid email format."
        }
    ]
}
```

### 巢狀驗證用 `@Valid`

`CreateOrderRequest.items` 是 `List<CreateOrderItemRequest>`。要驗證每個 item 內的 `@NotNull productId` / `@Positive quantity`，需要在集合欄位上加 `@Valid`：

```java
@NotEmpty
@Valid           // ← 才會遞進去驗 items 裡每個 element
private List<CreateOrderItemRequest> items;
```

沒 `@Valid` → items 只驗 `@NotEmpty`（集合非空），內部 item 錯不驗。

## Request vs Response DTO

兩種 DTO **不要共用同一個類別**：

| Request DTO | Response DTO |
|---|---|
| 客戶端**送進來**的合法欄位 | 客戶端**看得到**的欄位 |
| 有驗證 annotation | 通常無驗證 |
| 通常 mutable（Spring 反序列化需要 setter） | 通常 immutable（final field + constructor） |
| 例：`RegisterRequest` | 例：`UserResponse` |

有時「一進一出」欄位 90% 相同，很誘惑用同一個類別。**別**。以後只要有一個欄位開始分歧（response 加了 `createdAt`，request 沒有）就得拆，屆時已散落多處。

## Controller 職責

現在 Controller 的 `register` 收 DTO 後**手動**建 User entity：

```java
User user = new User();
user.setEmail(request.getEmail());
user.setPassword(request.getPassword());
user.setName(request.getName());
```

這是**中繼**做法。理想的分層：

```
Controller (DTO in, DTO out)  →  Service (Command in, domain model out)  →  Repository (Entity in/out)
```

Batch 3 #8「refactor/mapper-layer」會把 DTO ↔ Entity 的 mapping 邏輯集中到 mapper class，controller 就只呼叫 mapper。目前 register/login 這種簡單 case 手寫 mapping 可讀性最高，先不 over-engineer。

## 專案內現況

已導入 request DTO 的 endpoint：

| Endpoint | Request DTO |
|---|---|
| `POST /api/register` | `RegisterRequest` |
| `POST /api/login` | `LoginRequest` |
| `POST /api/order` | `CreateOrderRequest` + `CreateOrderItemRequest` |

尚未（因為用 multipart / `@RequestParam` 而不是 JSON body）：

| Endpoint | 現況 |
|---|---|
| `POST /api/products/` | 幾個 `@RequestParam` + `@NotBlank` / `@Pattern`，已有 method-level validation。改成 `@ModelAttribute SaveProductRequest` 可以更整齊，但功能等價。留給後續小改善。 |

## 相關

- `docs/jwt-basics.md`：JWT payload 為什麼可以曝光但 secret 不能
- `AGENTS.md` 第 27 行「JPA Entity、API request DTO 與 response DTO 應分離」
- `ARCHITECTURE_TODO.md` P1「Entity 與 API DTO 分離」

## 常見錯誤

### `@Valid` 沒加，驗證沒發生

有寫 `@NotBlank` 但 controller 忘了 `@Valid` → Spring 不會驗，錯誤 payload 直接進 service。**在 Controller 的每個 `@RequestBody` 前加 `@Valid` 是硬規則**。

### 用 entity 當 request DTO 「暫時」

「先跑起來，之後再拆」= 上線後永遠不拆。安全洞跟著上線。DTO 是 Day 1 該做的事。

### 巢狀 DTO 忘記 `@Valid`

`CreateOrderRequest.items` 需要 `@Valid` 才會驗證每個 item。同樣 `Map<String, @Valid Foo>` 需要在型別參數上加。

### DTO 有 getter/setter 但沒 `public` 無參構造子

Jackson 需要 `public` 無參 constructor 才能反序列化。忘了 → 反序列化拋 `MismatchedInputException`。專案內每個 DTO 都有明確 `public XxxRequest() {}`。

### DTO 直接 return 給前端當 response

Request DTO 通常有 password 等敏感欄位。**不要**把 request DTO 當 response 回傳。用另一個 Response DTO。
