#!/usr/bin/env sh
set -eu

show_help() {
    cat <<'EOF'
Usage: sh scripts/agents/list-tasks.sh

Lists all Gradle tasks through the repository wrapper.
EOF
}

case "${1:-}" in
    -h|--help)
        show_help
        exit 0
        ;;
    "")
        ;;
    *)
        echo "Unknown argument: $1" >&2
        show_help >&2
        exit 2
        ;;
esac

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$PROJECT_DIR"

./gradlew --no-daemon tasks --all
