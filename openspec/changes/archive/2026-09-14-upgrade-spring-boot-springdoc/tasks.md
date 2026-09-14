## 1. Dependency and compatibility update

- [x] 1.1 Update `spring-boot-starter-parent` from `3.3.4` to `3.5.16` and `springdoc-openapi-starter-webmvc-ui` from `2.6.0` to `2.9.1`; verify Maven resolves the intended dependency graph with `./mvnw dependency:tree`.
- [x] 1.2 Run a repository-wide compatibility audit for Boot 3.4/3.5 migration points and update only necessary source/configuration, including replacing equivalent deprecated Mockito test annotations; verify the project has no remaining compile-time use of the migrated test API.
- [x] 1.3 Confirm security configuration still exposes only the intended public OpenAPI paths and preserves JWT-protected business endpoints; verify with the existing security/controller tests and an OpenAPI endpoint test or reproducible HTTP check.

## 2. Verification and documentation

- [x] 2.1 Run affected unit, controller, and security tests with `./mvnw test`; record command, environment, result, and any known limitation in `verification.md`.
- [x] 2.2 Run the full MySQL/Flyway/Spring-context verification with `MAVEN_GOAL=verify docker compose -p shopping-test -f docker-compose.test.yml up --abort-on-container-exit --exit-code-from test`, then clean up with `docker compose -p shopping-test -f docker-compose.test.yml down --volumes --remove-orphans`; record actual evidence in `verification.md`.
- [x] 2.3 Review the final diff for unintended API, schema, frontend, secret, or build-output changes and update migration documentation/change artifacts with the final compatibility notes; verify `git diff` is limited to this migration plus the pre-existing `AGENTS.md` modification.
- [x] 2.4 Run the project code review skill, record findings and remediation status in `code-review.md`, and verify no P0/P1 finding remains before completing the change.
