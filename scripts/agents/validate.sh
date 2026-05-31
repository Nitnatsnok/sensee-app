#!/usr/bin/env sh
set -eu

show_help() {
    cat <<'EOF'
Usage: sh scripts/agents/validate.sh [fast|code|docs|architecture]

Modes:
  fast          Run lightweight Gradle help and agent-instruction validation.
  code          Run the best available aggregate code validation task.
  docs          Run agent-instruction validation and optional docs aggregate task.
  architecture  Run the best available architecture validation task.
EOF
}

case "${1:-fast}" in
    -h|--help)
        show_help
        exit 0
        ;;
    fast|code|docs|architecture)
        mode="${1:-fast}"
        ;;
    *)
        echo "Unknown validation mode: $1" >&2
        show_help >&2
        exit 2
        ;;
esac

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$PROJECT_DIR"

GRADLE="./gradlew"
TASKS_FILE=""

cleanup() {
    if [ -n "$TASKS_FILE" ] && [ -f "$TASKS_FILE" ]; then
        rm -f "$TASKS_FILE"
    fi
}
trap cleanup EXIT HUP INT TERM

run_agent_instruction_validator() {
    if [ ! -f "scripts/agents/validate-agent-instructions.py" ]; then
        echo "Skipping agent-instruction validation: validator script is missing." >&2
        return 0
    fi

    if [ -n "${PYTHON:-}" ]; then
        "$PYTHON" scripts/agents/validate-agent-instructions.py
    elif command -v python3 >/dev/null 2>&1; then
        python3 scripts/agents/validate-agent-instructions.py
    elif command -v python >/dev/null 2>&1; then
        python scripts/agents/validate-agent-instructions.py
    else
        echo "Skipping agent-instruction validation: python3/python was not found." >&2
    fi
}

load_gradle_tasks() {
    if [ -z "$TASKS_FILE" ]; then
        TASKS_FILE=$(mktemp "${TMPDIR:-/tmp}/sensee-gradle-tasks.XXXXXX")
        "$GRADLE" --no-daemon tasks --all > "$TASKS_FILE"
    fi
}

gradle_task_exists() {
    task_name="$1"
    load_gradle_tasks
    grep -Eq "^${task_name}([[:space:]]|-)" "$TASKS_FILE"
}

run_gradle_task_if_exists() {
    task_name="$1"
    if gradle_task_exists "$task_name"; then
        "$GRADLE" --no-daemon "$task_name"
    else
        echo "Skipping optional Gradle task '$task_name': task was not found."
    fi
}

run_best_code_validation() {
    if gradle_task_exists "verify"; then
        "$GRADLE" --no-daemon verify
    elif gradle_task_exists "check"; then
        "$GRADLE" --no-daemon check
    else
        echo "No aggregate code validation task found. Run the smallest affected module check manually."
    fi
}

run_best_architecture_validation() {
    if gradle_task_exists "verifyArchitecture"; then
        "$GRADLE" --no-daemon verifyArchitecture
    elif gradle_task_exists "konsistCheck"; then
        "$GRADLE" --no-daemon konsistCheck
    else
        echo "No aggregate architecture validation task found."
        echo "For LikeC4 changes, run: npx likec4 validate docs/c4"
    fi
}

case "$mode" in
    fast)
        "$GRADLE" --no-daemon help
        run_agent_instruction_validator
        ;;
    code)
        run_best_code_validation
        ;;
    docs)
        run_agent_instruction_validator
        run_gradle_task_if_exists "verifyDocs"
        ;;
    architecture)
        run_best_architecture_validation
        ;;
esac
