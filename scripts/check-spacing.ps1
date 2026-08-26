# Spacing discipline audit for the OmniTune UI rebuild.
# Counts raw dp literals in UI code. The rebuild waves must drive this number DOWN;
# theme/ definition files and the legacy singleton are excluded from counting.
#
# Usage:  powershell -File scripts\check-spacing.ps1 [-FailOnIncrease]
# Exit 1 when the count exceeds the baseline stored in .ui-spacing-baseline.

param(
    [switch]$FailOnIncrease
)

$root = Split-Path -Parent $PSScriptRoot
$uiRoot = Join-Path $root "app\src\main\kotlin\com\omnitune\app\ui"
$baselineFile = Join-Path $root ".ui-spacing-baseline"

$exclude = @(
    "theme\OmniSpacing.kt",
    "theme\OmniShapes.kt",
    "theme\OmniChrome.kt",
    "component\OmniComponents.kt",
    "component\PlayerSlider.kt",
    "component\AudioVisualizer.kt",
    "component\PlayingIndicator.kt",
    "component\DraggableScrollBarOverlay.kt",
    "player\PlayerBackgroundEffect.kt",
    "component\VeluneCompatibility.kt"
)

$total = 0
$byFile = @{}
Get-ChildItem -Recurse -Filter *.kt $uiRoot | ForEach-Object {
    $rel = $_.FullName.Substring($uiRoot.Length + 1)
    foreach ($ex in $exclude) { if ($rel -like $ex) { return } }
    $count = [regex]::Matches([System.IO.File]::ReadAllText($_.FullName), '\b\d+\.dp\b').Count
    if ($count -gt 0) { $byFile[$rel] = $count; $total += $count }
}

Write-Output "Raw dp literals (excluding drawing primitives/theme): $total"
$byFile.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 12 | ForEach-Object {
    Write-Output ("  {0,5}  {1}" -f $_.Value, $_.Key)
}

if (Test-Path $baselineFile) {
    $baseline = [int](Get-Content $baselineFile -TotalCount 1)
    Write-Output "Baseline: $baseline"
    if ($total -gt $baseline) {
        if ($FailOnIncrease) { exit 1 }
        Write-Output "WARNING: spacing violations increased."
    } else {
        Set-Content -Path $baselineFile -Value $total
        Write-Output "Improved; baseline updated to $total."
    }
} else {
    Set-Content -Path $baselineFile -Value $total
    Write-Output "Baseline created at $total. Waves must reduce this number."
}
