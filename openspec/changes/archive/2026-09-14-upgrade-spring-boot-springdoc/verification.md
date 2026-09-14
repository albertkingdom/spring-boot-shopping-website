# Verification

日期：2026-09-14  
環境：Docker Desktop 4.88.1、Java 21.0.12（`eclipse-temurin:21-jdk`）、MySQL 8.0.40、Maven Wrapper 3.9.16、macOS host

## Dependency resolution

- 命令：`./mvnw dependency:tree`
- 結果：PASS，Maven dependency tree 成功解析。
- 重要 resolved versions：Spring Boot `3.5.16`、Spring Framework `6.2.19`、Spring Security `6.5.11`、Hibernate `6.6.53.Final`、Flyway `11.7.2`、springdoc `2.9.1`、Swagger UI `5.32.14`、MySQL Connector/J `9.7.0`。

## Compatibility audit

- 命令：`rg -n 'WebSecurityConfigurerAdapter|@MockBean|@SpyBean|RestTemplate|WebClient|@ConfigurationProperties|Actuator|server\\.shutdown|DynamicPropertyRegistry|FilterRegistrationBean' src/main src/test`
- 結果：PASS，未發現本次 Boot 3.4/3.5 audit pattern；測試中的 Mockito mock 已改用 `@MockitoBean`。
- Security review：`SecurityConfig` 保留 `/v3/api-docs/**`、`/swagger-ui/**` 與 `/swagger-ui.html` whitelist，並保留 JWT business endpoint authorization rules。

## Tests

- 命令：`MAVEN_GOAL=test docker compose -p shopping-test -f docker-compose.test.yml up --abort-on-container-exit --exit-code-from test`
- 測試資料：全新 MySQL 8.0 test databases；由 Flyway 執行 6 個 migrations，並使用 compose 注入測試 datasource、Cloudinary placeholder 與 JWT placeholder。
- 結果：PASS，`Tests run: 87, Failures: 0, Errors: 0, Skipped: 0`。
- 包含驗證：Spring context startup、JPA schema validation、Flyway migration、OpenAPI `/v3/api-docs` 回 `200`、未登入 `/api/user/all` 回 `401`、既有 controller/security/service tests。

- 命令：`MAVEN_GOAL=verify docker compose -p shopping-test -f docker-compose.test.yml up --abort-on-container-exit --exit-code-from test`
- 結果：PASS，`Tests run: 87, Failures: 0, Errors: 0, Skipped: 0`，並成功完成 jar packaging/repackage。
- 清理：`docker compose -p shopping-test -f docker-compose.test.yml down --volumes --remove-orphans`，PASS；暫時 containers、network 與 MySQL volumes 已移除。

## Host limitation

- 直接在 host 執行 `./mvnw test` 未通過，原因是 host 預設 `java -version` 為 Java 8，而 Spring Boot 3.5 class files 需要 Java 17+；錯誤為 class file version `61.0` vs `52.0`。依 repository rule 改用 Java 21 compose 後，`test` 與 `verify` 均通過。

## Known non-blocking warnings

- Mockito inline mock maker 在目前 JDK 會顯示 dynamic agent warning；不影響測試結果。
- Spring Boot 顯示既有 `spring.jpa.open-in-view` warning；本次 migration 未改變此既有設定。
- MySQL test image 顯示既有 host-cache/timezone/certificate warnings；不影響 migration 或測試結果。
