# Chapter 10 — Quality Requirements (Требования к качеству)

## Purpose
Hold all quality requirements as a quality tree with scenarios — the elaboration of the top quality goals from §1, including lower-priority ones. Quality requirements strongly influence architecture decisions.

## Required / recommended subsections
- **Quality Requirements Overview** — refine the top quality goals from §1 (do not duplicate them; link back). The form is interchangeable: a simple **table**, a **mind-map**, or a quality **tree** (the tree is one documented option, not mandatory); whichever form is used, connect it to the scenarios below.
- **Quality Scenarios** — concrete scenarios: usage scenarios (system reaction to a stimulus), change scenarios (system modification), and fault scenarios. Each must be **specific and measurable** (e.g. "responds within one second"), in tabular or short/long text form.

## Form
Quality tree (tree/table) plus a scenario table; order by priority.

## Do not put here
- The 3–5 top quality goals themselves — they originate in §1; here they are elaborated with a back-reference.

## Related chapters
§1 (top quality goals), §11 (risks against quality), §4/§8 (how quality is achieved). In Sensee, `QG-*` live in §1; `US-*`/`CS-*`/`FS-*` (usage/change/fault) live here; ADR decision drivers reference them.

## Source
arc42 §10 — https://docs.arc42.org/section-10/
