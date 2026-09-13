# Feature Spec Template

Use this template for `docs/specs/<feature-name>.md`. It adapts the Spectra-style proposal → spec → design → tasks → verification flow to this repository's single-file Spec rule.

Replace the placeholders and remove guidance comments before considering the Spec ready for implementation. Do not keep sections that are genuinely not applicable without stating `無影響` or `不適用` and why.

```markdown
# <Feature name>

## Proposal

### Background and Goal

<!-- What problem are we solving? Who is affected? What outcome should exist? -->

### Scope

包含：

- ...

不包含：

- ...

### Assumptions and Decisions

- 假設：...
- 決策：...
- 重要取捨：...

## Requirements and Scenarios

### REQ-001: <Behavior requirement>

<!-- Describe externally observable behavior, not classes, methods, or implementation steps. -->

### AC-001: <Observable acceptance condition>

- Requirement: REQ-001
- Must be true: ...
- Validation scenarios: SCN-001, SCN-002

#### SCN-001: <Happy path or primary scenario>

- Given <precondition>
- When <trigger or input>
- Then <observable result>

#### SCN-002: <Error, permission, or boundary scenario>

- Given <precondition>
- When <trigger or input>
- Then <observable result>

## Design and Implementation Guidance

### Architecture and Request Flow

- ...

### API Impact

- Endpoint and method: ...
- Request: ...
- Response: ...
- Status codes: ...
- Authorization: ...
- Compatibility: ... or `無影響`

### Data Impact

- Model/entity changes: ...
- Migration: ... or `無影響`
- Constraints/indexes/seed data: ...
- Existing data handling: ...

### Business and Security Rules

- ...

### Errors and Boundaries

- Invalid input: ...
- Missing resource: ...
- Conflict: ...
- Unauthorized/forbidden: ...
- External service failure: ...

## Test Strategy

| Acceptance | Scenario | Test or validation method | Expected evidence |
|---|---|---|---|
| AC-001 | SCN-001 | Service/controller/integration/frontend test or manual check | ... |
| AC-001 | SCN-002 | ... | ... |

## Implementation Todo

Tasks must be ordered by dependency. Each task must reference the requirement, acceptance condition, or scenario it advances.

- [ ] T-001 <Task description> — covers REQ-001 / AC-001 / SCN-001
- [ ] T-002 <Task description> — after T-001; covers AC-001 / SCN-002
- [ ] T-003 Add or update tests — covers AC-001 / SCN-001, SCN-002
- [ ] T-004 Update affected documents/configuration and run final validation; update `ARCHITECTURE_TODO.md` only if a corresponding architecture item is completed or changed

## Verification and Acceptance

| Acceptance | Scenario | Automated test / manual procedure | Result | Evidence |
|---|---|---|---|---|
| AC-001 | SCN-001 | ... | pending | ... |
| AC-001 | SCN-002 | ... | pending | ... |

## Acceptance Record

- Status: pending | passed | blocked
- Date: ...
- Environment: ...
- Automated checks:
  - `./mvnw test` — passed/failed/not run
  - `./mvnw verify` — passed/failed/not required
- Manual validation:
  - Preconditions/test data: ...
  - Steps and expected results: ...
  - Actual results: ...
- Known limitations: ... or `無`
```

## Usage Rules

- `Discuss` produces decisions and open questions; it does not require a separate document.
- `Propose` fills the Proposal, Requirements and Scenarios, Design, Test Strategy, and Todo sections before implementation.
- `Apply` updates only the Todo and implementation-related evidence as work progresses.
- `Ingest` updates the affected requirements, scenarios, design, tests, and tasks before code continues.
- `Verify` fills Verification and Acceptance and must link every `AC-*` condition and `SCN-*` scenario to a test or explicit manual validation.
- `Review` is report-only: it checks the implementation against the Spec and reports findings without modifying files. `analyze`, `audit`, and `drift` are optional quality checks selected by risk.
- `Archive` keeps the final behavior, decisions, evidence, and known limitations; it does not delete the Spec.
