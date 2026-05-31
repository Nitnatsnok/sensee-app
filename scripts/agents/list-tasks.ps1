param(
    [switch]$Help
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($Help) {
    @"
Usage: pwsh -File scripts/agents/list-tasks.ps1

Lists all Gradle tasks through the repository wrapper.
"@
    exit 0
}

$ProjectDir = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $ProjectDir

& ".\gradlew.bat" "--no-daemon" "tasks" "--all"
if ($LASTEXITCODE -ne 0) {
    throw ".\gradlew.bat exited with code $LASTEXITCODE."
}
