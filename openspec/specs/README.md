# Living Specifications

這裡是目前系統行為的唯一規格來源。每個 capability 以一份 `spec.md` 描述可觀察的 Requirements 與 Scenarios；尚未完成的變更必須放在 `openspec/changes/`，不能直接改寫這裡來掩蓋未完成工作。

## Capability map

| Capability | 內容 | 舊文件來源 |
|---|---|---|
| [access-control](access-control/spec.md) | 登入、角色、seller 授權與 owner scope | `docs/specs/current-system.md`、`multi-seller-access.md` |
| [catalog](catalog/spec.md) | 公開商品、平台商品、seller 商品與圖片 | `current-system.md`、`multi-seller-access.md` |
| [orders](orders/spec.md) | 下單、價格計算、snapshot 與 seller 訂單 scope | `current-system.md`、`multi-seller-access.md` |
| [backoffice-ui](backoffice-ui/spec.md) | admin／seller workspace、導覽、狀態、主題與 RWD | `multi-seller-access.md` |
| [deployment-environments](deployment-environments/spec.md) | dev、staging、production 隔離與部署 promotion | `environment-separation.md` |

## 維護規則

1. 新 feature 先建立 `openspec/changes/<name>/`，用 delta spec 表達新增、修改、移除或重新命名的 Requirement。
2. 實作期間只更新 active change 的 artifacts；若需求改變，先回到 change 修改 proposal、spec、design 或 tasks。
3. 驗收通過後，將已實作 delta sync 回對應 capability，再 archive 完整 change。
4. 若現有行為沒有對應 capability，先新增 capability spec，再建立 feature change。
