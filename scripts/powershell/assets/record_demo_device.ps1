<#
.SYNOPSIS
  Registra flussi utente dal dispositivo per video promozionali.

.USAGE
  Registrazione manuale:
    .\scripts\powershell\assets\record_demo_device.ps1 -Flavor Full

  Registrazione automatica da profilo:
    .\scripts\powershell\assets\record_demo_device.ps1 -AssetProfile store-pro -Flavor Full
#>

param(
    [string]$OutputRoot = "",
    [string]$OutputFile = "",
    [ValidateSet("Full", "Demo", "Showcase")]
    [string]$Flavor = "Full",
    [string]$AssetProfile,
    [string]$Platform,
    [string]$Variant,
    [string]$PackageName = "",
    [string]$AvdName = "OnlyTechTakeDemo",
    [switch]$UseConnectedDevice,
    [switch]$SkipBuild,
    [switch]$SeedData,
    [ValidateSet("", "manual", "all", "full-workflow", "social-stories", "config", "customers", "maintenance", "demo-limit", "social-campaign")]
    [string]$ScenarioId = "", # Mantenuto per compatibilità
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

# Carica helper
. (Join-Path $PSScriptRoot "..\support\android_env.ps1")
Set-AndroidCliEnvironment | Out-Null
. (Join-Path $PSScriptRoot "..\support\android_demo.ps1")

# Banner
if (-not $NonInteractive) {
    Write-Host "`n--- OnlyTechTake Demo Recorder ---" -ForegroundColor Cyan
    Write-Host "Registra flussi utente dal dispositivo per video promozionali."
}

# Risoluzione AssetProfile/ScenarioId
if (-not $AssetProfile -and $ScenarioId -and $ScenarioId -ne "manual" -and $ScenarioId -ne "all") {
    $AssetProfile = $ScenarioId
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

# Output Root
if (-not $OutputRoot) {
    $basePath = if ($AssetProfile) { "reports/generated-assets/$AssetProfile" } else { "reports/generated-assets/$Flavor" }
    $targetPlatform = if ($Platform) { $Platform } else { "device" }
    $targetVariant = if ($Variant) { $Variant } else { "raw" }
    $OutputRoot = Join-Path $root "$basePath/$targetPlatform/$targetVariant"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputRoot)) {
    $OutputRoot = Join-Path $root $OutputRoot
}

Assert-PathInsideRoot -Path $OutputRoot -RootPath $root
New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null

if ($OutputFile -eq "") {
    $id = if ($AssetProfile) { $AssetProfile } elseif ($ScenarioId) { $ScenarioId } else { "manual" }
    $OutputFile = "onlytechtake-$($Flavor.ToLowerInvariant())-$id.mp4"
}

$outputPath = Join-Path $OutputRoot $OutputFile
if (Test-Path $outputPath) {
    Remove-Item -LiteralPath $outputPath -Force
}

# Detect and resolve device
$serial = Resolve-DemoDeviceSerial -AndroidHome $env:ANDROID_HOME -AvdName $AvdName -UseConnectedDevice:$UseConnectedDevice
Invoke-DemoApkBuildAndInstall -RootPath $root -Serial $serial -Flavor $Flavor -SkipBuild:$SkipBuild

trap {
    Restore-DeviceGlobalSettings
    throw $_
}

# Configurazioni Caricabili da profili o statiche per compatibilità
$profilesDir = Join-Path $root "demo/assets/profiles"
$recordingsConfig = @()

# Carica profili JSON
if (Test-Path $profilesDir) {
    Get-ChildItem -Path $profilesDir -Filter "*.asset.json" | ForEach-Object {
        $cfg = Get-Content -Path $_.FullName -Raw | ConvertFrom-Json
        $recordingsConfig += [PSCustomObject]@{
            Id = $cfg.id
            Description = "Profilo: $($cfg.id)"
            TestClass = $cfg.testClass
            RequiredFlavor = $cfg.flavor
        }
    }
}

# Aggiunta scenari legacy se non presenti (hardcoded fallback)
$legacyScenarios = @(
    @{ Id = "social-stories"; Description = "Social Stories Legacy"; TestClass = "com.onlytechtake.repairapp.demo.SocialStoryDemoScreenshotTest" },
    @{ Id = "social-campaign"; Description = "Social Campaign Legacy"; TestClass = "com.onlytechtake.repairapp.demo.SocialCampaignScreenshotTest" },
    @{ Id = "full-workflow"; Description = "Full Workflow Legacy"; TestClass = "com.onlytechtake.repairapp.demo.OperatorIntakeDemoScreenshotTest" }
)
foreach ($leg in $legacyScenarios) {
    if (-not ($recordingsConfig | Where-Object { $_.Id -eq $leg.Id })) {
        $recordingsConfig += [PSCustomObject]$leg
    }
}

# Selezione flussi
$selectedTestClass = $null
$allScenarios = $false

if ($ScenarioId -eq "all") {
    $allScenarios = $true
} elseif ($AssetProfile) {
    $selectedFlow = $recordingsConfig | Where-Object { $_.Id -eq $AssetProfile } | Select-Object -First 1
    if ($selectedFlow) {
        $selectedTestClass = $selectedFlow.TestClass
    }
}

if (-not $selectedTestClass -and -not $allScenarios -and $ScenarioId -ne "manual" -and -not $NonInteractive) {
    Write-Host "`n--- Selezione Modalità Registrazione ---" -ForegroundColor Yellow
    Write-Host "0. Manuale"
    Write-Host "A. Tutto (Batch)"
    for ($i = 0; $i -lt $recordingsConfig.Count; $i++) {
        $flow = $recordingsConfig[$i]
        $status = if ($flow.RequiredFlavor -and $flow.RequiredFlavor -ne $Flavor) { "[SOLO $($flow.RequiredFlavor)] " } else { "" }
        Write-Host "$($i + 1). $status$($flow.Description)"
    }

    $choice = Read-Host "`nScegli [0-$($recordingsConfig.Count) o A]"
    if ($choice -eq 'A') {
        $allScenarios = $true
    } elseif ($choice -match "^\d+$" -and [int]$choice -ge 1 -and [int]$choice -le $recordingsConfig.Count) {
        $selectedTestClass = $recordingsConfig[[int]$choice - 1].TestClass
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
            "-ss", $StartSec.ToString("F2", [System.Globalization.CultureInfo]::InvariantCulture),
            "-i", $Path,
            "-t", $normalizedDuration.ToString("F2", [System.Globalization.CultureInfo]::InvariantCulture),
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

function Grant-DemoPermissions {
    param([string]$Serial, [string]$PackageName)
    Write-Host "Concessione permessi a $PackageName..." -ForegroundColor Gray
    adb -s $Serial shell pm grant $PackageName android.permission.CAMERA 2>$null
    adb -s $Serial shell pm grant $PackageName android.permission.POST_NOTIFICATIONS 2>$null
}

Grant-DemoPermissions -Serial $serial -PackageName $PackageName

if ($SeedData) {
    Write-Host "Seeding dati demo ($Flavor)..." -ForegroundColor Yellow
    Invoke-DemoScreenshotTest -Serial $serial -TestClass "com.onlytechtake.repairapp.demo.SeedDemoDataTest" -Flavor $Flavor
}

$remoteVideoPath = "/sdcard/demo_temp.mp4"
$screenRecordArgs = @("--time-limit", [string]$MaxDurationSeconds, "--bit-rate", [string]$BitRate)
if ($Size) { $screenRecordArgs += @("--size", $Size) }
$screenRecordArgs += $remoteVideoPath

if ($selectedTestClass -or $allScenarios) {
    $fullRecordArgs = @("-s", $serial, "shell", "screenrecord") + $screenRecordArgs

    if ($allScenarios) {
        foreach ($flow in $recordingsConfig) {
            if ($flow.RequiredFlavor -and $flow.RequiredFlavor -ne $Flavor) { continue }
            if ($flow.SkipBatch) { continue }

            Write-Host "`n--- Esecuzione Scenario: $($flow.Id) ---" -ForegroundColor Yellow
            adb -s $serial shell pm clear $PackageName | Out-Null
            Grant-DemoPermissions -Serial $serial -PackageName $PackageName

            $recordProc = Start-Process -FilePath "adb" -ArgumentList $fullRecordArgs -PassThru -WindowStyle Hidden
            try {
                Start-Sleep -Seconds 3
                Invoke-DemoScreenshotTest -Serial $serial -TestClass $flow.TestClass -Flavor $Flavor -InstrumentationExtras @{ demoRecording = "true" }
            } finally {
                adb -s $serial shell "pkill -2 screenrecord 2>/dev/null"
                if ($recordProc) {
                    $recordProc | Wait-Process -Timeout 10 -ErrorAction SilentlyContinue
                    Stop-Process -Id $recordProc.Id -Force -ErrorAction SilentlyContinue
                }
                Start-Sleep -Seconds 2
            }

            $currentOutputFile = "onlytechtake-$($Flavor.ToLowerInvariant())-$($flow.Id).mp4"
            $currentOutputPath = Join-Path $OutputRoot $currentOutputFile
            Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "pull", $remoteVideoPath, $currentOutputPath)
            Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-f", $remoteVideoPath)

            Normalize-Video -Path $currentOutputPath -OutputRoot $OutputRoot -StartSec $AutomaticTrimStartSeconds -EndSec $AutomaticTrimEndSeconds
        }
        Write-Host "`nBatch completato." -ForegroundColor Green
    } else {
        Write-Host "Avvio registrazione e scenario AUTOMATICO..." -ForegroundColor Cyan
        $recordProc = Start-Process -FilePath "adb" -ArgumentList $fullRecordArgs -PassThru -WindowStyle Hidden
        try {
            Start-Sleep -Seconds 3
            Invoke-DemoScreenshotTest -Serial $serial -TestClass $selectedTestClass -Flavor $Flavor -InstrumentationExtras @{ demoRecording = "true" }
            Write-Host "Scenario completato." -ForegroundColor Green
        } finally {
            adb -s $serial shell "pkill -2 screenrecord 2>/dev/null"
            if ($recordProc) {
                $recordProc | Wait-Process -Timeout 10 -ErrorAction SilentlyContinue
                Stop-Process -Id $recordProc.Id -Force -ErrorAction SilentlyContinue
            }
            Start-Sleep -Seconds 2
        }

        Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "pull", $remoteVideoPath, $outputPath)
        Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-f", $remoteVideoPath)
        Normalize-Video -Path $outputPath -OutputRoot $OutputRoot -StartSec $AutomaticTrimStartSeconds -EndSec $AutomaticTrimEndSeconds
    }
} else {
    if (-not $StartImmediately) {
        Write-Host "Premi INVIO per iniziare a registrare..." -ForegroundColor Yellow
        Read-Host
    }

    Write-Host "REGISTRAZIONE IN CORSO... Premi CTRL+C per fermare." -ForegroundColor Red
    try {
        adb -s $serial shell screenrecord @screenRecordArgs
    } catch {}

    Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "pull", $remoteVideoPath, $outputPath)
    Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-f", $remoteVideoPath)
}

Restore-DeviceGlobalSettings
Write-Host "`nRegistrazione terminata: $outputPath" -ForegroundColor Green
