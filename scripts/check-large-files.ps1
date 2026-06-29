param()

$SearchPath = "app/src/main/kotlin"

if (-not (Test-Path $SearchPath)) {
    Write-Host "Directory $SearchPath not found. Run from project root."
    exit 1
}

$files = Get-ChildItem -Path $SearchPath -Recurse -Filter "*.kt" | Where-Object { $_.FullName -notmatch "build" -and $_.FullName -notmatch "generated" }

$warningCount = 0
$reviewCount = 0

foreach ($file in $files) {
    # Fast line count trick in PS
    $lineCount = 0
    Get-Content $file.FullName | ForEach-Object { $lineCount++ }
    
    if ($lineCount -ge 1000) {
        Write-Host "REVIEW REQUIRED: 1000+ lines in $($file.FullName) ($lineCount lines)" -ForegroundColor Red
        $reviewCount++
    } elseif ($lineCount -ge 500) {
        Write-Host "WARNING: 500+ lines in $($file.FullName) ($lineCount lines)" -ForegroundColor Yellow
        $warningCount++
    }
}

Write-Host "----------------------------------------"
Write-Host "Scan complete. Warnings (500+): $warningCount, Reviews required (1000+): $reviewCount."

# Does not exit with error code by default so it doesn't break CI unless configured to.
exit 0
