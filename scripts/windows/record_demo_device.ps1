param(
    [string]$OutputRoot = "demo/recordings",
    [string]$OutputFile = "",
    [string]$PackageName = "com.onlytechtake.repairapp.debug",
    [string]$AvdName = "OnlyTechTakeDemo",
    [switch]$UseConnectedDevice,
    [switch]$SkipBuild,
    [switch]$SeedData,
    [int]$MaxDurationSeconds = 180,
    [int]$BitRate = 4000000,
    [string]$Size
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$environmentScript = Join-Path $PSScriptRoot "..\support\android_env.ps1"
if (Test-Path $environmentScript) {
    . $environmentScript
    Set-AndroidCliEnvironment | Out-Null
}

$demoSupportScript = Join-Path $PSScriptRoot "support\android_demo.ps1"
if (Test-Path $demoSupportScript) {
    . $demoSupportScript
} else {
    throw "Helper demo Android non trovato: $demoSupportScript"
}

if ($OutputFile -eq "") {
    $OutputFile = "demo_$(Get-Date -Format 'yyyyMMdd_HHmmss').mp4"
}

if (-not (Test-Path $OutputRoot)) {
    New-Item -ItemType Directory -Path $OutputRoot | Out-Null
}

$outputPath = Join-Path $OutputRoot $OutputFile

Write-Host "--- OnlyTechTake Demo Recorder ---" -ForegroundColor Cyan

# Detect and resolve device
$serial = Resolve-DemoDeviceSerial -AndroidHome $env:ANDROID_HOME -AvdName $AvdName -UseConnectedDevice:$UseConnectedDevice
Write-Host "Uso device: $serial" -ForegroundColor Green

if (-not $SkipBuild) {
    Write-Host "Building and installing APK..." -ForegroundColor Gray
    Invoke-DemoApkBuildAndInstall -RootPath $root -Serial $serial -SkipBuild:$SkipBuild
}

Write-Host "Pulizia dati app $PackageName..." -ForegroundColor Gray
adb -s $serial shell pm clear $PackageName

# Grant necessary permissions for demo recording (Camera)
Write-Host "Concessione permessi..." -ForegroundColor Gray
adb -s $serial shell pm grant $PackageName android.permission.CAMERA

if ($SeedData) {
    Write-Host "Seeding dati demo..." -ForegroundColor Yellow
    Invoke-DemoScreenshotTest -Serial $serial -TestClass "com.onlytechtake.repairapp.demo.SeedDemoDataTest" -FailureMessage "Seed dei dati fallito."
}

Write-Host "Avvio applicazione..." -ForegroundColor Gray
# MainActivity is in com.onlytechtake.repairapp, and PackageName is typically com.onlytechtake.repairapp.debug (full flavor)
adb -s $serial shell am start -W -n "$PackageName/com.onlytechtake.repairapp.MainActivity"

Write-Host "Avvio registrazione (Massimo $MaxDurationSeconds secondi)..." -ForegroundColor Yellow
Write-Host "Premi CTRL+C per fermare anticipatamente la registrazione." -ForegroundColor Gray

$screenRecordArgs = "/sdcard/demo_temp.mp4", "--time-limit", [string]$MaxDurationSeconds, "--bit-rate", [string]$BitRate
if ($Size) {
    $screenRecordArgs += "--size"
    $screenRecordArgs += $Size
}

# We use a simple wait loop and trap CTRL+C
$stopRequested = $false
$Host.UI.RawUI.FlushInputBuffer()

try {
    # Screenrecord blocks until time-limit or CTRL+C (which kills the shell command)
    adb -s $serial shell screenrecord @screenRecordArgs
}
catch {
    # This might catch termination
}

Write-Host "`nRegistrazione terminata. Recupero file..." -ForegroundColor Cyan
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "pull", "/sdcard/demo_temp.mp4", $outputPath)
adb -s $serial shell rm /sdcard/demo_temp.mp4

if (Test-Path $outputPath) {
    Write-Host "File salvato correttamente in: $outputPath" -ForegroundColor Green
} else {
    Write-Host "ERRORE: Recupero file non riuscito." -ForegroundColor Red
}

Restore-DeviceGlobalSettings
