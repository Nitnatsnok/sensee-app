# Agent Skills Catalog

`.agents/skills` contains project-specific skills for repeatable agent workflows. They complement `AGENTS.md`: `AGENTS.md` files define project rules, while skills provide task-specific procedures.

`CLAUDE.md` in the root and beside each `AGENTS.md` stays a thin `@AGENTS.md` adapter. For Claude Code, this keeps one source of rules without copying text. The commit convention lives separately in `docs/engineering/commits.md`.

Architecture knowledge should not move into skills. ADRs explain decisions, `docs/arc42` describes the client state, and `docs/c4/*.c4` remains the canonical LikeC4 model.

## Skills

| Skill | When to use | Status | Mode | Primary output |
| --- | --- | --- | --- | --- |
| `agent-task-slicer` | Slice a large task into safe independent steps | active | planning-support | Slices, non-goals, verification |
| `pr-diff-review` | Review the current diff before finalization or PR | active | review-only | Findings by severity |
| `commit-preparation` | Prepare commit split and messages | active | review/support | Commit messages, suspicious files |
| `code-comments-and-docs-review` | Review KDoc, comments, and nearby docs | active | review/support | What to add, remove, or move to docs |
| `kmp-module-boundary-review` | Check dependencies and module boundaries | active | review/support | Boundary risks and minimal fix |
| `gradle-convention-plugin-change` | Change Gradle convention plugins and quality wiring | active | implementation-support | Build logic impact and checks |
| `kmp-source-set-review` | Check source-set placement and platform leakage | active | review/support | Source-set issues and commands |
| `decompose-navigation-review` | Check Decompose configs, stacks, and restoration | active | review/support | Navigation risks and ownership |
| `compose-multiplatform-ui-review` | Check Compose UI and design-system changes | active | review/support | UI/API consistency issues |
| `sqldelight-schema-aggregation-review` | Check schema ownership, migrations, aggregation | active | review/support | Persistence risks and test gaps |
| `fsrs-srs-engine-review` | Check FSRS/SRS scheduling behavior | active | review/support | Scheduling invariants and edge cases |
| `observability-logging-review` | Check logging, diagnostics, analytics, breadcrumbs | active | review/support | Sensitive-data and backend risks |
| `dictionary-enrichment-schema-review` | Check vocabulary enrichment, verification, schema | active | review/support | Schema/prompt/verification risks |
| `likec4-architecture-model-review` | Check LikeC4 model and views | active | review/support | Drift, view readability, validation |
| `architecture-docs-sync` | Decide whether docs/ADR/LikeC4 updates are needed | active | review/support | Docs impact and concise patch suggestion |

## Refinement

- Add gotchas after repeated agent mistakes.
- Tighten `description` if a skill triggers too often or misses relevant requests.
- Expand `evals/evals.json` only for important workflows where real runs expose a weak spot.
- Do not copy long architecture references into skills; link to `docs/`, ADRs, and LikeC4 instead.

## Validation

After changing agent-facing instructions or skills, run the read-only validator:

```shell
python3 scripts/agents/validate-agent-instructions.py
```

On Windows:

```shell
py -3 scripts/agents/validate-agent-instructions.py
```

The validator checks:

- skill frontmatter (`name` and `description`);
- skill folder/name consistency;
- commit convention links to `docs/engineering/commits.md`;
- thin `CLAUDE.md` shims and `AGENTS.md`/`CLAUDE.md` sibling pairing;
- structural eval JSON under `.agents/skills/*/evals/*.json`.

It does not perform semantic review of skill quality and does not judge whether eval prompts are good. Semantic review still belongs to manual review and the relevant project skills.
