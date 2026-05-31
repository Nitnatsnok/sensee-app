#!/usr/bin/env sh
set -eu

show_help() {
    cat <<'EOF'
Usage: sh scripts/agents/setup-local-env.sh

Prepares a lightweight local agent worktree:
- prints the working directory, Java version, and Gradle wrapper version;
- makes ./gradlew executable on Unix;
- writes local.properties when ANDROID_HOME or ANDROID_SDK_ROOT points to an SDK-like directory;
- runs ./gradlew --no-daemon help.
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

echo "Project: $(pwd)"

if command -v java >/dev/null 2>&1; then
    echo "Java version:"
    java -version
else
    echo "Warning: java was not found on PATH." >&2
fi

if [ ! -f "./gradlew" ]; then
    echo "Error: ./gradlew was not found. Run this script from a Sensee checkout." >&2
    exit 1
fi

chmod +x ./gradlew || echo "Warning: could not make ./gradlew executable." >&2

echo "Gradle wrapper version:"
./gradlew --no-daemon --version

android_sdk="${ANDROID_HOME:-}"
if [ -z "$android_sdk" ]; then
    android_sdk="${ANDROID_SDK_ROOT:-}"
fi

if [ -n "$android_sdk" ]; then
    if [ -d "$android_sdk" ]; then
        sdk_dir=$(CDPATH= cd -- "$android_sdk" && pwd)
        if [ ! -d "$sdk_dir/platforms" ] && [ ! -d "$sdk_dir/cmdline-tools" ]; then
            echo "Warning: Android SDK directory does not contain platforms/ or cmdline-tools/; local.properties was written anyway." >&2
        fi
        printf 'sdk.dir=%s\n' "$sdk_dir" > local.properties
        echo "Wrote local.properties for the detected Android SDK."
    else
        echo "Warning: Android SDK variable is set but the directory does not exist." >&2
    fi
else
    echo "Warning: ANDROID_HOME and ANDROID_SDK_ROOT are not set; local.properties was not generated." >&2
fi

echo "Running lightweight Gradle warmup:"
./gradlew --no-daemon help
