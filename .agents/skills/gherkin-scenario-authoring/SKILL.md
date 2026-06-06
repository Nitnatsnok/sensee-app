---
name: gherkin-scenario-authoring
description: Use when writing or reviewing a Gherkin .feature behavior scenario in docs/scenarios (declarative style, Given/When/Then roles, one-behavior scenarios, tags, русские ключевые слова), following docs/scenarios/README.md. Not for arc42 runtime prose (arc42-authoring), ADRs (adr-authoring), or general prose proofreading (docs-language-review).
---

# Gherkin Scenario Authoring

Status: active.

## Operating mode

Review/support. Author or review a `.feature` behavior scenario against Gherkin/BDD best practice and `docs/scenarios/README.md` (canonical project conventions). Edit only when the task asks. Format-agnostic; keep code terms in backticks.

## Non-goals

- Do not duplicate `arc42/06` runtime prose — scenarios trace to it, they do not restate it.
- Do not turn a scenario into an imperative UI script or an automation test.
- Do not introduce a third status tag beyond `@implemented`/`@planned`.

## Workflow

1. Read `docs/scenarios/README.md` for the canonical conventions: status tags, `# language: ru`, the "Источники реализации:" block, and traceability to `arc42/06`.
2. Decide: a new `@planned` scenario or an update to an `@implemented` one.
3. Apply the best practice below plus the project conventions; keep `@implemented` in step with code and `@planned` as the target.

## Check

Best practice:

- Declarative, not imperative — describe WHAT the system does, not HOW (no UI keystrokes/selectors). Test: would the wording change if the implementation changed? If yes, it is too imperative. (cucumber.io/docs/bdd/better-gherkin)
- One behavior per scenario — one main action→outcome; extra `When→Then` pairs signal a second behavior, so split it. (Automation Panda, "BDD 101: Writing Good Gherkin")
- Step roles: `Given` = context/precondition, `When` = the single triggering action, `Then` = an observable result. Do not reassign roles.
- Conjunctive steps are an anti-pattern: do not put several ACTIONS in one step. Separate `И`/`And` steps are fine — including several observable-property assertions under `Then`. (github.com/andredesousa/gherkin-best-practices)
- Meaningful `Функция`/`Сценарий` names; the user's/business point of view; scenarios independent of execution order; `Предыстория`/Background only for shared context; `Структура сценария`/Scenario Outline for variations of one behavior, not overused.

Project conventions (canonical in `docs/scenarios/README.md`):

- `# language: ru` with Russian keywords (Функция/Сценарий/Допустим/Когда/Тогда/И).
- Exactly two status tags — `@implemented` (matches code) or `@planned` (target); other tags are thematic, not status. A file that mixes statuses tags each scenario individually, not at the feature level.
- Keep the "Источники реализации:" block pointing at real classes/files and the "Связанные схемы в `docs/c4/`" note.
- A `.feature` file groups the scenarios of one behavior area (one runtime view from `arc42/06`) and normally holds several `Сценарий:` — do NOT demand one scenario per file.

## Output

- Findings: anti-patterns (imperative wording, conjunctive actions, role mixing, hidden second behavior, UI coupling)
- Suggested rewrites
- Status-tag, traceability, and `arc42/06` sync notes
