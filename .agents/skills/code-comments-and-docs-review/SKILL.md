---
name: code-comments-and-docs-review
description: Use this skill when Sensee work touches public APIs, module contracts, KMP source-set constraints, Gradle plugin APIs, Decompose navigation, SQLDelight ownership, FSRS scheduling, enrichment schemas, or complex Compose behavior and comments/KDoc/docs need review or improvement.
---

# Code Comments and Docs Review

Status: active.

## Operating mode

Review/support. Recommend comment, KDoc, or docs changes; edit only when the current task asks for changes.

## Non-goals

- Do not add comments for obvious code.
- Do not replace tests, ADRs, or LikeC4 with long local prose.

## Workflow

1. Read nearby code and the closest `AGENTS.md`.
2. Decide whether the invariant belongs in a name, test, KDoc, code comment, ADR, arc42 text, or LikeC4 view.
3. Prefer removing stale or obvious comments over adding prose.
4. Keep docs factual: implemented first, planned only when explicitly marked.

## Check

- Comments explain why, invariants, contracts, or trade-offs, not obvious code.
- KDoc and code comments are written in English.
- Public API contracts have enough KDoc for callers in other modules.
- Tests pin behavior when prose would become stale.
- ADR/docs are better than a large local comment for architecture decisions.
- LikeC4 is better than Markdown prose for structural relationships.
- Existing comments are still true after the diff.
- User-facing product and architecture docs follow `docs/AGENTS.md` and use Russian prose; agent-facing files and engineering-process docs use English by default.

## Output

- Comment/doc summary
- Comments to add
- Comments to remove
- KDoc/public API documentation needed
- Better handled by tests
- Better handled by ADR/docs/LikeC4
- Risks of stale documentation
