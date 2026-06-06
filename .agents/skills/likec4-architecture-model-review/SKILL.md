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
2. Query the model with the `likec4` MCP when connected (`read-project-summary`, `read-element`, `find-relationship-paths`, `query-by-tags`) instead of re-deriving structure by hand; it is read-only.
3. Compare the model and views with actual modules in `settings.gradle.kts` and relevant `build.gradle.kts` files. Cross-check each non-trivial relationship against real code — module `build.gradle.kts` `implementation(projects...)`, imports, call sites — to catch model↔code drift: a static dependency present in code but missing from the model, or a model edge with no code basis. The `graphify` MCP (`query_graph`, `get_neighbors`) helps surface real cross-module edges, but confirm hits in the source before recommending a model change.
4. Check whether arc42/ADR text references the same views and architecture state.
5. Recommend model/view changes or explicitly say no model update is needed.

## Check

- The change follows the `docs/c4/AGENTS.md` modeling conventions (element kinds, C4 nesting, typed relationships `depends`/`flows`/`navigates`, `deployment {}` targets, view budget, planned `#prepared`/`#future` tagging, stable view ids) — verify against them rather than restating them here.
- Elements and relationships reflect real ownership/boundaries and real code edges: no model edge without a code basis, no code dependency missing from the model.
- Views stay purposeful and readable; planned cross-feature navigation is collected in `cross_feature_navigation`, not leaked into every component view.
- A view-id rename updates the canonical list in `docs/README.md` and arc42 references in the same change.
- Markdown does not duplicate the model; ADRs explain decisions while LikeC4 shows resulting structure; generated artifacts are not committed by default.

## Verification

Read/query the model with the `likec4` MCP (read-only). Validate, preview, and build with the repository CLI:

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
