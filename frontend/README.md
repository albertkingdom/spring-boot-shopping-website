# 購物網站 side project 前端

Deployed on AWS: https://reurl.cc/85DVxy

## 本機測試帳號

本 repository 不再保證固定的前台或後台測試帳號；舊版文件中的
`test6@gmail.com`、`admin@gmail.com` 與對應密碼已移除，避免把過期或可預測的
credential 當成目前環境的登入方式。

請依 repository 根目錄的 `.env.example`、目前使用的 dev fixture 或團隊提供的
本機測試資料建立／取得 disposable 帳號。密碼與 password hash 不得寫入 README、
版本庫或其他可提交的設定檔。

## 技術

- React
- Bootstrap

## 本機執行

前端已整合至本 repository。完整的本機環境應從 repository 根目錄啟動：

```bash
docker compose up --build
```

此指令會啟動 MySQL、Spring Boot 與 Nginx 前端；Nginx 會將 `/api/` 請求代理至 Compose service `spring`。

若只開發前端，可在本目錄直接執行 `npm start`，並先在 repository 根目錄啟動後端服務。`docker-compose-dev.yml` 和 `Dockerfile.dev` 仍可用於需要容器化 React 開發伺服器的情況。
