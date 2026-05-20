#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/.." && pwd)
git_cmd="${SENSEE_GIT_HOOKS_GIT_BIN:-git}"
lefthook_cmd="${SENSEE_GIT_HOOKS_LEFTHOOK_BIN:-lefthook}"
install_missing_deps="${SENSEE_GIT_HOOKS_INSTALL_MISSING_DEPS:-ask}"

has_command() {
    command -v "$1" >/dev/null 2>&1
}

is_interactive() {
    [ -t 0 ] && [ -t 1 ]
}

prompt_yes_no() {
    question="$1"
    default_answer="${2:-y}"

    case "$install_missing_deps" in
        always)
            return 0
            ;;
        never)
            return 1
            ;;
    esac

    if ! is_interactive; then
        return 1
    fi

    while true; do
        if [ "$default_answer" = 'y' ]; then
            printf '%s [Y/n]: ' "$question"
        else
            printf '%s [y/N]: ' "$question"
        fi

        IFS= read -r answer || return 1

        case "$answer" in
            '')
                [ "$default_answer" = 'y' ] && return 0
                return 1
                ;;
            [Yy]|[Yy][Ee][Ss])
                return 0
                ;;
            [Nn]|[Nn][Oo])
                return 1
                ;;
        esac
    done
}

run_install_command() {
    description="$1"
    shift

    if ! prompt_yes_no "setup-git-hooks: $description. Install it now?" 'y'; then
        printf 'setup-git-hooks: cannot continue without the required dependency.\n' >&2
        return 1
    fi

    "$@"
}

ensure_git_available() {
    if has_command "$git_cmd"; then
        return 0
    fi

    printf 'setup-git-hooks: git executable "%s" was not found.\n' "$git_cmd" >&2
    printf 'setup-git-hooks: install Git manually and rerun this script.\n' >&2
    return 1
}

ensure_lefthook_available() {
    if has_command "$lefthook_cmd"; then
        return 0
    fi

    printf 'setup-git-hooks: lefthook executable "%s" was not found.\n' "$lefthook_cmd" >&2

    if has_command brew; then
        run_install_command 'Lefthook is required to wire the tracked git hooks' brew install lefthook || return 1
    elif has_command winget; then
        run_install_command 'Lefthook is required to wire the tracked git hooks' winget install --id evilmartians.lefthook -e --source winget || return 1
    elif has_command winget.exe; then
        run_install_command 'Lefthook is required to wire the tracked git hooks' winget.exe install --id evilmartians.lefthook -e --source winget || return 1
    elif has_command choco; then
        run_install_command 'Lefthook is required to wire the tracked git hooks' choco install lefthook -y || return 1
    elif has_command choco.exe; then
        run_install_command 'Lefthook is required to wire the tracked git hooks' choco.exe install lefthook -y || return 1
    elif has_command scoop; then
        run_install_command 'Lefthook is required to wire the tracked git hooks' scoop install lefthook || return 1
    elif has_command npm; then
        run_install_command 'Lefthook is required to wire the tracked git hooks' npm install --global lefthook || return 1
    elif has_command pnpm; then
        run_install_command 'Lefthook is required to wire the tracked git hooks' pnpm add --global lefthook || return 1
    elif has_command bun; then
        run_install_command 'Lefthook is required to wire the tracked git hooks' bun add --global lefthook || return 1
    elif has_command go; then
        run_install_command 'Lefthook is required to wire the tracked git hooks' go install github.com/evilmartians/lefthook@latest || return 1
    else
        printf 'setup-git-hooks: no supported package manager was detected for automatic Lefthook installation.\n' >&2
        printf 'setup-git-hooks: install Lefthook manually and rerun this script.\n' >&2
        return 1
    fi

    if ! has_command "$lefthook_cmd"; then
        printf 'setup-git-hooks: lefthook still is not available after installation attempt.\n' >&2
        printf 'setup-git-hooks: if the installer updated PATH, reopen the shell and rerun this script.\n' >&2
        return 1
    fi
}

unset_local_hooks_path_if_needed() {
    current_hooks_path=$($git_cmd config --local --get core.hooksPath 2>/dev/null || true)

    if [ -z "$current_hooks_path" ]; then
        return 0
    fi

    printf 'setup-git-hooks: removing local core.hooksPath=%s so Git uses Lefthook-managed .git/hooks.\n' "$current_hooks_path"
    $git_cmd config --local --unset core.hooksPath
}

cd "$repo_root"

ensure_git_available
ensure_lefthook_available

git_dir=$($git_cmd rev-parse --git-dir 2>/dev/null) || {
    printf 'setup-git-hooks: this script must be run inside the repository worktree.\n' >&2
    exit 1
}

if [ ! -f "lefthook.yml" ]; then
    printf 'setup-git-hooks: expected lefthook.yml to exist in %s.\n' "$repo_root" >&2
    exit 1
fi

if [ ! -f ".githooks/pre-commit" ] || [ ! -f ".githooks/pre-push" ]; then
    printf 'setup-git-hooks: expected tracked hook scripts under .githooks/.\n' >&2
    exit 1
fi

unset_local_hooks_path_if_needed

chmod +x .githooks/pre-commit .githooks/pre-push
"$lefthook_cmd" install

printf 'setup-git-hooks: git dir is %s\n' "$git_dir"
printf 'setup-git-hooks: lefthook has been installed into .git/hooks.\n'
printf 'setup-git-hooks: hooks are ready.\n'

