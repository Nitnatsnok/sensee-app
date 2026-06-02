#!/usr/bin/env python3
"""Common validation entrypoints for local agent work.

Modes:
  fast          Run lightweight Gradle help and agent-instruction validation.
  full          Run the full root Gradle `check` task.
  docs          Run agent-instruction validation.
  architecture  Run the Gradle `konsistCheck` task.
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parents[2]
VALIDATOR = PROJECT_DIR / "scripts" / "agents" / "validate-agent-instructions.py"


def gradlew() -> list[str]:
    return [str(PROJECT_DIR / "gradlew.bat")] if os.name == "nt" else ["./gradlew"]


def run_gradle(*args: str) -> None:
    result = subprocess.run(gradlew() + ["--no-daemon", *args], check=False)
    if result.returncode != 0:
        raise SystemExit(result.returncode)


def run_agent_instruction_validator() -> None:
    if not VALIDATOR.is_file():
        print(
            "Skipping agent-instruction validation: validator script is missing.",
            file=sys.stderr,
        )
        return
    interpreter = os.environ.get("PYTHON") or sys.executable
    result = subprocess.run([interpreter, str(VALIDATOR)], check=False)
    if result.returncode != 0:
        raise SystemExit(result.returncode)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Run a validation mode.")
    parser.add_argument(
        "mode",
        nargs="?",
        default="fast",
        choices=["fast", "full", "docs", "architecture"],
        help="Validation mode to run (default: fast).",
    )
    args = parser.parse_args(argv)

    os.chdir(PROJECT_DIR)

    if args.mode == "fast":
        run_gradle("help")
        run_agent_instruction_validator()
    elif args.mode == "full":
        run_gradle("check")
    elif args.mode == "docs":
        run_agent_instruction_validator()
    elif args.mode == "architecture":
        run_gradle("konsistCheck")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
