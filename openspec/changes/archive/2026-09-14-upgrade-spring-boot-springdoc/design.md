## Context

目前專案以 Java 21、Spring Boot `3.3.4`、Spring Security、Spring Data JPA、MySQL、Flyway 與 springdoc OpenAPI `2.6.0` 組成單體應用。現有 security configuration 使用 `SecurityFilterChain`，沒有預期中的 `WebSecurityConfigurerAdapter` migration；應用也沒有 Actuator、`@ConfigurationProperties` 或 `RestTemplate` 等已知高風險整合點。

這是 framework/dependency migration，不改變 `openspec/specs/` 所描述的產品 capability。實作仍需確認 Boot 3.5 管理的 transitive dependency（尤其 Spring Framework、Spring Security、Spring Data、Hibernate、Flyway、MySQL driver）不破壞目前的啟動、資料驗證與測試流程。

## Goals / Non-Goals

**Goals:**

- 將 parent 與 springdoc 升級到提案指定版本，讓 Maven 使用一致的 Spring Boot dependency management。
- 以最小必要 source change 修正 framework 版本差異，並保留現有 API、security、database 與 deployment contract。
- 驗證 application context、Flyway/JPA startup、JWT security rules、OpenAPI endpoints 與既有測試。
- 將 migration 的版本、相容性注意事項與實際驗證證據留在 change artifacts。

**Non-Goals:**

- 不升級到 Spring Boot 4 或 Jakarta API major generation。
- 不重新設計 controller/service/repository 架構，不修改業務規則、API payload、資料表或 seed data。
- 不因 deprecation warning 而進行與本次 migration 無關的大規模重構。
- 不修改 frontend，除非驗證明確發現既有 API contract 受到非預期影響。

## Decisions

### 1. 使用 Spring Boot `3.5.16` 作為 dependency management source

直接更新 `spring-boot-starter-parent`，讓 Spring Boot BOM 統一管理 transitive dependency，避免逐項手動鎖定 Spring、Hibernate、Flyway、driver 或測試套件版本。這比只更新個別 starter 更能揭露並處理同一 release train 的相容性問題。

替代方案是先停在 Boot `3.4.x` 作為中間版本；該方案可降低單次差異，但會留下額外 migration checkpoint，且本次目標仍需再次升級至 3.5，因此不採用。若驗證發現第三方整合無法直接通過，才以明確的相容性修正或回退版本處理，不預先引入中間版本。

### 2. 使用 springdoc `2.9.1`

將 `springdoc-openapi-starter-webmvc-ui` 與 Boot 3.5 一起升級至 `2.9.1`，保持 springdoc 2.x 與 Spring Boot 3 的 generation 對齊，並納入該版本線的修正。保留現有 OpenAPI starter 類型與 endpoint whitelist，不切換到 WebFlux starter 或手動 swagger-ui 整合。

替代方案是只升級 Boot、保留 `2.6.0`；這會讓 springdoc 留在 Boot 3.3 對應的版本線，且可能在 Boot 3.5 的 Spring Framework method/API 變動下於 startup 失敗，因此不採用。也不升級到 springdoc 3.x，因為那是 Boot 4 對應的 major generation。

### 3. 先處理明確的 test API deprecation，再處理其他編譯錯誤

若現有測試使用的 `@MockBean`/`@SpyBean` 在新的 Spring Framework baseline 只產生 deprecation，且對應的 Mockito bean-override annotation 可直接替代，則將其改為新的測試 API，以避免 migration 後留下可預見的 deprecated test surface。其他 source/config 只在編譯、啟動或測試證據顯示必要時修改。

替代方案是保留所有 deprecated annotation；雖可能暫時通過編譯，但會把已知 migration debt 留給下一次 major upgrade，因此不採用。

### 4. 以分層驗證確認 migration 邊界

先執行受影響的 unit/controller/security tests，再使用 repository 提供的 `docker-compose.test.yml` 執行 `./mvnw verify`，因為這個專案的完整 Spring context、MySQL、Flyway 與 container 驗證不能由未注入 datasource 的裸 Maven container 取代。OpenAPI endpoint 與 security whitelist 以測試或可重現的 HTTP 驗證確認。

### 5. 回退以版本 revert 為界

本 change 不執行 commit、push 或 deployment。若 staging/CI 驗證不通過，回退策略是恢復 `pom.xml` 的 parent/springdoc 版本及本次必要的相容性 source changes，再重新執行相同測試；不使用 destructive Git command，也不修改 production schema。

## Risks / Trade-offs

- [Transitive dependency behavior changes] → 以完整 `verify`、MySQL、Flyway、JPA startup 與既有 controller/security tests 驗證；只固定有證據需要的第三方版本。
- [Spring Framework 6.2 test bean override semantics differ] → 僅將現有 Mockito mock annotation 做等價替換，並以完整測試確認 context wiring 與 mock interaction。
- [OpenAPI generated schema or Swagger UI output changes] → 驗證 `/v3/api-docs`、`/swagger-ui/index.html` 與既有 security whitelist；若發現公開 contract 變動，停止擴大範圍並記錄差異。
- [Hibernate/Flyway behavior changes at startup] → 使用 test compose 執行 migration、schema validation 與整合測試；本 change 不修改正式 schema。
- [Future Boot 3.5 deprecations remain] → 將 warning 分類，只處理會阻擋建置、啟動、測試或本次相容性的項目，避免把 migration 變成無界線重構。
