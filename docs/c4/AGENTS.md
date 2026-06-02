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
launched with `npx -y @likec4/mcp` and `LIKEC4_WORKSPACE=docs/c4`. It exposes
this model and its views to MCP-capable agents so they can read and query the
architecture without exporting diagrams. It needs `npx` (Node) on `PATH`, in
line with the ambient `npx likec4 ...` workflow below.

## Local rules

- LikeC4 element descriptions and architecture view titles should keep the repository's existing documentation language, which is Russian for product/architecture text.
- Agent-facing instructions may use English.
- Treat `.c4` files as architecture source, not export output. Update model/views instead of editing generated images.
- Keep views purposeful. Split crowded views by perspective, feature area, runtime flow, deployment target, or persistence boundary.
- Keep view identifiers stable because they are useful for URLs and exports.
- Mark planned or future architecture explicitly with existing LikeC4 tags such as `#future`; do not describe planned work as implemented.
- Keep `docs/arc42` text synchronized with major LikeC4 changes.

## Local verification

- The repository currently has no root Node package-manager project for LikeC4; use the documented ambient `npx likec4 ...` commands until a project-local toolchain is introduced deliberately.
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
