---
name: backlog-maintenance
description: Use this skill when Sensee work surfaces deferred cross-cutting or product work that will not ship in the current change, or when adding, updating, or removing an item in docs/backlog/ — allocate the EB-N id, follow the file-per-item schema, keep the index and any @planned scenario / arc42 NOTE references in sync, and delete items once implemented.
---

# Backlog Maintenance

Status: active.

## Operating mode

Implementation-support. The canonical policy and item schema live in
`docs/backlog/index.adoc`; this skill is the procedure, not a second copy of the
rules. Read the index first and follow it.

## Non-goals

- Do not record architectural decisions here — those belong in `docs/adr/`.
- Do not keep implemented items as a changelog; git history is the journal.
- Do not slice or plan the implementation work itself — that is `agent-task-slicer`.
- Do not decide broad documentation impact — that is `architecture-docs-sync`.

## Workflow

1. Read `docs/backlog/index.adoc` (policy, statuses, schema, the "Как вести backlog"
   section) and follow `docs/AGENTS.md` for language and tone (Russian, AsciiDoc).
2. Add an item: take the next free `EB-<N>` (monotonic, never reused — ids are cited
   in arc42, scenarios, and code). Create `docs/backlog/EB-<N>-<slug>.adoc` from the
   schema and add a row to the "Открытые пункты" (`status: open`) or "Обдумываемое"
   (`status: considering`) table.
3. Update an item: change `:status:`, the trigger, or the "Связи" block as the code
   evolves; keep the file title and the index row consistent.
4. Remove an item (implemented): delete the item file and its index row. If it backed
   a `@planned` scenario or an arc42 `NOTE`, move that to `@implemented` / update the
   text in the same change.
5. Verify the item is real before keeping it: confirm the gap against current code and
   cite concrete anchors (types/files in backticks) in "Связи".

## Check

- Id is unique and not reused; filename, title id, and index row agree.
- Required fields present: `= EB-<N>: ...`, `:status:` (`open` | `considering`),
  "Триггер / что разблокирует".
- The item describes a real, current gap — not already-implemented behavior.
- No duplicate list of items elsewhere: scenarios / arc42 / `README.md` reference
  `docs/backlog/` and the id, they do not re-enumerate the backlog.
- After editing skills or agent-facing instructions, run
  `python3 scripts/agents/validate-agent-instructions.py`.

## Output

- The created/updated/removed item file
- The synced `docs/backlog/index.adoc` row
- Any scenario / arc42 reference moved or repointed
- Note of remaining drift, if a referenced doc still claims the old state
