#!/usr/bin/env python3
"""Create a local, git-ignored link so Claude Code discovers project skills.

Links ``.claude/skills`` to the canonical ``.agents/skills`` without copying.
On Windows a directory junction is used (no admin rights or Developer Mode
required); on Unix a relative symlink. The canonical skills stay in
``.agents/skills``; this link is per-machine only.
"""
from __future__ import annotations

import argparse
import os
import shutil
import stat
import subprocess
import sys
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parents[2]
LINK = PROJECT_DIR / ".claude" / "skills"
TARGET = PROJECT_DIR / ".agents" / "skills"


def is_link(path: Path) -> bool:
    """Report whether ``path`` is a symlink or a Windows junction reparse point."""
    if not os.path.lexists(path):
        return False
    if os.name == "nt":
        try:
            attrs = os.lstat(path).st_file_attributes
            return bool(attrs & stat.FILE_ATTRIBUTE_REPARSE_POINT)
        except (OSError, AttributeError):
            return path.is_symlink()
    return path.is_symlink()


def remove_link(path: Path) -> None:
    if os.name == "nt":
        # rmdir removes the junction reparse point itself, never the target.
        os.rmdir(path)
    else:
        os.unlink(path)


def looks_like_skills_copy(path: Path) -> bool:
    """Report whether a plain ``.claude/skills`` directory is a harness-made
    copy of the canonical skills, safe to replace with a link.

    Claude Code's worktree seeding dereferences the link and copies the skill
    tree as a plain directory, so a fresh session cannot re-link over it. A
    directory whose entries are all skill folders (each holding ``SKILL.md``),
    or an empty directory, is such a copy: the canonical tree still lives in
    ``.agents/skills``, so dropping this duplicate loses nothing. Any other
    content is treated as user data and left untouched.
    """
    if not path.is_dir():
        return False
    try:
        entries = list(path.iterdir())
    except OSError:
        return False
    return all(entry.is_dir() and (entry / "SKILL.md").is_file() for entry in entries)


def create_link() -> None:
    LINK.parent.mkdir(parents=True, exist_ok=True)
    if os.name == "nt":
        # ln -s deep-copies a directory under MSYS, and os.symlink needs admin
        # rights, so create a directory junction instead.
        subprocess.run(
            ["cmd", "/c", "mklink", "/J", str(LINK), str(TARGET)],
            check=True,
            capture_output=True,
            text=True,
        )
    else:
        os.symlink(os.path.relpath(TARGET, LINK.parent), LINK)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(
        description="Link .claude/skills to the canonical .agents/skills.",
    )
    parser.add_argument(
        "--remove",
        action="store_true",
        help="Remove the .claude/skills link if it exists.",
    )
    args = parser.parse_args(argv)

    if args.remove:
        if is_link(LINK):
            remove_link(LINK)
            print(f"Removed {LINK}")
        else:
            print(f"No link at {LINK}; nothing to remove.")
        return 0

    if not TARGET.is_dir():
        print(f"Canonical skills directory not found: {TARGET}", file=sys.stderr)
        return 1

    if is_link(LINK):
        remove_link(LINK)
    elif LINK.exists():
        if looks_like_skills_copy(LINK):
            # Harness-copied skill tree (or empty dir): the canonical skills
            # stay in .agents/skills, so replacing this duplicate with a link
            # loses nothing and lets a worktree repoint at its own skills.
            shutil.rmtree(LINK)
            print(f"Replaced harness-copied skills directory at {LINK}.")
        else:
            print(f"{LINK} exists and is not a link; refusing to overwrite.", file=sys.stderr)
            print("Move or delete it manually if you want the skills link.", file=sys.stderr)
            return 1

    create_link()
    print(f"Linked {LINK} -> {TARGET}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
