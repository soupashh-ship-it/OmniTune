# Usage: .\scripts\gradle-bg.ps1 -ArgsList ":app:compileDebugKotlin","--console=plain"
# Launches Gradle detached, writes output to build\gradle-bg.log, returns immediately.
# Poll completion by checking for EXITCODE= line in build\gradle-bg.log.
param(
    [Parameter(Mandatory = $true)]
    [string[]]$ArgsList,
    [string]$LogName = "gradle-bg"
)

$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root "build"
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Force -Path $logDir | Out-Null }
$log = Join-Path $logDir "$LogName.log"
Remove-Item $log -Force -ErrorAction SilentlyContinue

$argLine = ($ArgsList | ForEach-Object { '"' + $_ + '"' }) -join ' '
$cmd = "cd /d `"$root`" && gradlew.bat $argLine >> `"$log`" 2>&1 & echo EXITCODE=%ERRORLEVEL% >> `"$log`""
Start-Process -FilePath "cmd.exe" -ArgumentList "/c", $cmd -WindowStyle Hidden
Write-Output "LAUNCHED -> $log"
