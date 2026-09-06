# 購物網站 side project 前端

Deployed on AWS: https://reurl.cc/85DVxy

## 使用者帳密

|          | 前台 user       | 後台 admin      |
| -------- | --------------- | --------------- |
| id       | test6@gmail.com | admin@gmail.com |
| password | test666         | myadmin         |

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
