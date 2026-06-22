param(
    [string]$OutputRoot = "",
    [string]$OutputFile = "",
    [ValidateSet("Full", "Demo")]
    [string]$Flavor = "Full",
    [string]$PackageName = "",
    [string]$AvdName = "OnlyTechTakeDemo",
    [switch]$UseConnectedDevice,
    [switch]$SkipBuild,
    [switch]$SeedData,
    [ValidateSet("", "manual", "all", "full-workflow", "social-stories", "config", "customers", "maintenance", "demo-limit", "social-campaign")]
    [string]$ScenarioId = "",
    [switch]$StartImmediately,
    [int]$MaxDurationSeconds = 180,
    [int]$BitRate = 4000000,
    [double]$AutomaticTrimStartSeconds = 7,
    [double]$AutomaticTrimEndSeconds = 2,
    [string]$Size,
    [switch]$NonInteractive
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path

# Banner
if (-not $NonInteractive) {
    Write-Host "`n--- OnlyTechTake Demo Recorder ---" -ForegroundColor Cyan
    Write-Host "Registra flussi utente dal dispositivo per video promozionali."
}

if ([string]::IsNullOrWhiteSpace($PackageName)) {
    $packageNameSuffix = switch ($Flavor) {
        "Full" { "" }
        "Demo" { ".demo" }
        "Showcase" { ".showcase" }
    }
    $PackageName = "com.onlytechtake.repairapp$($packageNameSuffix).debug"
}

if ($MaxDurationSeconds -lt 1 -or $MaxDurationSeconds -gt 180) {
    throw "MaxDurationSeconds deve essere compreso tra 1 e 180, limite di adb screenrecord."
}
if ($AutomaticTrimStartSeconds -lt 0 -or $AutomaticTrimEndSeconds -lt 0) {
    throw "I margini di taglio automatico non possono essere negativi."
}

if (-not $OutputRoot) {
    $OutputRoot = Join-Path $root "reports/generated-assets/$Flavor/demo/videos/device"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputRoot)) {
    $OutputRoot = Join-Path $root $OutputRoot
}
$environmentScript = Join-Path $PSScriptRoot "..\support\android_env.ps1"
if (Test-Path $environmentScript) {
    . $environmentScript
    Set-AndroidCliEnvironment | Out-Null
}

$demoSupportScript = Join-Path $PSScriptRoot "..\support\android_demo.ps1"
if (Test-Path $demoSupportScript) {
    . $demoSupportScript
} else {
    throw "Helper demo Android non trovato: $demoSupportScript"
}

if ($OutputFile -eq "") {
    $scenarioName = if ([string]::IsNullOrWhiteSpace($ScenarioId)) { "manual" } else { $ScenarioId }
    $OutputFile = "onlytechtake-$($Flavor.ToLowerInvariant())-$scenarioName.mp4"
}

Assert-PathInsideRoot -Path $OutputRoot -RootPath $root
New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null

$outputPath = Join-Path $OutputRoot $OutputFile
if (Test-Path $outputPath) {
    Remove-Item -LiteralPath $outputPath -Force
}

Write-Host "--- OnlyTechTake Demo Recorder ---" -ForegroundColor Cyan

trap {
    Restore-DeviceGlobalSettings
    throw $_
}

# Detect and resolve device
$serial = Resolve-DemoDeviceSerial -AndroidHome $env:ANDROID_HOME -AvdName $AvdName -UseConnectedDevice:$UseConnectedDevice
Write-Host "Uso device: $serial" -ForegroundColor Green

Write-Host "Preparazione e installazione APK ($Flavor)..." -ForegroundColor Gray
Invoke-DemoApkBuildAndInstall -RootPath $root -Serial $serial -Flavor $Flavor -SkipBuild:$SkipBuild

Write-Host "Pulizia dati app $PackageName..." -ForegroundColor Gray
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "pm", "clear", $PackageName)

# Configurazioni flussi demo per registrazione
$recordingsConfig = @(
    [PSCustomObject]@{
        Id = "social-campaign"
        Description = "Social Campaign (Flusso completo ottimizzato per marketing)"
        TestClass = "com.onlytechtake.repairapp.demo.SocialCampaignScreenshotTest"
    },
    [PSCustomObject]@{
        Id = "full-workflow"
        Description = "Workflow Completo (Wizard, Firma, WhatsApp, PDF, Chiusura)"
        TestClass = "com.onlytechtake.repairapp.demo.OperatorIntakeDemoScreenshotTest"
    },
    [PSCustomObject]@{
        Id = "social-stories"
        Description = "Social Stories (Tutti i flussi in un unico video)"
        TestClass = "com.onlytechtake.repairapp.demo.SocialStoryDemoScreenshotTest"
        SkipBatch = $true
    },
    [PSCustomObject]@{
        Id = "config"
        Description = "Configurazione (Categorie e Template)"
        TestClass = "com.onlytechtake.repairapp.demo.SocialStoryDemoScreenshotTest#capturesCategoryAndTemplateStories"
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
    },
    [PSCustomObject]@{
        Id = "demo-limit"
        Description = "Limite commesse Demo"
        TestClass = "com.onlytechtake.repairapp.demo.SocialStoryDemoScreenshotTest#capturesDemoLimitStory"
        RequiredFlavor = "Demo"
    }
)

# Selezione flussi se non specificato
$selectedTestClass = $null
$allScenarios = $false

if ($ScenarioId) {
    if ($ScenarioId -eq "all") {
        $allScenarios = $true
        Write-Host "Verranno eseguiti TUTTI gli scenari automatici in sequenza." -ForegroundColor Green
    } elseif ($ScenarioId -ne "manual") {
        $selectedFlow = $recordingsConfig | Where-Object { $_.Id -eq $ScenarioId } | Select-Object -First 1
        if ($null -eq $selectedFlow) {
            throw "ScenarioId non valido: $ScenarioId"
        }
        if ($selectedFlow.RequiredFlavor -and $selectedFlow.RequiredFlavor -ne $Flavor) {
            throw "Lo scenario '$ScenarioId' richiede il flavor $($selectedFlow.RequiredFlavor)."
        }
        $selectedTestClass = $selectedFlow.TestClass
        Write-Host "Verrà eseguito lo scenario: $($selectedFlow.Description)" -ForegroundColor Green
    }
} elseif (-not $NonInteractive) {
    Write-Host "`n--- Selezione Modalità Registrazione ---" -ForegroundColor Yellow
    Write-Host "0. Manuale (Avvia l'app e registra quello che fai)"
    Write-Host "A. Tutto (Esegue tutti gli scenari automatici in sequenza)"
    $i = 1
    foreach ($flow in $recordingsConfig) {
        if ($flow.RequiredFlavor -and $flow.RequiredFlavor -ne $Flavor) {
            Write-Host "$i. [NON DISPONIBILE] $($flow.Description)" -ForegroundColor Gray
        } else {
            Write-Host "$i. $($flow.Description)"
        }
        $i++
    }

    $choice = Read-Host "`nScegli il flusso da registrare [0-$($i-1) o A]"
    if ($choice -eq 'A') {
        $allScenarios = $true
        Write-Host "Verranno eseguiti TUTTI gli scenari." -ForegroundColor Green
    } elseif ($choice -match "^\d+$" -and [int]$choice -gt 0 -and [int]$choice -lt $i) {
        $selectedTestClass = $recordingsConfig[[int]$choice - 1].TestClass
        Write-Host "Verrà eseguito lo scenario: $($recordingsConfig[[int]$choice - 1].Description)" -ForegroundColor Green
    }
}

function Normalize-Video {
    param(
        [string]$Path,
        [string]$OutputRoot,
        [double]$StartSec,
        [double]$EndSec
    )

    if ($null -eq (Get-Command "ffmpeg" -ErrorAction SilentlyContinue) -or
        $null -eq (Get-Command "ffprobe" -ErrorAction SilentlyContinue)
    ) {
        return
    }

    $probeJson = & ffprobe -v error -show_entries format=duration -of json $Path
    if ($LASTEXITCODE -ne 0) { return }
    $durationText = (($probeJson -join "`n") | ConvertFrom-Json).format.duration
    $duration = [double]::Parse([string]$durationText, [System.Globalization.CultureInfo]::InvariantCulture)
    $normalizedDuration = $duration - $StartSec - $EndSec
    if ($normalizedDuration -le 0) { return }

    $tempPath = Join-Path $OutputRoot (".temp_norm_" + [System.IO.Path]::GetFileName($Path))
    try {
        Write-Host "Normalizzazione video..." -ForegroundColor Gray
        Invoke-Checked -FilePath "ffmpeg" -Arguments @(
            "-y", "-fflags", "+genpts",
            "-ss", $StartSec.ToString([System.Globalization.CultureInfo]::InvariantCulture),
            "-i", $Path,
            "-t", $normalizedDuration.ToString([System.Globalization.CultureInfo]::InvariantCulture),
            "-map", "0:v:0", "-an",
            "-vf", "setpts=PTS-STARTPTS,fps=30",
            "-c:v", "libx264", "-preset", "veryfast", "-crf", "20",
            "-pix_fmt", "yuv420p", "-movflags", "+faststart",
            $tempPath
        )
        Move-Item -LiteralPath $tempPath -Destination $Path -Force
    } finally {
        if (Test-Path $tempPath) { Remove-Item -LiteralPath $tempPath -Force }
    }
}

# Grant necessary permissions for demo recording
function Grant-DemoPermissions {
    param([string]$Serial, [string]$PackageName)
    Write-Host "Concessione permessi a $PackageName..." -ForegroundColor Gray
    adb -s $Serial shell pm grant $PackageName android.permission.CAMERA 2>$null
    adb -s $Serial shell pm grant $PackageName android.permission.POST_NOTIFICATIONS 2>$null
}

Grant-DemoPermissions -Serial $serial -PackageName $PackageName

if ($SeedData) {
    Write-Host "Seeding dati demo ($Flavor)..." -ForegroundColor Yellow
    Invoke-DemoScreenshotTest -Serial $serial -TestClass "com.onlytechtake.repairapp.demo.SeedDemoDataTest" -Flavor $Flavor -FailureMessage "Seed dei dati fallito."
}

if ($null -eq $selectedTestClass -and -not $allScenarios) {
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

if ($selectedTestClass -or $allScenarios) {
    Write-Host "Avvio registrazione e scenario AUTOMATICO..." -ForegroundColor Cyan

    $fullRecordArgs = @("-s", $serial, "shell", "screenrecord") + $screenRecordArgs

    if ($allScenarios) {
        foreach ($flow in $recordingsConfig) {
            if ($flow.RequiredFlavor -and $flow.RequiredFlavor -ne $Flavor) { continue }
            if ($flow.SkipBatch) { continue }

            $scenarioName = $flow.Id
            $currentOutputFile = "onlytechtake-$($Flavor.ToLowerInvariant())-$scenarioName.mp4"
            $currentOutputPath = Join-Path $OutputRoot $currentOutputFile

            Write-Host "`n--- Esecuzione Scenario: $($flow.Description) ---" -ForegroundColor Yellow

            # Pulizia dati per ogni scenario nel batch per garantire stato noto
            adb -s $serial shell pm clear $PackageName | Out-Null
            Grant-DemoPermissions -Serial $serial -PackageName $PackageName

            # Restart screenrecord for each scenario to have separate files
            $recordProc = Start-Process -FilePath "adb" -ArgumentList $fullRecordArgs -PassThru -WindowStyle Hidden
            try {
                Start-Sleep -Seconds 3
                Invoke-DemoScreenshotTest `
                    -Serial $serial `
                    -TestClass $flow.TestClass `
                    -Flavor $Flavor `
                    -InstrumentationExtras @{ demoRecording = "true" }
            } finally {
                adb -s $serial shell "pkill -2 screenrecord 2>/dev/null"
                if ($recordProc) {
                    $recordProc | Wait-Process -Timeout 10 -ErrorAction SilentlyContinue
                    Stop-Process -Id $recordProc.Id -Force -ErrorAction SilentlyContinue
                }
                Start-Sleep -Seconds 2
            }

            Write-Host "Recupero video: $currentOutputFile" -ForegroundColor Cyan
            Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "test", "-s", $remoteVideoPath)
            Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "pull", $remoteVideoPath, $currentOutputPath)
            Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-f", $remoteVideoPath)

            # Normalize each video
            Normalize-Video -Path $currentOutputPath -OutputRoot $OutputRoot -StartSec $AutomaticTrimStartSeconds -EndSec $AutomaticTrimEndSeconds
        }
        Write-Host "`nBatch completato." -ForegroundColor Green
        Restore-DeviceGlobalSettings
        exit 0
    }

    # Avviamo screenrecord come processo separato per gestire meglio la terminazione
    $recordProc = Start-Process -FilePath "adb" -ArgumentList $fullRecordArgs -PassThru -WindowStyle Hidden

    try {
        Start-Sleep -Seconds 3 # Buffer per inizializzazione codec
        Invoke-DemoScreenshotTest `
            -Serial $serial `
            -TestClass $selectedTestClass `
            -Flavor $Flavor `
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
    if (-not $StartImmediately) {
        Write-Host "Premi INVIO per iniziare a registrare..." -ForegroundColor Yellow
        Read-Host
    }

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
    Write-Host "Salvataggio in: $(Get-RepoRelativePath -FullPath $outputPath -RootPath $root)" -ForegroundColor Gray
    if ($selectedTestClass) {
        Normalize-Video -Path $outputPath -OutputRoot $OutputRoot -StartSec $AutomaticTrimStartSeconds -EndSec $AutomaticTrimEndSeconds
    }

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
