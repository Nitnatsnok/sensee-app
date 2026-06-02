#!/usr/bin/env python3
"""List all Gradle tasks through the repository wrapper."""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parents[2]


def gradlew() -> list[str]:
    return [str(PROJECT_DIR / "gradlew.bat")] if os.name == "nt" else ["./gradlew"]


def main(argv: list[str]) -> int:
    argparse.ArgumentParser(description="List all Gradle tasks.").parse_args(argv)
    os.chdir(PROJECT_DIR)
    return subprocess.run(gradlew() + ["--no-daemon", "tasks", "--all"], check=False).returncode


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
