# Agent Instructions for `docs/c4`

This file extends the root `AGENTS.md` and `docs/AGENTS.md`.
Follow those rules first; this file only adds local rules for the LikeC4 model.

## Scope

Applies to:
- `docs/c4/...`

## Local context

This directory contains the canonical LikeC4 architecture model and views for the Sensee client:
- `model.c4` - model elements and relationships.
- `views.c4` - static and dynamic views.
- `specification.c4` - custom element kinds and tags.
- `likec4.config.json` - LikeC4 workspace config.

## MCP server

A LikeC4 MCP server is configured as `likec4` for Codex in
`.codex/config.toml` and for Claude-style MCP clients in `.mcp.json`. It is
launched by Codex with `npx -y @likec4/mcp` and `cwd = "docs/c4"` from
`.codex/config.toml`. Codex resolves project-scoped MCP `cwd` values from the
repository root, so the MCP process current directory is the LikeC4 workspace.
It exposes this model and its views to MCP-capable agents so they can read and
query the architecture without exporting diagrams. It needs `npx` (Node) on
`PATH`, in line with the ambient `npx likec4 ...` workflow below.

## Modeling conventions

- The model uses C4 nesting: `client` (system) → `container` (module group, e.g. a feature or `shared/<area>`) → `component` (module) / `store` (local storage) / `capability` (user-meaningful product capability, possibly realized across several modules, e.g. `vocabulary_capture`, `card_derivation`). Avoid a flat list of sibling elements under the system.
- Element kinds carry meaning: `container`, `component`, `store`, `capability`, `external_system`, `client_app`. Do not model a Gradle module, a runtime target, and a product capability with the same kind.
- Relationship kinds separate semantics: `depends` (static/DI dependency), `flows` (runtime data/control flow), `navigates` (user navigation between sections). Pick the dominant semantic instead of one untyped edge.
- Platform deployable targets live in the `deployment {}` model (`deploymentNode platform`), not as logical elements.
- A new shared module/seam becomes a `component` in its `container` (or a new container for a standalone area); a feature/seam schema is a `store` owned by that feature/seam and aggregated by an edge from `persistence`.
- Feature containers model the navigable product sections (Home, Practice, Library, Vocabulary Editor, Profile). A bootstrap or cross-cutting feature module — e.g. `shared/feature/startup` — is modeled where it runs (`shell.splash`), not as a separate navigable container; keep its module name visible in the element `technology`.

## Local rules

- LikeC4 prose (element/relationship descriptions, view titles) is Russian for product/architecture text; identifiers, `technology` values, module names, and technical terms stay English, consistent with the root language policy. Agent-facing instructions may use English. Relationship labels in `model.c4` are English (technical wiring); dynamic-view step labels are Russian narration.
- View titles use one consistent Russian category prefix — `Контекст` / `Структура` / `Компоненты` / `Поведение` / `Развёртывание` / `Планируемое` — followed by ` / ` and a short name; module and scenario proper nouns (e.g. `Practice`, `Library`) may stay English. LikeC4 renders the segment before ` / ` as a navigation group, so the prefix set is functional, not decorative — do not mix English category words (`Runtime`, `Planned`) into it.
- Treat `.c4` files as architecture source, not export output. Update model/views instead of editing generated images.
- Keep views purposeful. Prefer scoped `view ... of <container>` over one overloaded diagram. Aim for roughly ≤15 *expanded leaf* nodes in component/feature views; parent and collaborator boundary frames (a container shown only to host one included child) and a top-level container overview does not count against that budget; `containers` itself is a curated product-dependency slice (feature → seam/domain deps, cross-feature, external boundaries), with storage, network/providers, and DI composition delegated to `database_aggregation` / `integration_seams` / `primary_navigation_shell`. Split crowded views by perspective, feature area, runtime flow, deployment target, or persistence boundary, and keep planned cross-feature navigation in `cross_feature_navigation` instead of letting it leak into every component view (use `exclude * -> * where tag is #prepared` / `#future` for that). `implicitViews: true` also generates per-element views, so the canonical set in `docs/README.md` is a curated subset.
- Keep view identifiers stable because they are used in URLs, exports, and arc42/README prose. Do not rename casually; when a rename is necessary, update the canonical list in `docs/README.md` and the arc42 prose references in the same change.
- Mark planned or future architecture with `#prepared` (code exists but is not wired) or `#future` (not yet implemented), on both elements and relationships; do not describe planned work as implemented. Planned cross-feature navigation is a relationship, not a fake element — keep it as a tagged edge, collected in the `cross_feature_navigation` view so it has one reviewable home. Audit accordingly: planned *elements* via `query-by-tags` / `query-by-tag-pattern`, planned *relationships* via `read-view` / `find-relationships` or by reading the source — tag queries return elements only, but relationship tags are still exposed there.
- Do not re-enumerate the canonical view list outside `docs/README.md`; arc42 references it instead of duplicating it.
- Keep `docs/arc42` text synchronized with major LikeC4 changes.

## Local verification

- To read or query the model (elements, relationships, tags, views), prefer the `likec4` MCP server described above (read-only); use the CLI below for validation, preview, and static build.
- The repository currently has no root Node package-manager project for LikeC4; use the documented ambient `npx likec4 ...` commands until a project-local toolchain is introduced deliberately (`early-stage`; tightening trigger in arc42 §11).
- Validate LikeC4 after changing `.c4` or `likec4.config.json`:
  ```shell
  npx likec4 validate docs/c4
  ```
- For local preview:
  ```shell
  npx likec4 start docs/c4
  ```
- Static exports, when needed for review, should go to a temporary or ignored build output location such as `build/likec4/docs-c4`.

## Do not

- Do not add generated diagram artifacts to documentation by default.
- Do not duplicate structural relationships in long Markdown prose when the LikeC4 model already captures them.
- Do not rename views casually; view names are part of the reviewer workflow.

## Related skills

- `.agents/skills/likec4-architecture-model-review`
- `.agents/skills/architecture-docs-sync`
