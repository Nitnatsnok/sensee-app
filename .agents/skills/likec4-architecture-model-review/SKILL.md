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

- LikeC4 is the architecture model source, not a generated-image format.
- Elements represent real ownership and boundaries; kinds carry meaning (`container` = module group, `component` = module, `store` = local storage, `capability` = cross-cutting product capability, `external_system` = outside the client).
- Structure uses C4 nesting (system → container → component), not one flat tier; platform targets live in the `deployment {}` model, not as logical elements.
- Relationships use the right kind: `depends` (static/DI), `flows` (runtime data), `navigates` (user navigation) — not one untyped edge space.
- Views are purposeful projections, not one overloaded diagram; prefer scoped `view ... of <container>`. Aim for ≤15 *expanded leaf* nodes in component/feature views — parent/collaborator boundary frames and the C4 `containers` overview (which legitimately shows every module group) do not count. Keep planned cross-feature navigation in `cross_feature_navigation` via `exclude * -> * where tag is #prepared` / `#future` (a relationship predicate — `exclude * where tag is ...` filters elements, not edges), not in every component view. Remember `implicitViews: true` also generates per-element views.
- View ids are stable (URLs/exports/arc42 prose). A rename updates the canonical list in `docs/README.md` and arc42 references in the same change.
- Planned architecture is tagged `#prepared` (code exists, not wired) or `#future` (not implemented), on elements and relationships. Audit planned elements via `query-by-tags` / `query-by-tag-pattern` and planned relationships via `read-view` / `find-relationships` (tag queries return elements only, but relationship tags are exposed there); confirm planned cross-feature navigation is collected in the `cross_feature_navigation` view.
- Markdown docs do not duplicate the model — the canonical view list lives in `docs/README.md`, not re-enumerated across arc42.
- ADRs explain decisions; LikeC4 shows resulting structure. Generated artifacts are not committed by default.

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
