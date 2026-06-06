---
name: adr-authoring
description: Use when writing or reviewing an Architecture Decision Record in docs/adr — decide if a decision is ADR-worthy and structure context/decision/alternatives/consequences with value-neutral context and honest trade-offs, following docs/adr/index.adoc. Not for the LikeC4 structure (likec4-architecture-model-review) or arc42 chapters (arc42-authoring).
---

# ADR Authoring

Status: active.

## Operating mode

Review/support. Decide whether a decision is ADR-worthy, then author or review the record against `docs/adr/index.adoc` (canonical policy) and ADR best practice. Edit ADRs only when the task asks. Format-agnostic (AsciiDoc here); do not reformat unrelated text.

## Non-goals

- Do not write an ADR for a local decision or one already covered elsewhere — document those near the code.
- Do not split one coherent decision into several ADRs (project rule: the set stays minimal).
- Do not duplicate structure or relationships — that is the LikeC4 model in `docs/c4/`.

## Workflow

1. Read `docs/adr/index.adoc` for the canonical policy: field schema, statuses, numbering, conventions.
2. Decide ADR-worthiness: a significant, expensive, hard-to-reverse, or risky decision affecting structure, boundaries, or quality attributes. If it is local — keep it near the code.
3. Take the next sequential `NNNN` (never reused); create `docs/adr/NNNN-kebab-title.adoc`; add the row to `index.adoc`.
4. Write per the project field schema; link decision drivers to `QG-*` (arc42/01) and `US-*`/`CS-*`/`FS-*` (arc42/10) and to related ADRs.
5. To change or reverse an accepted decision, do not rewrite it — author a new ADR that supersedes it and mark the old one `Superseded by ADR-NNN` (two-way link).

## Check

- One coherent decision per ADR; genuinely ADR-worthy (not local, not already covered).
- Project field schema present: `Статус`, `Дата`, `Ответственные`, `Драйверы решения` (→ `QG-*`/`US-*`/`CS-*`/`FS-*`), `Контекст`, `Решение`, `Альтернативы`, `Последствия`.
- `Контекст` is value-neutral — forces and facts, no verdict (Nygard); move judgements into `Альтернативы`/`Последствия`.
- `Решение` is in active voice (Nygard's "We will …"; in this repo e.g. «Используем …» / «Возврат … проецируется …»).
- `Последствия` are honest — both the benefits and the accepted cost; `(реализованные)` and `(ожидаемые)` are not mixed.
- `Альтернативы` lists the serious rejected options, each with the reason it was rejected.
- Lifecycle respected: `Proposed` → `Accepted`; an accepted ADR is immutable in substance — reverse it via `Superseded by ADR-NNN`, `Deprecated`, or `Rejected`, never by editing the conclusion.
- The ADR explains WHY for a future reader; it does not duplicate other docs; structure is shown by LikeC4, not the ADR.
- A decision is not presented as implemented in arc42 until `Accepted` and confirmed by code (project rule).
- `index.adoc` row and sequential numbering stay consistent.

Standards (background only — the field form is strictly `docs/adr/index.adoc`, Nygard-style): Nygard — https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions.html ; MADR — https://adr.github.io/madr/ .

## Output

- ADR needed: yes/no with reason
- Draft or patch following the project field schema
- `index.adoc` row and numbering
- Lifecycle/supersede actions and decision-driver links
