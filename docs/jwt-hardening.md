# JWT Hardening

專案內部知識庫，本專案 JWT 除了 `docs/jwt-basics.md` 講的 secret + signature，還多加了哪些保護，以及為什麼。

## 一句話

**「有正確簽名」不等於「這個 token 可以拿來做這件事」**。type / iss / aud 這三個 claim 是防「用對地方」的護欄；opaque error 則是不告訴攻擊者哪裡錯了。

## 加了什麼

### 1. `type` claim — 分開 access token 與 refresh token

每個 token 帶 `"type": "access"` 或 `"type": "refresh"`。

驗證方式：

```java
public DecodedJWT verify(String token, TokenType expected) {
    return JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim(CLAIM_TYPE, expected.claimValue())  // ← 型別必須對
            .build()
            .verify(token);
}
```

呼叫端明講預期的型別：

```java
// CustomAuthorizationFilter — 只吃 access token
jwtUtil.verify(token, JwtUtil.TokenType.ACCESS);

// UserController.getRefreshToken — 只吃 refresh token
jwtUtil.verify(refreshToken, JwtUtil.TokenType.REFRESH);
```

**防的攻擊**：

如果沒有 type 檢查，簽名 valid 的任何 token 都能驗過 authorization filter。攻擊者拿到自己的 **refresh token**（存在 client-side 或某次被截到），直接放進 `Authorization: Bearer` header → 沒 type 驗證會通過 → 但 refresh token 內容不含 `roles` claim → filter 建立的 `Authentication` 沒有 authorities → 之後 endpoint 授權會失敗。看起來自然拒絕？

**不見得**。某些 endpoint 只需要 `authenticated()` 而不檢查特定 role（例：登入使用者的個人 profile）。用 refresh token 混過去就能存取那類 endpoint。反向也一樣：把 access token 放去 `/api/refreshToken` 換新的 → 沒 type 檢查 → 直接發新 access token，這條路等於**永久不過期的 access token 生產機**。

加了 type check → 兩個方向都直接 400/403，攻擊面關閉。

### 2. `iss` (issuer) claim — 標記發行者

`iss=shopping-website`（可由 `JWT_ISSUER` env 覆蓋）。驗證時要求 `withIssuer(...)`。

**防的攻擊**：

- 攻擊者取得某個「共用同一支 secret」的兄弟系統（例如 dev + staging 共用 secret 的錯誤設定）發出的 token → 我方系統若不驗 `iss`，會照收
- 未來多環境部署，可用不同 issuer 讓 dev token 無法對 prod 使用

### 3. `aud` (audience) claim — 標記接收者

`aud=shopping-website-api`（可由 `JWT_AUDIENCE` env 覆蓋）。驗證時要求 `withAudience(...)`。

**防的攻擊**：

- 一組 team 有多個服務（API、web、admin console）用同一支 SSO secret → 用 `aud` 分割
- 攻擊者取得為別的服務發的 token（例：admin console 的），沒有 aud check → API 也收
- 加了 aud check → 只收「發給 API」的 token

### 4. Error message opacity — 不告訴攻擊者為什麼失敗

之前：

```java
Map<String, String> error = new HashMap<>();
error.put("error_message", exception.getMessage());  // ← 洩漏
return ResponseEntity.status(403).body(error);
```

`exception.getMessage()` 會回：
- `"The Token has expired on 2026-09-01T00:00:00Z"`
- `"The Claim 'iss' value doesn't match the required issuer."`
- `"The Claim 'type' value doesn't match the required one."`
- `"Signature verification failed."`

**這些訊息是給攻擊者的免費教學**：
- 「你的 token 過期了」→ 攻擊者知道有效 token 該長什麼樣
- 「issuer 不對」→ 攻擊者知道你有驗 issuer，開始猜正確 issuer 字串
- 「type 不對」→ 攻擊者知道有 type check，開始爆破 type 值
- 「Signature verification failed」→ 攻擊者知道 secret 錯了但 token 結構對；也可能開始用 timing attack 試 secret

改成 opaque：

```java
error.put("error_message", "invalid refresh token");
```

- 對合法用戶：一樣清楚（他們知道要重登入）
- 對攻擊者：只知道「不行」，不知道哪裡不行

**Log 端仍記錯誤細節**：`log.warn("jwt authorization failed", exception)` — 內部 debug 看得到，外部客戶端看不到。這叫 **defense in depth**：內外資訊不對稱。

## 現有實作對照

| 檔案 | 用途 |
|---|---|
| `JwtUtil.generateAccessToken` | 產 access token，帶 `type=access` + iss + aud |
| `JwtUtil.generateRefreshToken` | 產 refresh token，帶 `type=refresh` + iss + aud |
| `JwtUtil.regenerateAccessToken` | 換新的 access token，帶 `type=access` + iss + aud |
| `JwtUtil.verify(token, TokenType)` | 一併驗簽名/過期/iss/aud/type |
| `CustomAuthorizationFilter` | 呼叫 `verify(..., ACCESS)`，錯回 opaque 403 |
| `UserController.getRefreshToken` | 呼叫 `verify(..., REFRESH)`，錯回 opaque 403 |
| `application.properties` | `jwt.issuer` / `jwt.audience` 從 env 覆蓋 |

## 測試

`JwtUtilTest` 6 個 case 覆蓋核心不變量：
1. Access token verifies as ACCESS ✅
2. Refresh token verifies as REFRESH ✅
3. Access token 當作 REFRESH → `InvalidClaimException` ✅ ← **核心防禦**
4. Refresh token 當作 ACCESS → `InvalidClaimException` ✅ ← **核心防禦**
5. 別的 issuer 的 token → 拒絕 ✅
6. 別的 audience 的 token → 拒絕 ✅

## 尚未做的（未來 P2）

以下都是 P2 待辦，本 PR 沒動：

### Refresh token rotation
現在每次 `/refreshToken` 回傳**同一個** refresh token。應該產生**新**的 refresh token 並記錄舊的已用過（refresh token reuse detection）。這需要 refresh token 有 server-side state（DB 表存 jti + issued/used timestamp）。

### 撤銷（revocation）
JWT 本質 stateless → 沒法個別撤銷。要撤銷（例：使用者按「登出所有裝置」）就要：
- Token blacklist（存黑名單的 jti/user，每次驗證查一下）→ 破壞 stateless 但簡單
- Short-lived access token + rotation → 現在 access 10 分鐘，refresh 24 小時，若換 secret，短時間內全撤銷
- 換 `JWT_SECRET`（**核選項**）→ 全體立即失效，強制重登

### JTI (JWT ID)
每個 token 加唯一 `jti`。目前沒加，因為沒有 blacklist / audit log 需要對照。加了 jti 為以後 revoke/audit 鋪路。

## 相關

- `docs/jwt-basics.md`：JWT 三段結構、payload 為什麼可見、signature 為什麼可傳
- `AGENTS.md` 第 40 行「Access token 與 refresh token 必須維持不同用途，驗證流程不得允許互相替代」
- `ARCHITECTURE_TODO.md` P2「JWT 與 Spring Security」

## 常見錯誤

### 「有簽章就通過」
最常見的 JWT 用法錯誤：只驗簽名，不驗 claims。等於只確認「這個 token 是我們家發的」，不確認「這個 token 該做這件事」。type / iss / aud 是必要 claim check。

### 「用 exception.getMessage() 當錯誤訊息比較清楚」
對合法用戶不會更清楚（他們就是重登入），對攻擊者變成教學。**外部訊息 opaque，內部 log 詳細**。

### Type 用字串比對而不是 claim 檢查
```java
if ("access".equals(decoded.getClaim("type").asString())) { ... }  // ← 差
```
應該把 type check 放進 `.withClaim(...)` 讓 verifier 整體 fail-atomic，而不是驗完再手動比對。前者失敗抛 exception 傳到 filter chain，後者容易被忘寫、或 `if` 條件寫反。

### 同一支 secret 給多個服務用
共用 secret = 任何一個服務外洩其他全掛。**每個 audience 應該有獨立 secret**。這個專案目前只有一個 audience，OK；未來拆服務時每個服務自己一支。
