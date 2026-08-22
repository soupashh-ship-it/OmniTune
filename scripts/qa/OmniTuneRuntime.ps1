<#
.SYNOPSIS
    Safe ADB helpers for the disposable OmniTune debug profile.

.DESCRIPTION
    This script is intentionally hard-coded to com.omnitune.app.debug. It never targets the
    release application ID and refuses to clear data unless -ConfirmResetDebugProfile is supplied.
    Generated evidence is written to .qa-runtime/, which is ignored by git.

.EXAMPLE
    .\scripts\qa\OmniTuneRuntime.ps1 -Action Build
    .\scripts\qa\OmniTuneRuntime.ps1 -Action Install -Serial emulator-5554
    .\scripts\qa\OmniTuneRuntime.ps1 -Action ResetDebugProfile -ConfirmResetDebugProfile
    .\scripts\qa\OmniTuneRuntime.ps1 -Action StartLogcat
#>
[CmdletBinding()]
param(
    [ValidateSet("Status", "Build", "BuildTestApk", "Install", "InstallTestApk", "RunFixture", "SeedDataset", "ResetDebugProfile", "Launch", "ForceStop", "ProcessDeath", "StartLogcat", "StopLogcat", "Screenshot", "PullBackup", "SetNetwork")]
    [string]$Action = "Status",
    [string]$Serial,
    [string]$AdbPath,
    [string]$ApkPath = (Join-Path $PSScriptRoot "..\..\app\build\outputs\apk\debug\app-debug.apk"),
    [string]$TestApkPath = (Join-Path $PSScriptRoot "..\..\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"),
    [string]$ArtifactRoot = (Join-Path $PSScriptRoot "..\..\.qa-runtime"),
    [ValidateSet("Unchanged", "Offline", "Online")]
    [string]$NetworkMode = "Unchanged",
    [switch]$ConfirmResetDebugProfile,
    [switch]$AllowNetworkMutation,
    [string]$RemoteBackupPath,
    [ValidateSet("PlaybackPreferences", "SearchViewModel", "MediaSessionCommands", "StreamResolver", "LibraryPlaylists", "OfflineCache", "BackupRepository")]
    [string]$Fixture
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$DebugPackage = "com.omnitune.app.debug"
$DebugTestPackage = "$DebugPackage.test"
# applicationIdSuffix changes the installed package only; the activity class remains
# in the production namespace.  Use the fully-qualified class so ADB resolves it.
$DebugActivity = "$DebugPackage/com.omnitune.app.MainActivity"

function Resolve-Adb {
    if ($AdbPath) {
        if (-not (Test-Path -LiteralPath $AdbPath)) { throw "ADB was not found at '$AdbPath'." }
        return (Resolve-Path -LiteralPath $AdbPath).Path
    }
    $command = Get-Command adb -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    if ($env:ANDROID_SDK_ROOT) {
        $sdkAdb = Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"
        if (Test-Path -LiteralPath $sdkAdb) { return $sdkAdb }
    }
    throw "ADB was not found. Put platform-tools on PATH or pass -AdbPath."
}

function Get-ConnectedSerial {
    param([string]$ResolvedAdb)
    if ($Serial) { return $Serial }
    $devices = & $ResolvedAdb devices | Select-Object -Skip 1 | ForEach-Object {
        $parts = $_ -split "\s+"
        if ($parts.Count -ge 2 -and $parts[1] -eq "device") { $parts[0] }
    } | Where-Object { $_ }
    if ($devices.Count -ne 1) {
        throw "Expected exactly one connected ADB device. Pass -Serial explicitly when more than one is connected."
    }
    return $devices[0]
}

function Invoke-Adb {
    param([string[]]$Arguments)
    $result = & $script:ResolvedAdb -s $script:ResolvedSerial @Arguments
    if ($LASTEXITCODE -ne 0) { throw "ADB command failed: adb -s $script:ResolvedSerial $($Arguments -join ' ')" }
    return $result
}

function Ensure-DebugPackageInstalled {
    $path = Invoke-Adb @("shell", "pm", "path", $DebugPackage)
    if (-not ($path -match "^package:")) {
        throw "The disposable debug package '$DebugPackage' is not installed on $script:ResolvedSerial."
    }
}

function Ensure-DebugTestPackageInstalled {
    $path = Invoke-Adb @("shell", "pm", "path", $DebugTestPackage)
    if (-not ($path -match "^package:")) {
        throw "The debug instrumentation package '$DebugTestPackage' is not installed on $script:ResolvedSerial. Run -Action BuildTestApk then -Action InstallTestApk first."
    }
}

function New-ArtifactPath {
    param([string]$LeafName)
    New-Item -ItemType Directory -Force -Path $ArtifactRoot | Out-Null
    Join-Path $ArtifactRoot "$(Get-Date -Format 'yyyyMMdd-HHmmss')-$LeafName"
}

function Get-NetworkSummary {
    (Invoke-Adb @("shell", "dumpsys", "connectivity")) | Select-String -Pattern "ActiveNetwork|NetworkAgentInfo|TRANSPORT" | Select-Object -First 30 | ForEach-Object Line
}

function Save-DeviceStatus {
    Ensure-DebugPackageInstalled
    $gitCommit = try { (git -C (Join-Path $PSScriptRoot "..\..") rev-parse HEAD).Trim() } catch { "unavailable" }
    $packageDump = Invoke-Adb @("shell", "dumpsys", "package", $DebugPackage)
    $status = [ordered]@{
        capturedAt = (Get-Date).ToUniversalTime().ToString("o")
        serial = $script:ResolvedSerial
        model = (Invoke-Adb @("shell", "getprop", "ro.product.model")).Trim()
        androidVersion = (Invoke-Adb @("shell", "getprop", "ro.build.version.release")).Trim()
        sdk = (Invoke-Adb @("shell", "getprop", "ro.build.version.sdk")).Trim()
        packageName = $DebugPackage
        packageVersion = ($packageDump | Select-String -Pattern "versionName=|versionCode=" | ForEach-Object Line)
        commit = $gitCommit
        networkMode = $NetworkMode
        networkSummary = Get-NetworkSummary
        providerAccountState = if ($env:OMNITUNE_QA_PROVIDER_ACCOUNT_STATE) { $env:OMNITUNE_QA_PROVIDER_ACCOUNT_STATE } else { "record manually; do not put tokens in this field" }
    }
    $destination = New-ArtifactPath "device-status.json"
    $status | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $destination -Encoding utf8
    Write-Host "Saved device status: $destination"
}

function Start-LogcatCapture {
    Ensure-DebugPackageInstalled
    New-Item -ItemType Directory -Force -Path $ArtifactRoot | Out-Null
    $logPath = New-ArtifactPath "logcat.txt"
    $pidPath = Join-Path $ArtifactRoot "logcat.pid"
    if (Test-Path -LiteralPath $pidPath) {
        throw "A QA logcat capture is already registered. Run -Action StopLogcat first."
    }
    Invoke-Adb @("logcat", "-c") | Out-Null
    $process = Start-Process -FilePath $script:ResolvedAdb -ArgumentList @("-s", $script:ResolvedSerial, "logcat", "-v", "threadtime", "-f", $logPath) -WindowStyle Hidden -PassThru
    [ordered]@{ pid = $process.Id; path = $logPath; serial = $script:ResolvedSerial } | ConvertTo-Json | Set-Content -LiteralPath $pidPath -Encoding utf8
    Write-Host "Capturing logcat to $logPath (PID $($process.Id))."
}

function Stop-LogcatCapture {
    $pidPath = Join-Path $ArtifactRoot "logcat.pid"
    if (-not (Test-Path -LiteralPath $pidPath)) {
        Write-Host "No QA logcat capture is registered."
        return
    }
    $capture = Get-Content -LiteralPath $pidPath -Raw | ConvertFrom-Json
    $process = Get-Process -Id $capture.pid -ErrorAction SilentlyContinue
    if ($process) { Stop-Process -Id $process.Id }
    Remove-Item -LiteralPath $pidPath
    Write-Host "Stopped QA logcat capture. Evidence: $($capture.path)"
}

$requiresDevice = $Action -notin @("Build", "BuildTestApk", "StopLogcat")
if ($requiresDevice) {
    $ResolvedAdb = Resolve-Adb
    $ResolvedSerial = Get-ConnectedSerial -ResolvedAdb $ResolvedAdb
}

switch ($Action) {
    "Build" {
        & (Join-Path $PSScriptRoot "..\..\gradlew.bat") ":app:assembleDebug"
        if ($LASTEXITCODE -ne 0) { throw "Debug build failed." }
        Write-Host "Debug APK: $ApkPath"
    }
    "BuildTestApk" {
        & (Join-Path $PSScriptRoot "..\..\gradlew.bat") ":app:assembleDebugAndroidTest"
        if ($LASTEXITCODE -ne 0) { throw "Debug instrumentation-test build failed." }
        Write-Host "Debug test APK: $TestApkPath"
    }
    "Status" { Save-DeviceStatus }
    "Install" {
        if (-not (Test-Path -LiteralPath $ApkPath)) { throw "Debug APK is missing: $ApkPath. Run -Action Build first." }
        Invoke-Adb @("install", "-r", $ApkPath) | Write-Host
        Save-DeviceStatus
    }
    "InstallTestApk" {
        Ensure-DebugPackageInstalled
        if (-not (Test-Path -LiteralPath $TestApkPath)) { throw "Debug test APK is missing: $TestApkPath. Run -Action BuildTestApk first." }
        Invoke-Adb @("install", "-r", $TestApkPath) | Write-Host
    }
    "RunFixture" {
        if ([string]::IsNullOrWhiteSpace($Fixture)) {
            throw "Pass -Fixture with one of the explicitly isolated fixture names."
        }
        Ensure-DebugPackageInstalled
        Ensure-DebugTestPackageInstalled
        $fixtureClass = switch ($Fixture) {
            "PlaybackPreferences" { "com.omnitune.app.playback.PlaybackPreferenceObserverInstrumentedTest" }
            "SearchViewModel" { "com.omnitune.app.ui.screens.SearchViewModelInstrumentedTest" }
            "MediaSessionCommands" { "com.omnitune.app.playback.MusicSessionCallbackInstrumentedTest" }
            "StreamResolver" { "com.omnitune.app.playback.StreamUrlResolverInstrumentedTest" }
            "LibraryPlaylists" { "com.omnitune.app.db.LibraryPlaylistPersistenceInstrumentedTest" }
            "OfflineCache" { "com.omnitune.app.playback.OfflinePlaybackCacheRoutingInstrumentedTest" }
            "BackupRepository" { "com.omnitune.app.backup.OmniBackupRepositoryInstrumentedTest" }
            default { throw "Unsupported isolated fixture '$Fixture'." }
        }
        $outputPath = New-ArtifactPath "fixture-$Fixture.txt"
        $result = & $script:ResolvedAdb -s $script:ResolvedSerial shell am instrument -w -r -e class $fixtureClass "$DebugTestPackage/androidx.test.runner.AndroidJUnitRunner"
        $result | Set-Content -LiteralPath $outputPath -Encoding utf8
        if ($LASTEXITCODE -ne 0 -or (($result -join "`n") -match "FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed")) {
            throw "Fixture '$Fixture' failed. Instrumentation evidence: $outputPath"
        }
        Save-DeviceStatus
        Write-Host "Fixture '$Fixture' passed. Instrumentation evidence: $outputPath"
    }
    "SeedDataset" {
        Ensure-DebugPackageInstalled
        Invoke-Adb @("shell", "am", "force-stop", $DebugPackage) | Out-Null
        Invoke-Adb @(
            "shell", "am", "instrument", "-w", "-r",
            "-e", "class", "com.omnitune.app.runtime.RuntimeSmokeDatasetTest#seedDisposableDebugProfile",
            "com.omnitune.app.debug.test/androidx.test.runner.AndroidJUnitRunner"
        ) | Write-Host
        Save-DeviceStatus
        Write-Host "Seeded the disposable debug dataset. Completed and partial downloads are prepared by RT-07, not fabricated by this database fixture."
    }
    "ResetDebugProfile" {
        if (-not $ConfirmResetDebugProfile) {
            throw "Refusing to clear data. Re-run with -ConfirmResetDebugProfile; only $DebugPackage can be cleared."
        }
        Ensure-DebugPackageInstalled
        Invoke-Adb @("shell", "pm", "clear", $DebugPackage) | Write-Host
        Write-Host "Cleared only the disposable debug profile: $DebugPackage"
    }
    "Launch" {
        Ensure-DebugPackageInstalled
        Invoke-Adb @("shell", "am", "start", "-n", $DebugActivity) | Write-Host
    }
    "ForceStop" {
        Ensure-DebugPackageInstalled
        Invoke-Adb @("shell", "am", "force-stop", $DebugPackage) | Out-Null
        Write-Host "Force-stopped $DebugPackage without clearing data."
    }
    "ProcessDeath" {
        Ensure-DebugPackageInstalled
        Invoke-Adb @("shell", "am", "force-stop", $DebugPackage) | Out-Null
        Invoke-Adb @("shell", "am", "start", "-n", $DebugActivity) | Write-Host
        Write-Host "Simulated process death by force-stopping and relaunching the debug app."
    }
    "StartLogcat" { Start-LogcatCapture }
    "StopLogcat" { Stop-LogcatCapture }
    "Screenshot" {
        Ensure-DebugPackageInstalled
        $remote = "/sdcard/omnitune-runtime-screenshot.png"
        $destination = New-ArtifactPath "screenshot.png"
        Invoke-Adb @("shell", "screencap", "-p", $remote) | Out-Null
        Invoke-Adb @("pull", $remote, $destination) | Out-Null
        Invoke-Adb @("shell", "rm", "-f", $remote) | Out-Null
        Write-Host "Saved screenshot: $destination"
    }
    "PullBackup" {
        if ([string]::IsNullOrWhiteSpace($RemoteBackupPath) -or -not $RemoteBackupPath.StartsWith("/sdcard/")) {
            throw "Pass -RemoteBackupPath for an explicit /sdcard/... backup file."
        }
        $destination = New-ArtifactPath ([IO.Path]::GetFileName($RemoteBackupPath))
        Invoke-Adb @("pull", $RemoteBackupPath, $destination) | Out-Null
        Write-Host "Pulled backup to $destination"
    }
    "SetNetwork" {
        if ($NetworkMode -eq "Unchanged") { throw "Pass -NetworkMode Offline or -NetworkMode Online." }
        if (-not $AllowNetworkMutation) {
            throw "Refusing to change device networking. Re-run with -AllowNetworkMutation after recording the current network state."
        }
        if ($NetworkMode -eq "Offline") {
            Invoke-Adb @("shell", "svc", "wifi", "disable") | Out-Null
            Invoke-Adb @("shell", "svc", "data", "disable") | Out-Null
        } else {
            Invoke-Adb @("shell", "svc", "wifi", "enable") | Out-Null
            Invoke-Adb @("shell", "svc", "data", "enable") | Out-Null
        }
        Save-DeviceStatus
        Write-Host "Requested $NetworkMode network state. Confirm the actual state in the saved device-status evidence."
    }
}
