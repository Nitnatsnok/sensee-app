#!/usr/bin/env python3
"""Generate an Obsidian vault from graphify-out/graph.json (no graph rebuild).

Writes one note per graph node, `_COMMUNITY_*` overview notes, and graph.canvas.
Community notes are auto-labelled by their dominant module path and annotated
with cohesion. Reads the committed graph, so it reflects the current branch with
no rebuild.

The default output dir is a sibling of the repo, OUTSIDE the project tree
(`../<repo-name>-graphify-vault`). This is deliberate: a vault is thousands of
tiny files, and an IDE (Android Studio / IntelliJ) indexing them inside the repo
will hang. Pass an explicit OUTPUT_DIR to override. The default dir is cleaned
before writing; a caller-supplied dir is written into without deleting.

Open the resulting folder as an Obsidian vault; the graph view colours nodes by
community and graph.canvas lays communities out as groups.

Usage: python scripts/graphify-obsidian.py [OUTPUT_DIR]
"""
import json
import os
import shutil
import subprocess
import sys
from collections import Counter, defaultdict
from pathlib import Path


def _ensure_graphify():
    """Re-exec under a graphify-capable interpreter when this one lacks it.

    graphify is usually installed via `uv tool` / pipx, hidden from the system
    Python, so running `python3 scripts/graphify-obsidian.py` directly often
    cannot import it. Find an interpreter that can import graphify and re-run
    this script under it; this replaces the old .sh/.ps1 wrapper bootstrap.
    """
    try:
        import graphify  # noqa: F401

        return
    except ModuleNotFoundError:
        pass

    if os.environ.get("_GRAPHIFY_OBSIDIAN_BOOTSTRAPPED"):
        print(
            "error: no Python with graphify found. Install it: uv tool install graphifyy",
            file=sys.stderr,
        )
        sys.exit(1)

    candidates: list[str] = []
    marker = Path("graphify-out/.graphify_python")
    if marker.exists():
        recorded = marker.read_text(encoding="utf-8").strip()
        if recorded:
            candidates.append(recorded)
    uv = shutil.which("uv")
    if uv:
        try:
            uv_dir = subprocess.run(
                [uv, "tool", "dir"], capture_output=True, text=True, check=False
            ).stdout.strip()
        except OSError:
            uv_dir = ""
        if uv_dir:
            candidates.append(str(Path(uv_dir) / "graphifyy" / "Scripts" / "python.exe"))
            candidates.append(str(Path(uv_dir) / "graphifyy" / "bin" / "python"))
    for name in ("python3", "python"):
        found = shutil.which(name)
        if found:
            candidates.append(found)

    env = {**os.environ, "_GRAPHIFY_OBSIDIAN_BOOTSTRAPPED": "1"}
    script = str(Path(__file__).resolve())
    for candidate in candidates:
        try:
            probe = subprocess.run(
                [candidate, "-c", "import graphify"], capture_output=True, check=False
            )
        except OSError:
            continue
        if probe.returncode == 0:
            sys.exit(subprocess.run([candidate, script, *sys.argv[1:]], env=env).returncode)

    print(
        "error: no Python with graphify found. Install it: uv tool install graphifyy",
        file=sys.stderr,
    )
    sys.exit(1)


def _default_out():
    repo = Path.cwd()
    return str(repo.parent / (repo.name + "-graphify-vault"))


def main(argv):
    if any(a in ("-h", "--help") for a in argv):
        print(__doc__)
        return 0
    out_dir = argv[0] if argv else _default_out()

    graph_path = Path("graphify-out/graph.json")
    if not graph_path.exists():
        print(
            "error: graphify-out/graph.json not found — build the graph first "
            "(graphify update . or the /graphify skill).",
            file=sys.stderr,
        )
        return 1

    _ensure_graphify()

    from networkx.readwrite import json_graph
    from graphify.cluster import score_all
    from graphify.export import to_canvas, to_obsidian

    G = json_graph.node_link_graph(
        json.loads(graph_path.read_text(encoding="utf-8")), edges="links"
    )

    communities = defaultdict(list)
    for nid, data in G.nodes(data=True):
        communities[int(data.get("community", 0))].append(nid)
    communities = dict(communities)

    sep = chr(92)

    def auto_label(node_ids):
        segs = Counter()
        for nid in node_ids:
            parts = (G.nodes[nid].get("source_file", "") or "").replace(sep, "/").split("/")
            if "shared" in parts:
                i = parts.index("shared")
                segs["/".join(parts[i:i + 3])] += 1
            elif parts and parts[0]:
                segs[parts[0]] += 1
        return segs.most_common(1)[0][0] if segs else None

    labels = {cid: (auto_label(ids) or f"Community {cid}") for cid, ids in communities.items()}
    cohesion = score_all(G, communities)

    # Clean the managed default vault for a reproducible result; never auto-delete
    # a caller-supplied directory.
    out_path = Path(out_dir)
    if out_dir == _default_out() and out_path.exists():
        shutil.rmtree(out_path)

    notes = to_obsidian(G, communities, out_dir, community_labels=labels, cohesion=cohesion)
    to_canvas(G, communities, str(out_path / "graph.canvas"), community_labels=labels)
    print(
        f"Obsidian vault: {notes} notes + graph.canvas in {out_path.resolve()} "
        f"({len(communities)} communities)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
