#!/usr/bin/env python3
"""Validate Sensee agent-facing instruction files.

This script is intentionally read-only and uses only the Python standard
library so agents can run it before changing or finalizing instruction work.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from dataclasses import dataclass
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SKILLS_DIR = REPO_ROOT / ".agents" / "skills"
SKILL_NAME_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
EVAL_ID_RE = re.compile(r"^[a-z0-9-]+$")
ERROR = "ERROR"
WARNING = "WARNING"
EXCLUDED_DIR_NAMES = {
    ".claude",
    ".git",
    ".gradle",
    ".idea",
    ".kotlin",
    "__pycache__",
    "build",
    "kotlin-js-store",
    "node_modules",
}


@dataclass(frozen=True)
class Finding:
    path: Path
    message: str
    severity: str = ERROR

    def format(self) -> str:
        return f"{self.severity} {self.path.relative_to(REPO_ROOT)}: {self.message}"


@dataclass(frozen=True)
class ValidationReport:
    findings: list[Finding]
    checked_skill_count: int
    checked_eval_file_count: int

    @property
    def error_count(self) -> int:
        return sum(1 for finding in self.findings if finding.severity == ERROR)

    @property
    def warning_count(self) -> int:
        return sum(1 for finding in self.findings if finding.severity == WARNING)


def parse_frontmatter(path: Path) -> dict[str, str]:
    lines = path.read_text(encoding="utf-8").splitlines()
    if not lines or lines[0].strip() != "---":
        return {}

    data: dict[str, str] = {}
    for line in lines[1:]:
        if line.strip() == "---":
            return data
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        key, separator, value = line.partition(":")
        if not separator:
            continue
        data[key.strip()] = value.strip()
    return {}


def is_excluded_path(path: Path) -> bool:
    try:
        parts = path.relative_to(REPO_ROOT).parts
    except ValueError:
        parts = path.parts
    return any(part in EXCLUDED_DIR_NAMES for part in parts)


def iter_named_repo_files(file_names: set[str]) -> list[Path]:
    files: list[Path] = []
    for current_root, dir_names, names in os.walk(REPO_ROOT):
        dir_names[:] = sorted(name for name in dir_names if name not in EXCLUDED_DIR_NAMES)
        current_path = Path(current_root)
        for file_name in sorted(names):
            if file_name in file_names:
                path = current_path / file_name
                if not is_excluded_path(path):
                    files.append(path)
    return sorted(files)


def project_skill_dirs() -> list[Path]:
    if not SKILLS_DIR.exists():
        return []
    return sorted(path for path in SKILLS_DIR.iterdir() if path.is_dir())


def validate_skills(skill_dirs: list[Path]) -> list[Finding]:
    findings: list[Finding] = []
    if not SKILLS_DIR.exists():
        findings.append(Finding(SKILLS_DIR, "missing .agents/skills directory"))
        return findings

    for skill_dir in skill_dirs:
        skill_md = skill_dir / "SKILL.md"
        if not skill_md.exists():
            findings.append(Finding(skill_dir, "skill directory is missing SKILL.md"))
            continue

        frontmatter = parse_frontmatter(skill_md)
        name = frontmatter.get("name", "")
        description = frontmatter.get("description", "")
        if not name:
            findings.append(Finding(skill_md, "frontmatter is missing required name"))
        elif name != skill_dir.name:
            findings.append(Finding(skill_md, f"name '{name}' does not match folder '{skill_dir.name}'"))
        elif not SKILL_NAME_RE.fullmatch(name):
            findings.append(Finding(skill_md, "name must use lowercase letters, numbers, and hyphens"))

        if not description:
            findings.append(Finding(skill_md, "frontmatter is missing required description"))
        elif len(description) > 1024:
            findings.append(Finding(skill_md, "description exceeds 1024 characters"))

    return findings


def validate_eval_file(path: Path, expected_skill_name: str) -> list[Finding]:
    findings: list[Finding] = []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        findings.append(
            Finding(path, f"invalid JSON: {error.msg} at line {error.lineno} column {error.colno}"),
        )
        return findings
    except OSError as error:
        findings.append(Finding(path, f"could not read file: {error}"))
        return findings

    if not isinstance(data, dict):
        findings.append(Finding(path, "top-level value must be an object"))
        return findings

    skill_name = data.get("skill_name")
    if "skill_name" not in data:
        findings.append(Finding(path, "missing required skill_name"))
    elif not isinstance(skill_name, str) or not skill_name.strip():
        findings.append(Finding(path, "skill_name must be a non-empty string"))
    elif skill_name != expected_skill_name:
        findings.append(Finding(path, f"skill_name '{skill_name}' does not match folder '{expected_skill_name}'"))

    evals = data.get("evals")
    if "evals" not in data:
        findings.append(Finding(path, "missing required evals"))
        return findings
    if not isinstance(evals, list) or not evals:
        findings.append(Finding(path, "evals must be a non-empty array"))
        return findings

    seen_ids: set[str] = set()
    required_fields = ("id", "prompt", "expected_output")
    for index, item in enumerate(evals):
        if not isinstance(item, dict):
            findings.append(Finding(path, f"evals[{index}] must be an object"))
            continue

        for field in required_fields:
            value = item.get(field)
            if not isinstance(value, str) or not value.strip():
                findings.append(Finding(path, f"evals[{index}].{field} must be a non-empty string"))
                continue

            if field == "id":
                if not EVAL_ID_RE.fullmatch(value):
                    findings.append(Finding(path, f"evals[{index}].id must match {EVAL_ID_RE.pattern}"))
                if value in seen_ids:
                    findings.append(Finding(path, f"evals[{index}].id duplicates '{value}'"))
                seen_ids.add(value)

    return findings


def validate_eval_json(skill_dirs: list[Path]) -> tuple[list[Finding], int]:
    findings: list[Finding] = []
    checked_eval_file_count = 0

    for skill_dir in skill_dirs:
        evals_dir = skill_dir / "evals"
        if not evals_dir.exists():
            continue
        if not evals_dir.is_dir():
            findings.append(Finding(evals_dir, "evals path exists but is not a directory"))
            continue

        eval_files = sorted(evals_dir.glob("*.json"))
        if not eval_files:
            findings.append(
                Finding(evals_dir, "evals directory exists but contains no .json files", WARNING),
            )
            continue

        for eval_file in eval_files:
            checked_eval_file_count += 1
            findings.extend(validate_eval_file(eval_file, skill_dir.name))

    return findings, checked_eval_file_count


def validate_commit_links() -> list[Finding]:
    findings: list[Finding] = []
    root_agents = REPO_ROOT / "AGENTS.md"
    commit_skill = SKILLS_DIR / "commit-preparation" / "SKILL.md"

    for path in (root_agents, commit_skill):
        text = path.read_text(encoding="utf-8", errors="ignore") if path.exists() else ""
        if "docs/engineering/commits.md" not in text:
            findings.append(Finding(path, "must reference docs/engineering/commits.md"))

    return findings


def validate_claude_shims() -> list[Finding]:
    findings: list[Finding] = []
    claude_files = iter_named_repo_files({"CLAUDE.md"})
    if not claude_files:
        return findings

    for path in claude_files:
        content = path.read_text(encoding="utf-8").strip()
        if content != "@AGENTS.md":
            findings.append(Finding(path, "CLAUDE.md must stay a thin @AGENTS.md shim"))

    return findings


def validate_agents_claude_pairing() -> list[Finding]:
    findings: list[Finding] = []
    agents_files = iter_named_repo_files({"AGENTS.md"})
    claude_files = iter_named_repo_files({"CLAUDE.md"})
    agents_dirs = {path.parent for path in agents_files}
    claude_dirs = {path.parent for path in claude_files}

    for path in agents_files:
        if path.parent not in claude_dirs:
            findings.append(Finding(path, "AGENTS.md is missing its sibling CLAUDE.md shim"))

    for path in claude_files:
        if path.parent not in agents_dirs:
            findings.append(Finding(path, "CLAUDE.md is missing its sibling AGENTS.md"))

    return findings


def run_validation() -> ValidationReport:
    skill_dirs = project_skill_dirs()
    findings: list[Finding] = []
    findings.extend(validate_skills(skill_dirs))
    eval_findings, checked_eval_file_count = validate_eval_json(skill_dirs)
    findings.extend(eval_findings)
    findings.extend(validate_commit_links())
    findings.extend(validate_claude_shims())
    findings.extend(validate_agents_claude_pairing())
    return ValidationReport(
        findings=findings,
        checked_skill_count=len(skill_dirs),
        checked_eval_file_count=checked_eval_file_count,
    )


def print_summary(report: ValidationReport) -> None:
    print("Summary:")
    print(f"- errors: {report.error_count}")
    print(f"- warnings: {report.warning_count}")
    print(f"- checked skills: {report.checked_skill_count}")
    print(f"- checked eval files: {report.checked_eval_file_count}")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Validate Sensee AGENTS.md, CLAUDE.md, Agent Skills, and related instruction files.",
    )
    parser.parse_args()

    report = run_validation()
    errors = [finding for finding in report.findings if finding.severity == ERROR]
    warnings = [finding for finding in report.findings if finding.severity == WARNING]

    if errors:
        print("Agent instruction validation failed:")
        for finding in errors + warnings:
            print(finding.format())
        print_summary(report)
        return 1

    if warnings:
        print("Agent instruction validation passed with warnings:")
        for finding in warnings:
            print(finding.format())
        print_summary(report)
        return 0

    print("Agent instruction validation passed.")
    print_summary(report)
    return 0


if __name__ == "__main__":
    sys.exit(main())
