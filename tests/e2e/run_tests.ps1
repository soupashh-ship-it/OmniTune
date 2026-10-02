# OmniTune E2E Test Suite Execution Wrapper (PowerShell)
# Usage:
#   .\tests\e2e\run_tests.ps1
#   .\tests\e2e\run_tests.ps1 -Tier 1
#   .\tests\e2e\run_tests.ps1 -Tier 2
#   .\tests\e2e\run_tests.ps1 -Tier 3
#   .\tests\e2e\run_tests.ps1 -Tier 4

param (
    [int]$Tier = 0
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RunnerScript = Join-Path $ScriptDir "harness\runner.js"

if (-not (Test-Path $RunnerScript)) {
    Write-Error "Runner script not found at $RunnerScript"
    exit 1
}

$Arguments = @($RunnerScript)
if ($Tier -ge 1 -and $Tier -le 4) {
    $Arguments += "--tier"
    $Arguments += "$Tier"
}

Write-Host "Launching OmniTune E2E Test Suite via Node.js..." -ForegroundColor Cyan
& node @Arguments

if ($LASTEXITCODE -ne 0) {
    Write-Host "OmniTune E2E Test Suite failed with exit code $LASTEXITCODE." -ForegroundColor Red
    exit $LASTEXITCODE
} else {
    Write-Host "OmniTune E2E Test Suite passed successfully." -ForegroundColor Green
    exit 0
}
