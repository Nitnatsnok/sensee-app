#!/usr/bin/env python3
"""Prepare a lightweight local agent worktree.

Prints the working directory, Java version, and Gradle wrapper version; makes
``./gradlew`` executable on Unix; writes ``local.properties`` when
``ANDROID_HOME`` or ``ANDROID_SDK_ROOT`` points to an SDK-like directory; and
runs ``gradlew --no-daemon help``. It does not run full ``check``, emulators,
app launches, screenshots, or device checks.
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


def gradlew() -> list[str]:
    return [str(PROJECT_DIR / "gradlew.bat")] if os.name == "nt" else ["./gradlew"]


def write_local_properties() -> None:
    android_sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not android_sdk:
        print(
            "Warning: ANDROID_HOME and ANDROID_SDK_ROOT are not set; "
            "local.properties was not generated.",
            file=sys.stderr,
        )
        return

    sdk_path = Path(android_sdk)
    if not sdk_path.is_dir():
        print(
            "Warning: Android SDK variable is set but the directory does not exist.",
            file=sys.stderr,
        )
        return

    sdk_dir = sdk_path.resolve()
    if not (sdk_dir / "platforms").is_dir() and not (sdk_dir / "cmdline-tools").is_dir():
        print(
            "Warning: Android SDK directory does not contain platforms/ or "
            "cmdline-tools/; local.properties was written anyway.",
            file=sys.stderr,
        )
    # Gradle reads sdk.dir as a properties value, where backslashes are escapes;
    # forward slashes are accepted on every platform.
    (PROJECT_DIR / "local.properties").write_text(
        f"sdk.dir={sdk_dir.as_posix()}\n", encoding="utf-8"
    )
    print("Wrote local.properties for the detected Android SDK.")


def main(argv: list[str]) -> int:
    argparse.ArgumentParser(description="Prepare a lightweight local agent worktree.").parse_args(argv)

    os.chdir(PROJECT_DIR)
    print(f"Project: {PROJECT_DIR}")

    if shutil.which("java"):
        print("Java version:")
        subprocess.run(["java", "-version"], check=False)
    else:
        print("Warning: java was not found on PATH.", file=sys.stderr)

    wrapper_path = PROJECT_DIR / ("gradlew.bat" if os.name == "nt" else "gradlew")
    if not wrapper_path.exists():
        print(
            "Error: Gradle wrapper was not found. Run this script from a Sensee checkout.",
            file=sys.stderr,
        )
        return 1

    if os.name != "nt":
        gradlew_unix = PROJECT_DIR / "gradlew"
        try:
            mode = gradlew_unix.stat().st_mode
            gradlew_unix.chmod(mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
        except OSError:
            print("Warning: could not make ./gradlew executable.", file=sys.stderr)

    print("Gradle wrapper version:")
    subprocess.run(gradlew() + ["--no-daemon", "--version"], check=True)

    write_local_properties()

    print("Running lightweight Gradle warmup:")
    return subprocess.run(gradlew() + ["--no-daemon", "help"], check=False).returncode


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
