param(
    [string]$OutputRoot = "demo/recordings",
    [string]$OutputFile = "",
    [switch]$SkipBuild,
    [int]$MaxDurationSeconds = 180,
    [int]$BitRate = 4000000,
    [string]$Size
)

if ($OutputFile -eq "") {
    $OutputFile = "demo_$(Get-Date -Format 'yyyyMMdd_HHmmss').mp4"
}

if (-not (Test-Path $OutputRoot)) {
    New-Item -ItemType Directory -Path $OutputRoot | Out-Null
}

$outputPath = Join-Path $OutputRoot $OutputFile

Write-Host "--- OnlyTechTake Demo Recorder ---" -ForegroundColor Cyan

# Detect device
$devices = adb devices | Select-String -Pattern "\tdevice$"
if ($devices.Count -eq 0) {
    Write-Host "ERRORE: Nessun dispositivo fisico rilevato via USB (adb devices)." -ForegroundColor Red
    exit 1
}
if ($devices.Count -gt 1) {
    Write-Host "ERRORE: Rilevati più dispositivi. Collegarne solo uno." -ForegroundColor Red
    exit 1
}

$deviceId = $devices[0].ToString().Split("`t")[0].Trim()
Write-Host "Dispositivo rilevato: $deviceId" -ForegroundColor Green

if (-not $SkipBuild) {
    Write-Host "Building and installing APK..." -ForegroundColor Gray
    .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    adb -s $deviceId install -r app/build/outputs/apk/debug/app-debug.apk
}

Write-Host "Pulizia dati app com.onlytechtake.repairapp..." -ForegroundColor Gray
adb -s $deviceId shell pm clear com.onlytechtake.repairapp

Write-Host "Avvio applicazione..." -ForegroundColor Gray
adb -s $deviceId shell am start -n com.onlytechtake.repairapp/com.onlytechtake.repairapp.MainActivity

Write-Host "Avvio registrazione (Massimo $MaxDurationSeconds secondi)..." -ForegroundColor Yellow
Write-Host "Premi CTRL+C per fermare anticipatamente la registrazione." -ForegroundColor Gray

$screenRecordArgs = "/sdcard/demo_temp.mp4 --time-limit $MaxDurationSeconds --bit-rate $BitRate"
if ($Size) { $screenRecordArgs += " --size $Size" }

# We use a simple wait loop and trap CTRL+C
$stopRequested = $false
$Host.UI.RawUI.FlushInputBuffer()

try {
    # Screenrecord blocks until time-limit or CTRL+C (which kills the shell command)
    adb -s $deviceId shell screenrecord $screenRecordArgs
}
catch {
    # This might catch termination
}

Write-Host "`nRegistrazione terminata. Recupero file..." -ForegroundColor Cyan
adb -s $deviceId pull /sdcard/demo_temp.mp4 $outputPath
adb -s $deviceId shell rm /sdcard/demo_temp.mp4

if (Test-Path $outputPath) {
    Write-Host "File salvato correttamente in: $outputPath" -ForegroundColor Green
} else {
    Write-Host "ERRORE: Recupero file non riuscito." -ForegroundColor Red
}
