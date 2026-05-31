Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ProjectDir = Resolve-Path (Join-Path $PSScriptRoot "..\..")
& (Join-Path $ProjectDir "scripts\agents\setup-local-env.ps1") @args
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
