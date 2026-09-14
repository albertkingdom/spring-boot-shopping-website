# Proposal: Upgrade Spring Boot and springdoc

## Why

The project is currently based on Spring Boot `3.3.4` and springdoc OpenAPI `2.6.0`. The application already runs on Java 21, so it can move to the maintained Spring Boot 3.5 line and a matching springdoc release to receive framework, dependency, and security fixes while staying on the Boot 3 generation.

## What Changes

- Upgrade the Spring Boot parent from `3.3.4` to `3.5.16`.
- Upgrade `springdoc-openapi-starter-webmvc-ui` from `2.6.0` to `2.9.1`.
- Update test integrations affected by the Spring Framework 6.2 test bean-override API, including replacing deprecated Mockito test annotations where compilation and behavior remain equivalent.
- Audit application configuration, security configuration, persistence startup, Flyway, and OpenAPI endpoints for Boot 3.4/3.5 compatibility.
- Preserve the existing business APIs, authentication and authorization behavior, database schema contract, environment variables, and deployment workflow.

The dependency upgrade is a compatibility boundary: deprecated framework APIs or configuration behavior may require source changes, and the generated OpenAPI document or Swagger UI dependency versions may change. No intentional business-level API or database-schema change is included.

## Capabilities

### New Capabilities

None. This is a framework and dependency migration with no new product capability.

### Modified Capabilities

None at the specification level. Existing capability behavior must remain unchanged; this change explicitly opts out of a new delta spec because it does not alter catalog, order, access-control, or deployment behavior by intent.

## Impact

- **Build:** `pom.xml` parent and springdoc dependency versions change; transitive Spring, Spring Security, Spring Data, Hibernate, Flyway, MySQL driver, and test-container versions will be managed by the new Boot BOM.
- **Source and tests:** compile errors, deprecations, or test-context changes introduced by the new framework baseline will be fixed only where needed for compatibility.
- **Runtime verification:** application startup, JPA schema validation, Flyway migrations, security rules, and Swagger/OpenAPI endpoints require verification.
- **Frontend:** no frontend change is expected because no API contract change is intended; it will be revisited only if generated API documentation exposes an actual contract change.
- **Operations:** no new environment variable, migration script, image, or deployment procedure is planned.
