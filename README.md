## 購物網站side project後端
前端網站deploy link: https://reurl.cc/eOKv3M \
前端github: https://github.com/albertkingdom/react-shopping-website-for-spring-boot 

*後端部署在AWS平台*

## 使用者帳密
|  | 前台user| 後台admin |
| -------- | -------- | -------- |
| id | test6@gmail.com | admin@gmail.com |
| password | test666  | myadmin   |


## 功能
### 前台
- 註冊帳號
- 登入
- 購物車、下訂單 \
### 後台
- 上架、下架商品、修改商品資訊
- 檢視訂單、刪除訂單

### 權限管理
只有admin帳號可以進入後台

## 使用技術
- java spring boot框架
- 前後端使用RESTful API溝通
- MySQL資料庫存取
- spring security進行權限管理，使用jwt進行身份驗證(Authorization)
