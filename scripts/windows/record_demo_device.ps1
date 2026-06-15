param(
    [string]$OutputRoot = "",
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

if ($MaxDurationSeconds -lt 1 -or $MaxDurationSeconds -gt 180) {
    throw "MaxDurationSeconds deve essere compreso tra 1 e 180, limite di adb screenrecord."
}

if (-not $OutputRoot) {
    $OutputRoot = Join-Path $root "reports/marketing-assets/recordings"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputRoot)) {
    $OutputRoot = Join-Path $root $OutputRoot
}
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

trap {
    Restore-DeviceGlobalSettings
    throw $_
}

# Detect and resolve device
$serial = Resolve-DemoDeviceSerial -AndroidHome $env:ANDROID_HOME -AvdName $AvdName -UseConnectedDevice:$UseConnectedDevice
Write-Host "Uso device: $serial" -ForegroundColor Green

Write-Host "Preparazione e installazione APK..." -ForegroundColor Gray
Invoke-DemoApkBuildAndInstall -RootPath $root -Serial $serial -SkipBuild:$SkipBuild

Write-Host "Pulizia dati app $PackageName..." -ForegroundColor Gray
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "pm", "clear", $PackageName)

# Configurazioni flussi demo per registrazione
$recordingsConfig = @(
    [PSCustomObject]@{
        Id = "full-workflow"
        Description = "Workflow Completo (Wizard, Firma, WhatsApp, PDF, Chiusura)"
        TestClass = "com.onlytechtake.repairapp.demo.OperatorIntakeDemoScreenshotTest"
    },
    [PSCustomObject]@{
        Id = "social-stories"
        Description = "Social Stories (Wizard, Configurazione, Clienti)"
        TestClass = "com.onlytechtake.repairapp.demo.SocialStoryDemoScreenshotTest"
    },
    [PSCustomObject]@{
        Id = "customers"
        Description = "Anagrafiche Clienti (Privato/Azienda)"
        TestClass = "com.onlytechtake.repairapp.demo.SocialStoryDemoScreenshotTest#capturesPrivateAndBusinessCustomerStories"
    },
    [PSCustomObject]@{
        Id = "maintenance"
        Description = "Manutenzione (Storage, Backup, Export Excel)"
        TestClass = "com.onlytechtake.repairapp.demo.SocialStoryDemoScreenshotTest#capturesMaintenanceStories"
    }
)

# Selezione flussi se non specificato
$selectedTestClass = $null
Write-Host "`n--- Selezione Modalità Registrazione ---" -ForegroundColor Yellow
Write-Host "0. Manuale (Avvia l'app e registra quello che fai)"
$i = 1
foreach ($flow in $recordingsConfig) {
    Write-Host "$i. $($flow.Description)"
    $i++
}

$choice = Read-Host "`nScegli il flusso da registrare [0-$($i-1)]"
if ($choice -match "^\d+$" -and [int]$choice -gt 0 -and [int]$choice -lt $i) {
    $selectedTestClass = $recordingsConfig[[int]$choice - 1].TestClass
    Write-Host "Verrà eseguito lo scenario: $($recordingsConfig[[int]$choice - 1].Description)" -ForegroundColor Green
}

# Grant necessary permissions for demo recording
Write-Host "Concessione permessi..." -ForegroundColor Gray
adb -s $serial shell pm grant $PackageName android.permission.CAMERA
adb -s $serial shell pm grant $PackageName android.permission.POST_NOTIFICATIONS 2>$null

if ($SeedData) {
    Write-Host "Seeding dati demo..." -ForegroundColor Yellow
    Invoke-DemoScreenshotTest -Serial $serial -TestClass "com.onlytechtake.repairapp.demo.SeedDemoDataTest" -FailureMessage "Seed dei dati fallito."
}

if ($null -eq $selectedTestClass) {
    Write-Host "Avvio applicazione in modalità MANUALE..." -ForegroundColor Gray
    adb -s $serial shell am start -W -n "$PackageName/com.onlytechtake.repairapp.MainActivity"
}

Write-Host "Preparazione registrazione (Massimo $MaxDurationSeconds secondi)..." -ForegroundColor Yellow

$remoteVideoPath = "/sdcard/demo_temp.mp4"
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-f", $remoteVideoPath)

$screenRecordArgs = @("--time-limit", [string]$MaxDurationSeconds, "--bit-rate", [string]$BitRate)
if ($Size) {
    $screenRecordArgs += "--size"
    $screenRecordArgs += $Size
}
$screenRecordArgs += $remoteVideoPath

if ($selectedTestClass) {
    Write-Host "Avvio registrazione e scenario AUTOMATICO..." -ForegroundColor Cyan

    $fullRecordArgs = @("-s", $serial, "shell", "screenrecord") + $screenRecordArgs

    # Avviamo screenrecord come processo separato per gestire meglio la terminazione
    $recordProc = Start-Process -FilePath "adb" -ArgumentList $fullRecordArgs -PassThru -WindowStyle Hidden

    try {
        Start-Sleep -Seconds 3 # Buffer per inizializzazione codec
        Invoke-DemoScreenshotTest `
            -Serial $serial `
            -TestClass $selectedTestClass `
            -InstrumentationExtras @{ demoRecording = "true" }
        Write-Host "Scenario completato." -ForegroundColor Green
    }
    finally {
        Write-Host "Finalizzazione file video sul dispositivo..." -ForegroundColor Gray
        # Inviamo SIGINT (2) al processo screenrecord sul device per permettere la chiusura corretta del file MP4
        # Silenziamo eventuali errori di pkill su processi di sistema protetti
        adb -s $serial shell "pkill -2 screenrecord 2>/dev/null"

        if ($recordProc) {
            $recordProc | Wait-Process -Timeout 10 -ErrorAction SilentlyContinue
            Stop-Process -Id $recordProc.Id -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Seconds 2 # Buffer per il flushing del file su SDCard
    }
} else {
    Write-Host "Premi INVIO per iniziare a registrare..." -ForegroundColor Yellow
    Read-Host

    Write-Host "REGISTRAZIONE IN CORSO... Premi CTRL+C per fermare anticipatamente." -ForegroundColor Red
    try {
        adb -s $serial shell screenrecord @screenRecordArgs
    }
    catch {
        # Catch termination
    }
}

Write-Host "`nRegistrazione terminata. Recupero file..." -ForegroundColor Cyan
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "test", "-s", $remoteVideoPath)
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "pull", $remoteVideoPath, $outputPath)
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-f", $remoteVideoPath)

if (Test-Path $outputPath) {
    if ($null -ne (Get-Command "ffprobe" -ErrorAction SilentlyContinue)) {
        Invoke-Checked -FilePath "ffprobe" -Arguments @(
            "-v", "error",
            "-select_streams", "v:0",
            "-show_entries", "stream=codec_name,width,height,duration",
            "-of", "default=noprint_wrappers=1",
            $outputPath
        )
    }
    Write-Host "File salvato correttamente in: $outputPath" -ForegroundColor Green
} else {
    throw "Recupero file non riuscito: $outputPath"
}

Restore-DeviceGlobalSettings
