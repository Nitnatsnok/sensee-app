param(
    [ValidateSet("fast", "code", "docs", "architecture")]
    [string]$Mode = "fast",
    [switch]$Help
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$script:GradleTasksOutput = $null

function Show-Help {
    @"
Usage: pwsh -File scripts/agents/validate.ps1 [-Mode fast|code|docs|architecture]

Modes:
  fast          Run lightweight Gradle help and agent-instruction validation.
  code          Run the best available aggregate code validation task.
  docs          Run agent-instruction validation and optional docs aggregate task.
  architecture  Run the best available architecture validation task.
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

function Invoke-AgentInstructionValidator {
    if (-not (Test-Path -LiteralPath "scripts\agents\validate-agent-instructions.py" -PathType Leaf)) {
        Write-Warning "Skipping agent-instruction validation: validator script is missing."
        return
    }

    $candidates = @()
    if ($env:PYTHON) {
        $candidates += @{ Command = $env:PYTHON; Arguments = @("scripts/agents/validate-agent-instructions.py") }
    }
    $candidates += @(
        @{ Command = "py"; Arguments = @("-3", "scripts/agents/validate-agent-instructions.py") },
        @{ Command = "python3"; Arguments = @("scripts/agents/validate-agent-instructions.py") },
        @{ Command = "python"; Arguments = @("scripts/agents/validate-agent-instructions.py") }
    )

    foreach ($candidate in $candidates) {
        if (-not (Get-Command $candidate.Command -ErrorAction SilentlyContinue)) {
            continue
        }

        try {
            $arguments = [string[]]$candidate.Arguments
            & $candidate.Command @arguments
        } catch {
            Write-Warning "Could not start $($candidate.Command); trying the next Python candidate."
            continue
        }

        if ($LASTEXITCODE -eq 0) {
            return
        }

        throw "$($candidate.Command) exited with code $LASTEXITCODE."
    }

    Write-Warning "Skipping agent-instruction validation: py/python3/python was not available."
}

function Initialize-GradleTasks {
    if ($null -eq $script:GradleTasksOutput) {
        $script:GradleTasksOutput = & ".\gradlew.bat" "--no-daemon" "tasks" "--all"
        if ($LASTEXITCODE -ne 0) {
            throw ".\gradlew.bat tasks --all exited with code $LASTEXITCODE."
        }
    }
}

function Test-GradleTask {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    Initialize-GradleTasks
    $pattern = "^$([regex]::Escape($Name))(\s|-)"
    return (($script:GradleTasksOutput | Select-String -Pattern $pattern -Quiet) -eq $true)
}

function Invoke-GradleTaskIfExists {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    if (Test-GradleTask $Name) {
        Invoke-Native ".\gradlew.bat" @("--no-daemon", $Name)
    } else {
        Write-Host "Skipping optional Gradle task '$Name': task was not found."
    }
}

function Invoke-BestCodeValidation {
    if (Test-GradleTask "verify") {
        Invoke-Native ".\gradlew.bat" @("--no-daemon", "verify")
    } elseif (Test-GradleTask "check") {
        Invoke-Native ".\gradlew.bat" @("--no-daemon", "check")
    } else {
        Write-Host "No aggregate code validation task found. Run the smallest affected module check manually."
    }
}

function Invoke-BestArchitectureValidation {
    if (Test-GradleTask "verifyArchitecture") {
        Invoke-Native ".\gradlew.bat" @("--no-daemon", "verifyArchitecture")
    } elseif (Test-GradleTask "konsistCheck") {
        Invoke-Native ".\gradlew.bat" @("--no-daemon", "konsistCheck")
    } else {
        Write-Host "No aggregate architecture validation task found."
        Write-Host "For LikeC4 changes, run: npx likec4 validate docs/c4"
    }
}

if ($Help) {
    Show-Help
    exit 0
}

$ProjectDir = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $ProjectDir

switch ($Mode) {
    "fast" {
        Invoke-Native ".\gradlew.bat" @("--no-daemon", "help")
        Invoke-AgentInstructionValidator
    }
    "code" {
        Invoke-BestCodeValidation
    }
    "docs" {
        Invoke-AgentInstructionValidator
        Invoke-GradleTaskIfExists "verifyDocs"
    }
    "architecture" {
        Invoke-BestArchitectureValidation
    }
}
