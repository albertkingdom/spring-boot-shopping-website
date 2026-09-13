---
name: project-sdd-workflow
description: Use for new features or major user-visible, API, schema, authentication/authorization, or frontend contract changes in this repository. Guide the Spectra-inspired Discuss → Propose → Apply → Ingest → Verify → Review → Archive lifecycle using the project's docs/specs feature Spec. Do not use for simple bug fixes, refactors, or documentation-only edits unless requirements change.
metadata:
  short-description: Run the project feature lifecycle from discuss to archive
---

# Project SDD Workflow

Use this skill together with the repository root `AGENTS.md`. It is the detailed operating procedure; `AGENTS.md` remains authoritative for architecture, security, testing, Git, authorization, and file-location rules.

The project keeps one feature contract at `docs/specs/<feature-name>.md`. Do not introduce an `openspec/`, `docs/spectra/`, or separate change directory unless the repository rules are deliberately changed first.

## Before any phase

- Confirm the repository and worktree with `pwd`, `git branch --show-current`, and `git status --short --branch`.
- Read the relevant `AGENTS.md`, existing specs, architecture notes, API documentation, and affected code before making assumptions.
- Preserve user changes. Do not reset, checkout over, or delete unrelated work.
- Planning or specification work does not authorize implementation, commit, push, merge, deployment, or secret rotation.

## Discuss

Use this phase to turn an idea into a bounded problem statement. Do not modify feature code.

- Clarify the problem, users, goals, scope, non-goals, constraints, risks, and unresolved decisions.
- Inspect the relevant existing behavior before comparing design options.
- Record decisions and reasons when they affect API shape, data ownership, security, compatibility, or user flow.
- Stop and ask for clarification when the request cannot yet be described as observable behavior.

## Propose

Use `$feature-spec` when creating or updating the feature contract. If it is unavailable in the current session, use this skill's template and the minimum requirements in `AGENTS.md` as a local fallback; do not skip the planning and acceptance gates.

- Put the complete contract in `docs/specs/<feature-name>.md`.
- Use [references/feature-spec-template.md](references/feature-spec-template.md) as the default section structure. Keep the single-file rule; do not split the feature into separate proposal, design, or task files unless the repository rules are deliberately changed first.
- Include background, goals, scope, non-goals, implementation guidance, scenarios, acceptance conditions, API impact, data impact, business/security rules, error boundaries, test strategy, and an ordered todo list.
- Write behavior and externally observable outcomes first. Keep detailed class/method steps in implementation guidance or todo items.
- Give every acceptance condition an `AC-*` label and every behavior scenario an `SCN-*` label. For every important scenario, state preconditions, trigger/input, expected result, authorization behavior, error behavior, and its test or manual-validation method.
- Inspect existing specs, API contracts, data models, and frontend flows for contradictions or omissions before starting implementation.

## Pre-implementation review

Do not write feature code until all of the following are true:

- The spec answers why the change is needed, what behavior must exist, and what counts as complete.
- Every `AC-*` acceptance condition has at least one corresponding `SCN-*` scenario, todo, and automated test or explicit manual validation.
- API, migration, authentication, authorization, money, order, external-service, and frontend-contract risks have documented compatibility and failure behavior.
- The todo list includes migrations, backend, frontend when affected, tests, documentation, environment setup, and final validation.
- The selected branch/worktree is correct and is not `master`.

If the repository has an existing implementation or spec that conflicts with the proposal, resolve the conflict in the spec before coding.

## Apply

- Implement todo items in dependency order.
- After each independently verifiable item, run the narrowest relevant test and keep the todo state accurate.
- If a task is unclear, its premise is false, or a missing requirement appears, stop expanding the code change and return to Propose/Ingest.
- Check a todo item only after implementation and its required evidence are complete; compilation alone is not evidence of acceptance.
- When TDD is appropriate, use Red → Green → Refactor. Every testable scenario needs a corresponding test; purely visual details may use documented manual validation.

## Ingest

Use this phase whenever requirements change during implementation or review.

- Pause code changes first.
- Update the same spec and its todo list for changed goals, scope, scenarios, API, data, authorization, or UI behavior.
- Reconcile the spec, implementation guidance, acceptance conditions, test strategy, and todo dependencies before resuming Apply.
- If the new request is outside the original feature boundary, create a new spec instead of silently expanding the current one.

## Verify

Before declaring the feature complete:

- Compare the implementation and diff against every spec section and acceptance condition.
- Use the template's Verification and Acceptance sections to connect every `AC-*` condition and `SCN-*` scenario to an automated test or manual validation and record the evidence.
- Run affected tests and the required project checks, including `./mvnw test` and `./mvnw verify` when the repository rules require them.
- Run frontend tests/builds, integration tests, migration checks, staging checks, or external-service failure tests when the feature requires them.
- For manual acceptance, record prerequisites, test data, steps, expected results, actual results, environment, date, and any known limitations in the spec or its todo.
- Treat a failed acceptance condition as either an implementation defect or a documented requirement change; never edit the condition merely to match the current behavior.
- Review security, sensitive logging, API compatibility, migration safety, and unrelated diff scope.

## Review (report-only)

Use this phase when the user requests a review or when the change's risk requires an independent quality check.

- Read the final spec, design guidance, todo state, implementation diff, tests, and acceptance evidence.
- Report findings with severity, file path, line number when available, rationale, and a concrete suggested fix.
- Do not modify, stage, commit, or otherwise mutate files during this phase.
- If there are no actionable findings, state that clearly and list residual risks or unverified areas.

## Optional quality checks

- `analyze`: use when proposal, requirements, design, or tasks may contradict or omit a dependency.
- `audit`: use when the change affects authentication, authorization, secrets, sensitive data, payments, or other security boundaries.
- `drift`: use when a change has been paused, the codebase has moved, or the implementation may no longer match the planned files or assumptions.

Suggested acceptance record:

```markdown
## Acceptance Record

- Status: passed | blocked
- Date:
- Environment:
- Automated checks:
  - `./mvnw test` — passed/failed
  - `./mvnw verify` — passed/failed/not required
- Manual scenarios:
  - Scenario:
    - Expected:
    - Actual:
    - Result: passed/failed
- Known limitations:
```

## Archive

- Archive only after all required `AC-*` acceptance conditions pass and all necessary todo items are checked with evidence.
- Update the spec to describe final behavior, final design decisions, validation results, and known limitations.
- Keep the completed spec and todo. Move old specs to `docs/specs/archive/` only when they no longer need to be in the daily active set; never delete their history.
- Do not commit, push, merge, tag, or deploy unless the user explicitly authorizes that action.
