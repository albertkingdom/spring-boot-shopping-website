---
name: project-code-review
description: Review a completed OpenSpec change against this project's architecture, security, tests, API/data contracts, and acceptance evidence; record findings in code-review.md and track their remediation. Use after implementation and before sync, archive, or PR.
metadata:
  short-description: Review completed changes and track fixes
---

# Project Code Review

Use this skill after a feature's implementation and affected tests are complete. The review is an independent quality gate for this repository; it is not a replacement for `openspec-verify-change`, automated tests, or staging acceptance.

## Inputs to read

1. Confirm the active change name and read its `proposal.md`, `specs/`, `design.md`, and `tasks.md`.
2. Read `AGENTS.md`, the affected living specs under `openspec/specs/`, and relevant project documentation.
3. Check `git status` before inspecting the diff. Review the complete change diff from its base branch, including uncommitted changes, without discarding user work.
4. Inspect existing tests and the reported `verification.md` evidence. If evidence is missing, record it as a finding rather than assuming the check passed.

## Review dimensions

Review only the change's scope and its direct dependencies:

- **Architecture:** Controller → Service → Repository → Database; constructor injection; DTO/entity separation; transaction boundaries; no business logic in controllers.
- **Security and authorization:** explicit `ROLE_ADMIN`／`ROLE_SELLER` checks, authenticated ownership scope, `401`／`403`／`404`／`409` behavior, no secrets or sensitive logs, and no frontend-only security boundary.
- **API and data:** validation, status codes, backward compatibility, money types, migrations, seed/profile boundaries, N+1 risk, and historical order snapshots.
- **Cloudinary/files:** MIME and size validation, safe temporary files, cleanup, failure compensation, and preservation of existing images when no new image is uploaded.
- **Tests and acceptance:** Service tests for business logic, Controller/security tests for endpoints, integration tests for MySQL/Flyway behavior, frontend or browser checks when applicable, and traceability to Requirement／Scenario.
- **Frontend and responsive behavior:** API contract compatibility, role-scoped navigation, loading/empty/error/confirmation states, theme behavior, narrow-screen usability, and no false success state.
- **Maintainability and scope:** naming, unused code, documentation, environment instructions, and unrelated changes.

## Record the review

Create or update this independent review record:

```text
openspec/changes/<change-name>/code-review.md
```

Use the template at `references/review-template.md`. Each finding must have:

- a stable id such as `CR-001`;
- severity (`P0` blocker, `P1` high, `P2` medium, or `P3` low);
- file and line evidence where applicable;
- the concrete problem and required fix;
- whether it requires replanning;
- fix status and verification evidence.

Do not put detailed findings in `tasks.md` or `verification.md`. `tasks.md` tracks implementation work; `verification.md` records test and acceptance evidence; `code-review.md` records review findings and their remediation.

## Remediation and replanning

When a finding is an implementation defect but the plan remains correct:

1. Fix the code and tests.
2. Run the smallest affected checks, then the required project checks.
3. Add the result and changed file references to the finding.
4. Check the finding only after the fix is verified.

When review shows that the original plan is wrong:

1. Pause the code fix; do not silently rewrite the requirement while changing code.
2. Mark the finding as `Replanning required` in `code-review.md`.
3. Update the affected active artifacts in order: `proposal.md` for intent/scope, delta `specs/` for behavior/API/data/auth/UI contract, `design.md` for implementation decisions, and `tasks.md` for the revised work sequence.
4. If the new request is a different intent rather than a refinement, start a new OpenSpec change instead of expanding the current one.
5. Record the planning revision, rationale, and affected artifact paths in `code-review.md`.
6. Re-run the pre-implementation review for the revised plan before applying the fix; then implement, test, and re-review the affected findings.

Never mark a finding resolved solely because the code was edited. A checked finding requires test, manual, or review evidence. A change is not ready for sync, archive, or PR while a P0/P1 finding remains open. P2/P3 findings may remain only with an explicit rationale and reviewer/user acceptance recorded in the review file.

## Final result

Set the review status to one of:

- `changes-requested`: open P0/P1 findings remain;
- `approved-with-notes`: no P0/P1 findings remain and accepted P2/P3 notes are recorded;
- `approved`: no open findings remain.

The final review must state the scope, base revision, checks performed, open findings, replanning decisions, and whether the change may proceed to sync/archive/PR.
