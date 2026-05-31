# Codex Local Environment

Use this page to configure OpenAI Codex App for this repository without duplicating the shared agent rules.

## Instruction Source

Codex should read repository instructions from `AGENTS.md` and nested `AGENTS.md` files. Project skills live in `.agents/skills` and are cataloged in `docs/agents/skills.md`.

Do not create Codex-only copies of these rules.

## Local Environment Setup

Official Codex App docs describe Local Environments as project settings for worktree setup scripts and common actions. The app stores generated shared configuration in `.codex/` at the project root, and that file can be checked into Git for the team.

This repository commits the project Local Environment config at:

```text
.codex/environments/environment.toml
```

The committed automatic setup script is:

```powershell
pwsh -File scripts/codex/setup-local-env.ps1
```

This matches the confirmed TOML shape from the Codex App generated example, which has a single `[setup]` script entry. The committed setup uses the Windows-safe PowerShell wrapper because this repository setup was validated on Windows and `pwsh` is available there.

macOS and Linux users have platform-specific `Setup Local Env` actions in the same file:

```shell
sh scripts/codex/setup-local-env.sh
```

Both wrappers delegate to `scripts/agents/setup-local-env.*`, which runs only lightweight setup and `Gradle help`.

The config intentionally omits `[cleanup]`.

Cleanup decision:

- `scripts/agents/setup-local-env.*` prepares the worktree, writes only ignored `local.properties` inside the worktree when an Android SDK is detected, may adjust `./gradlew` executability on Unix, and runs Gradle `help`.
- Gradle may populate normal user-level Gradle caches, but those caches are not project-owned resources and must not be removed by a repository cleanup action.
- There is no emulator, device, server, background process, temporary external service, or project-owned external resource to clean up.

Do not add cleanup just because a generated example contains a placeholder. Add `[cleanup]` only if a future setup step creates a concrete project-owned resource and the Codex environment schema still supports cleanup for that use case.

## Actions

| Action | macOS/Linux command | Windows command |
| --- | --- | --- |
| Setup Local Env | `sh scripts/codex/setup-local-env.sh` | `pwsh -File scripts/codex/setup-local-env.ps1` |
| Gradle Tasks | `sh scripts/agents/list-tasks.sh` | `pwsh -File scripts/agents/list-tasks.ps1` |
| Validate Fast | `sh scripts/agents/validate.sh fast` | `pwsh -File scripts/agents/validate.ps1 -Mode fast` |
| Validate Code | `sh scripts/agents/validate.sh code` | `pwsh -File scripts/agents/validate.ps1 -Mode code` |
| Validate Docs | `sh scripts/agents/validate.sh docs` | `pwsh -File scripts/agents/validate.ps1 -Mode docs` |
| Validate Architecture | `sh scripts/agents/validate.sh architecture` | `pwsh -File scripts/agents/validate.ps1 -Mode architecture` |
| Gradle Check | `./gradlew --no-daemon check` | `.\gradlew.bat --no-daemon check` |

Prefer module-scoped Gradle tasks from `AGENTS.md` when a change is narrow.

`Gradle Check` is a deliberate explicit action only. It is not part of setup.

## Aggregate Verification Decision

Current root Gradle tasks include `check` and `konsistCheck`. There are no project-level `verify`, `verifyDocs`, or `verifyArchitecture` tasks yet.

Decision: do not add new Gradle aggregate tasks in this agent-environment pass. `scripts/agents/validate.*` checks whether optional aggregate tasks exist, uses `check` or `konsistCheck` when appropriate, and reports missing optional docs/architecture aggregates clearly. Adding `verifyDocs` / `verifyArchitecture` remains future deliberate build-logic work, not an unresolved setup blocker.

## Worktrees

Codex App worktrees use Git worktrees. Each worktree has its own file checkout, so ignored local files such as `local.properties` are not carried over automatically.

The setup script regenerates `local.properties` from `ANDROID_HOME` or `ANDROID_SDK_ROOT` when an Android SDK is available. If neither variable is set, the script warns and continues.

Use Codex Handoff when moving a thread between Local and Worktree. Codex handles the Git movement between the two checkouts, but ignored files do not move with the thread. Before handoff, keep generated files, secrets, and local-only settings out of Git.

## Future Codex Config Changes

Future Codex App updates may generate additional `.codex` files or modify `.codex/environments/environment.toml`.

Decision: generated Codex config changes must be reviewed before committing:

- no machine-specific paths;
- no secrets;
- setup/actions call `scripts/codex/` or `scripts/agents/`;
- no copied `AGENTS.md` or duplicated skills.

## References

- OpenAI Codex Local Environments: https://developers.openai.com/codex/app/local-environments
- OpenAI Codex Worktrees: https://developers.openai.com/codex/app/worktrees
- OpenAI Codex `AGENTS.md`: https://developers.openai.com/codex/guides/agents-md
