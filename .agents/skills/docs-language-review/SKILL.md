---
name: docs-language-review
description: Use when reviewing Russian or mixed-language documentation prose (docs/**, .adoc/.md/.feature/.c4) for language mixing, first-occurrence term glossing, abbreviation expansion, and spelling/punctuation/grammar/style. Not for code comments or KDoc placement (code-comments-and-docs-review); never touches code identifiers or backticked terms.
---

# Docs Language Review

Status: active.

## Operating mode

Review-only. Check Russian or mixed-language documentation prose for language and terminology issues. Format-agnostic (`.adoc`/`.md`/`.feature`/`.c4` prose); do not reformat structure or touch code.

## Non-goals

- Do not touch code identifiers, module/type names, Gradle paths, or backticked terms — the root language policy keeps these English.
- Do not review code comments or KDoc placement — that is `code-comments-and-docs-review`.
- Do not rewrite content or change meaning; report and suggest.

## Workflow

1. Read the file and the glossary (`docs/arc42/sections/12_glossary.adoc`) for canonical term definitions and any RU/EN pairs.
2. Apply the four checks below to prose only.
3. Suggest minimal edits; preserve quoted original-language text.

## Check

1. Language mixing — prefer the established Russian term when one exists; use English only for terms genuinely ambiguous in Russian or for stable abbreviations/technical terms. Flag gratuitous English where a clean Russian equivalent exists.
2. Term glossing — on a term's first occurrence in a file, give its English equivalent in parentheses when it aids understanding; reuse the glossary's terms and add new RU/EN pairs there instead of inventing per-file translations.
3. Abbreviation expansion — expand each abbreviation in parentheses on its first occurrence in a file.
4. Language errors — spelling, punctuation, grammar, and style.

Not a violation (avoid false positives): backticked code terms, proper nouns, and module/identifier names; an English term that has no clean Russian equivalent.

## Output

- Findings by type (mixing / glossing / abbreviation / spelling / punctuation / style), each with location and a minimal suggested fix
- New term pairs to add to the glossary
