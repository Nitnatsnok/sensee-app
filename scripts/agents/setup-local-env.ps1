param(
    [switch]$Help
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Show-Help {
    @"
Usage: pwsh -File scripts/agents/setup-local-env.ps1

Prepares a lightweight local agent worktree:
- prints the working directory, Java version, and Gradle wrapper version;
- writes local.properties when ANDROID_HOME or ANDROID_SDK_ROOT points to an SDK;
- runs .\gradlew.bat --no-daemon help.
"@
}

function Invoke-Native {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [string[]]$Arguments = @()
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$FilePath exited with code $LASTEXITCODE."
    }
}

if ($Help) {
    Show-Help
    exit 0
}

$ProjectDir = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $ProjectDir

Write-Host "Project: $(Get-Location)"

if (Get-Command java -ErrorAction SilentlyContinue) {
    Write-Host "Java version:"
    & java -version
    if ($LASTEXITCODE -ne 0) {
        throw "java -version exited with code $LASTEXITCODE."
    }
} else {
    Write-Warning "java was not found on PATH."
}

if (-not (Test-Path -LiteralPath ".\gradlew.bat" -PathType Leaf)) {
    throw ".\gradlew.bat was not found. Run this script from a Sensee checkout."
}

Write-Host "Gradle wrapper version:"
Invoke-Native ".\gradlew.bat" @("--no-daemon", "--version")

$androidSdk = $null
if ($env:ANDROID_HOME) {
    $androidSdk = $env:ANDROID_HOME
} elseif ($env:ANDROID_SDK_ROOT) {
    $androidSdk = $env:ANDROID_SDK_ROOT
}

if ($androidSdk) {
    if (Test-Path -LiteralPath $androidSdk -PathType Container) {
        $sdkDir = (Resolve-Path -LiteralPath $androidSdk).Path.Replace("\", "/")
        Set-Content -LiteralPath "local.properties" -Value "sdk.dir=$sdkDir" -Encoding UTF8
        Write-Host "Wrote local.properties for the detected Android SDK."
    } else {
        Write-Warning "Android SDK variable is set but the directory does not exist."
    }
} else {
    Write-Warning "ANDROID_HOME and ANDROID_SDK_ROOT are not set; local.properties was not generated."
}

Write-Host "Running lightweight Gradle warmup:"
Invoke-Native ".\gradlew.bat" @("--no-daemon", "help")
