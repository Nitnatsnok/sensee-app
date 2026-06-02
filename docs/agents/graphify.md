# graphify Knowledge Graph

[graphify](https://github.com/safishamsi/graphify) builds a navigable knowledge
graph of the codebase: an AST-derived map of nodes (functions, classes,
modules) and edges (calls, implements, shares-data) with community clustering
and "god node" / bridge analysis. Agents use it to orient in the code before
broad architecture questions, instead of grepping blindly.

This is an agent-facing tool note. It does not define project rules; the root
`AGENTS.md` does.

## Where the graph lives

- `graphify-out/graph.json` — the graph (machine-readable, ~10 MB).
- `graphify-out/GRAPH_REPORT.md` — human-readable audit (god nodes, surprising
  connections, communities, suggested questions).

These two files are **tracked in git**; everything else under `graphify-out/`
(manifest, cache, html, the machine-specific `.graphify_python`) is gitignored.
Tracking the graph means it **travels with the branch**: `git checkout` restores
the graph that matches that branch's code, so no rebuild is needed on a switch —
the committed graph matches the checked-out branch state. Uncommitted working-tree
edits are intentionally not reflected until they are staged and committed.

`graph.json` (~10 MB, not newline-terminated) is exempt from the `pre-commit`
size/format guard (see `.githooks/pre-commit`), and is marked
`-text -diff merge=graphify` in `.gitattributes` so git avoids huge textual
diffs and uses the local graphify merge driver instead of leaving conflict
markers (driver configured by `scripts/setup-git-hooks.sh`).

## What gets indexed

Scanning scope is controlled by the tracked `.graphifyignore` (gitignore
syntax). It is a blacklist, not a whitelist:

- `*.json` is excluded. graphify treats `.json` as a code extension, but every
  `.json` here is fixture/config data (`mockFixtures/`, asset catalogs, tool
  config). Indexing it produced two 500-edge `lemmas` god nodes from
  `cefr.json` / `frequency.json` that drowned out the real abstractions.
- `.gradle/`, `.kotlin/`, `kotlin-js-store/` are excluded — graphify reads
  `.graphifyignore` *instead of* `.gitignore` in this directory, and these
  build/tooling dirs are not in graphify's built-in skip list.

A `* + !src/**` whitelist does **not** work: gitignore's parent-exclusion rule
keeps the excluded intermediate dirs from being re-included, so the corpus comes
out empty.

## Querying the graph

Read-only, no API cost. From the repo root:

```sh
graphify query "how does lexical verification routing work"   # BFS, broad context
graphify query "how does a captured sense reach the SRS deck" --dfs   # trace a path
graphify path "RoutingLexicalVerifier" "CachingLexicalVerifier"        # shortest path
graphify explain "runCatchingCancellable"                              # one node + neighbors
```

Agents can also reach the graph live through the **`graphify` MCP server**,
exposing `query_graph`, `get_node`, `get_neighbors`, `get_community`,
`god_nodes`, `graph_stats`, and `shortest_path`. Codex uses the committed
`.codex/config.toml`; Claude-style MCP clients use the committed `.mcp.json`.
Both launch the server with
`uv run --with graphifyy python -m graphify.serve graphify-out/graph.json`, a
portable command (no machine-specific interpreter path), so they need `uv` on
`PATH`.

## Rebuilding

- **Automatic (pre-commit):** Lefthook runs `.githooks/graphify-rebuild`, which
  calls graphify's incremental code rebuild with the staged `changed_paths`, then
  `git add`s `graph.json` + `GRAPH_REPORT.md` so they land **in the same commit**,
  in sync with the staged code. The normal path reads the worktree and re-extracts
  only changed files. When the same path has both staged and unstaged edits, the
  hook falls back to a temporary staged-index checkout so partial staging cannot
  leak unstaged code into the graph. Deleted paths are included in the change set
  so graphify can evict stale nodes. AST-only (no network/LLM). It never blocks
  the commit on graphify failures (any failure exits 0) and skips during
  rebase/merge/cherry-pick. `PYTHONHASHSEED=0` keeps clustering deterministic so
  the graph does not churn run-to-run. Opt out with `GRAPHIFY_SKIP=1`.
- No `post-checkout` / `post-merge` hook is needed — the tracked graph is
  restored by git on checkout and union-merged on merge.
- **Manual:** `graphify update .` (full, ~30s) or the `/graphify` skill for a
  fresh build with re-clustering and community labels. Use
  `graphify update . --force` after large deletions when `update` refuses to
  shrink the graph. The hook's incremental rebuild is the canonical path: a full
  `graphify update .` re-extracts everything instead of preserving unchanged
  nodes, so it can report a slightly different node/edge count than the
  incremental result. Run it to recover from a known drift, not routinely before
  a commit.

Requires graphify on `PATH` (`uv tool install graphifyy`), version `0.8.x` (the
hook imports graphify's internal incremental `_rebuild_code` entry point, which
can change between releases — pin a compatible version before upgrading). The
hook skips silently when graphify is absent.
