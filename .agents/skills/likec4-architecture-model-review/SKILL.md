---
name: likec4-architecture-model-review
description: Use this skill when Sensee work touches docs/c4 .c4 files, likec4.config.json, architecture model elements, relationships, C4-style views, generated diagram conventions, docs that reference LikeC4 views, or architecture changes that may need model updates.
---

# LikeC4 Architecture Model Review

Status: active.

## Operating mode

Review/support. Recommend model/view changes or validation; edit LikeC4 sources only when the current task asks for changes.

## Non-goals

- Do not update LikeC4 for trivial local implementation changes.
- Do not commit generated diagram exports by default.

## Workflow

1. Read `docs/c4/AGENTS.md`, `docs/README.md`, and the affected `.c4` files.
2. Compare the model and views with actual modules in `settings.gradle.kts` and relevant `build.gradle.kts` files.
3. Check whether arc42/ADR text references the same views and architecture state.
4. Recommend model/view changes or explicitly say no model update is needed.

## Check

- LikeC4 is treated as the architecture model source, not a generated-image format.
- Elements represent real ownership and boundaries in the repository.
- Relationships are meaningful and named when needed.
- Views are purposeful projections, not one overloaded diagram.
- Views split by perspective, feature area, bounded context, runtime concern, deployment target, persistence boundary, or use case when crowded.
- View names are stable and useful for URLs/exports.
- Planned architecture is explicitly marked with existing tags such as `#future`.
- Markdown docs do not duplicate structural relationships already captured in LikeC4 unless there is a clear reason.
- ADRs explain decisions; LikeC4 shows resulting structure/relationships.
- Generated artifacts follow repository convention and are not committed by default.

## Verification

Use repository-defined commands:

```shell
npx likec4 validate docs/c4
npx likec4 start docs/c4
npx likec4 build docs/c4 -o build/likec4/docs-c4
```

Run validation after changing `.c4` or `likec4.config.json`. Build/serve only when useful for review.

## Output

- Model/view summary
- Architecture drift risks
- View readability issues
- Missing or stale relationships
- Docs/ADR impact
- Suggested LikeC4 verification
