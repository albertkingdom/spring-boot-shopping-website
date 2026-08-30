# JWT 基礎筆記

專案內部知識庫，白話說明 JWT 的運作，避免每次 review 都要重推一遍。

## JWT 是什麼

一張後端發給使用者的「通行證」。使用者登入成功後拿到它，之後每次呼叫 API 都出示，後端就相信「這個人已登入，身分是 XXX」。

## 結構：三段用 `.` 分開

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGJlcnRAZ21haWwuY29tIiwicm9sZXMiOlsiUk9MRV9BRE1JTiJdLCJleHAiOjE3MjA1MDAwMDB9.abc123XYZsignature
```

| 段 | 內容 | 誰能看 |
|---|---|---|
| 1. Header | 用什麼演算法（例：HS256） | 誰都能看 |
| 2. Payload | 使用者資料（sub、roles、exp…） | **誰都能看** |
| 3. Signature | 用 `JWT_SECRET` 蓋的章 | 誰都能看，但只有伺服器算得出來 |

## Payload 是明文，不是加密

Payload 只是 **Base64 編碼**，不是加密。任何拿到 token 的人一秒就能解出來：

```bash
echo 'eyJzdWIiOiJhbGJlcnRAZ21haWwuY29tIiwicm9sZXMiOlsiUk9MRV9BRE1JTiJdLCJleHAiOjE3MjA1MDAwMDB9' | base64 -d
```

輸出：

```json
{
  "sub": "albert@gmail.com",
  "roles": ["ROLE_ADMIN"],
  "exp": 1720500000
}
```

或貼到 <https://jwt.io> 直接看。

使用者只要按 F12 → DevTools → Application → Local Storage 就能拿到自己的 token，複製貼進 jwt.io 就看得到內容。**這是設計，不是漏洞。**

**推論**：不要把敏感資訊塞進 payload。信用卡、密碼、私人個資都不行。目前專案 payload 只放 `sub`（email）、`roles`、`exp`，都是使用者自己身分的東西，OK。

## JWT_SECRET 的角色：鋼印

發 token 時：
1. 後端組 payload
2. 用 `JWT_SECRET` 對 payload 算 HMAC-SHA256，得到 signature
3. 把 `header.payload.signature` 完整回傳給前端

驗 token 時：
1. 前端把完整 token 放在 `Authorization: Bearer ...` 送回來
2. 後端取 payload，用 `JWT_SECRET` **重算一次** signature
3. 拿重算結果對照前端送來的那段 signature
4. 一致 → 通行證是真的、內容沒被改；不一致 → 拒絕

## Signature 為什麼要傳給前端

因為驗證是「後端拿到 token 重算一次去對照」，**沒有對照物就沒東西可比**。

JWT 的整個賣點是 **stateless**：後端不記錄任何 session，只要有 `JWT_SECRET` + 收到的完整 token 就能驗。這就要求 signature 必須跟著 token 一起在前後端之間傳。

替代方案是 session，但那就要維護 session store（Redis / DB），多台伺服器要共用，不是 JWT 的走向。

## 為什麼「使用者看得到」還安全

假設壞人拿自己 `ROLE_USER` 的 token，想升級成 `ROLE_ADMIN`：

1. 解出 payload → `{roles: ["ROLE_USER"]}`
2. 改成 `{roles: ["ROLE_ADMIN"]}` → 重新 Base64 → 新的第二段
3. 但第三段簽名還是舊的 → 送給後端
4. 後端拿新 payload + `JWT_SECRET` 重算簽名 → 對不上壞人送來的第三段 → 拒絕

要連簽名一起重算？**沒有 `JWT_SECRET` 算不出來**。

類比：下載檔案時網站給你 SHA256 hash，hash 本身公開沒差，因為改不出對得上的假 hash。**Signature 不是機密，是校驗值。**

## JWT_SECRET 的實務規則

- **要保密**：只有伺服器能有；外洩 → 任何人能冒充任何使用者，包含 admin，系統失守
- **要夠長**：至少 32 bytes（HMAC-SHA256 的安全門檻）；`"secret"`、專案名、字典字都會被幾秒破解
- **每個環境不同**：local / staging / prod 各自產生獨立值，一邊外洩不會連累其他
- **不要塞進 Git**：用環境變數注入，`.env` 進 `.gitignore`，repo 只放 placeholder
- **輪替方式**：換一個新值重啟後端 → 所有舊 token 立刻失效 → 強制所有人重新登入（這是撤銷已外洩 token 最快的手段）

產生方式：

```bash
openssl rand -base64 48
# 或
openssl rand -hex 32
```

## 專案裡的實作對照

- `JwtUtil` 建構子讀 `${jwt.secret}`，缺值或 < 32 bytes 直接拋 `IllegalStateException`（fail-fast，避免弱 secret 悄悄上 prod）
- `application.properties` 有 `jwt.secret=${JWT_SECRET}`
- `JWT_SECRET` 從環境變數注入：本機 `.env`、`docker-compose.yml`、CI workflow env、prod secrets manager
- `.env.example` 只放 placeholder，真值絕不進 Git

## 未來要補的（見 `ARCHITECTURE_TODO.md` P2 JWT 段）

- Access token 與 refresh token 加 token type claim，禁止互相替代
- 加 issuer / audience 驗證
- Refresh token rotation 與撤銷策略
- JWT 驗證失敗訊息不直接回傳給客戶端
