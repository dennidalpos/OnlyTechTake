param(
    [string]$OutputRoot = "demo/recordings",
    [string]$OutputFile = "",
    [string]$PackageName = "com.onlytechtake.repairapp.debug",
    [switch]$SkipBuild,
    [switch]$SeedData,
    [int]$MaxDurationSeconds = 180,
    [int]$BitRate = 4000000,
    [string]$Size
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$supportScript = Join-Path $PSScriptRoot "..\support\android_env.ps1"

if (Test-Path $supportScript) {
    . $supportScript
    Set-AndroidCliEnvironment | Out-Null
}

$demoSupportScript = Join-Path $PSScriptRoot "support\android_demo.ps1"
if (Test-Path $demoSupportScript) {
    . $demoSupportScript
}

if ($OutputFile -eq "") {
    $OutputFile = "demo_$(Get-Date -Format 'yyyyMMdd_HHmmss').mp4"
}

if (-not (Test-Path $OutputRoot)) {
    New-Item -ItemType Directory -Path $OutputRoot | Out-Null
}

$outputPath = Join-Path $OutputRoot $OutputFile

Write-Host "--- OnlyTechTake Demo Recorder ---" -ForegroundColor Cyan

function Test-Command($Name) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    return $null -ne $cmd
}

if (-not (Test-Command "adb")) {
    throw "adb non trovato nel PATH. Installa Android Platform Tools."
}

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

$gradlew = Join-Path $root "gradlew.bat"

if (-not $SkipBuild) {
    Write-Host "Building and installing APK..." -ForegroundColor Gray
    Push-Location $root
    try {
        & $gradlew :app:assembleFullDebug
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    } finally {
        Pop-Location
    }

    $apkPath = Join-Path $root "app/build/outputs/apk/full/debug/app-full-debug.apk"

    if (Test-Path $apkPath) {
        Write-Host "Installing $apkPath..." -ForegroundColor Gray
        adb -s $deviceId install -r $apkPath
    } else {
        Write-Host "ERRORE: APK non trovato." -ForegroundColor Red
        exit 1
    }
}

Write-Host "Pulizia dati app $PackageName..." -ForegroundColor Gray
adb -s $deviceId shell pm clear $PackageName

if ($SeedData) {
    Write-Host "Seeding dati demo..." -ForegroundColor Yellow
    # Install AndroidTest APK if needed
    if (-not $SkipBuild) {
        Push-Location $root
        try {
            & $gradlew :app:assembleFullDebugAndroidTest
        } finally {
            Pop-Location
        }
        $testApkPath = Join-Path $root "app/build/outputs/apk/androidTest/full/debug/app-full-debug-androidTest.apk"
        if (Test-Path $testApkPath) {
            adb -s $deviceId install -r $testApkPath
        } else {
            Write-Host "ERRORE: Test APK non trovato." -ForegroundColor Red
            exit 1
        }
    }

    $testClass = "com.onlytechtake.repairapp.demo.SeedDemoDataTest"
    $runner = "$PackageName.test/androidx.test.runner.AndroidJUnitRunner"

    Write-Host "Eseguo seed dati..."
    Invoke-InstrumentationChecked -Arguments @("-s", $deviceId, "shell", "am", "instrument", "-w", "-e", "class", $testClass, $runner)
}

Write-Host "Avvio applicazione..." -ForegroundColor Gray
adb -s $deviceId shell am start -n "$PackageName/com.onlytechtake.repairapp.MainActivity"

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
    adb -s $deviceId shell screenrecord @screenRecordArgs
}
catch {
    # This might catch termination
}

Write-Host "`nRegistrazione terminata. Recupero file..." -ForegroundColor Cyan
Invoke-Checked -FilePath "adb" -Arguments @("-s", $deviceId, "pull", "/sdcard/demo_temp.mp4", $outputPath)
adb -s $deviceId shell rm /sdcard/demo_temp.mp4

if (Test-Path $outputPath) {
    Write-Host "File salvato correttamente in: $outputPath" -ForegroundColor Green
} else {
    Write-Host "ERRORE: Recupero file non riuscito." -ForegroundColor Red
}
