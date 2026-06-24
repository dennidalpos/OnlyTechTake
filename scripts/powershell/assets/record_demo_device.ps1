<#
.SYNOPSIS
  Registra flussi utente dal dispositivo per video promozionali.

.USAGE
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
    [string]$ScenarioId = "",
    [switch]$StartImmediately,
    [int]$MaxDurationSeconds = 180,
    [int]$BitRate = 4000000,
    [double]$AutomaticTrimStartSeconds = 7,
    [double]$AutomaticTrimEndSeconds = 2,
    [string]$Size,
    [switch]$NonInteractive,
    [switch]$NoDistribute
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path

# Carica helper
. (Join-Path $PSScriptRoot "..\support\android_env.ps1")
Set-AndroidCliEnvironment | Out-Null
. (Join-Path $PSScriptRoot "..\support\android_demo.ps1")

# Banner
if (-not $NonInteractive) {
    Write-Host "`n--- OnlyTechTake Video Recorder ---" -ForegroundColor Cyan
}

# Risoluzione AssetProfile
if (-not $AssetProfile -and $ScenarioId -and $ScenarioId -ne "manual") {
    $AssetProfile = if ($ScenarioId -eq "full-workflow") { "operator-intake" } else { $ScenarioId }
}

$profilesDir = Join-Path $root "demo/assets/profiles"
$selectedProfileConfig = $null
if ($AssetProfile) {
    $profilePath = Join-Path $profilesDir "$AssetProfile.asset.json"
    if (Test-Path $profilePath) {
        $selectedProfileConfig = Get-Content -Path $profilePath -Raw | ConvertFrom-Json
        if ($selectedProfileConfig.flavor -and ($PSBoundParameters.ContainsKey('Flavor') -eq $false)) {
            $Flavor = $selectedProfileConfig.flavor
        }
    }
}

if ([string]::IsNullOrWhiteSpace($PackageName)) {
    $packageNameSuffix = switch ($Flavor) { "Full" { "" }; "Demo" { ".demo" }; "Showcase" { ".showcase" } }
    $PackageName = "com.onlytechtake.repairapp$($packageNameSuffix).debug"
}

# Output Root (Gerarchia Orizzontale)
if (-not $OutputRoot) {
    $OutputRoot = Join-Path $root "asset/$AssetProfile"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputRoot)) {
    $OutputRoot = Join-Path $root $OutputRoot
}

if (-not (Test-Path $OutputRoot)) { New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null }

if ($OutputFile -eq "") {
    $id = if ($AssetProfile) { $AssetProfile } else { "manual" }
    $OutputFile = "onlytechtake-$($Flavor.ToLowerInvariant())-$id.mp4"
}

$outputPath = Join-Path $OutputRoot $OutputFile
if (Test-Path $outputPath) { Remove-Item -LiteralPath $outputPath -Force }

# Detect device
$serial = Resolve-DemoDeviceSerial -AndroidHome $env:ANDROID_HOME -AvdName $AvdName -UseConnectedDevice:$UseConnectedDevice
Invoke-DemoApkBuildAndInstall -RootPath $root -Serial $serial -Flavor $Flavor -SkipBuild:$SkipBuild

trap { Restore-DeviceGlobalSettings; throw $_ }

function Normalize-Video {
    param([string]$Path, [string]$OutputRoot, [double]$StartSec, [double]$EndSec)
    if ($null -eq (Get-Command "ffmpeg" -ErrorAction SilentlyContinue)) { return }
    $tempPath = Join-Path $OutputRoot (".temp_norm_" + [System.IO.Path]::GetFileName($Path))
    try {
        Invoke-Checked -FilePath "ffmpeg" -Arguments @("-y", "-i", $Path, "-ss", $StartSec.ToString(), "-vf", "fps=30", "-c:v", "libx264", "-crf", "20", "-pix_fmt", "yuv420p", $tempPath)
        Move-Item -LiteralPath $tempPath -Destination $Path -Force
    } finally { if (Test-Path $tempPath) { Remove-Item -LiteralPath $tempPath -Force } }
}

# Permissions
adb -s $serial shell pm grant $PackageName android.permission.CAMERA 2>$null

if ($SeedData) {
    Invoke-DemoScreenshotTest -Serial $serial -TestClass "com.onlytechtake.repairapp.demo.SeedDemoDataTest" -Flavor $Flavor
}

$remoteVideoPath = "/sdcard/demo_temp.mp4"
$screenRecordArgs = @("--time-limit", [string]$MaxDurationSeconds, "--bit-rate", [string]$BitRate, $remoteVideoPath)

Write-Host "Avvio registrazione..." -ForegroundColor Cyan
$recordProc = Start-Process -FilePath "adb" -ArgumentList (@("-s", $serial, "shell", "screenrecord") + $screenRecordArgs) -PassThru -WindowStyle Hidden
try {
    Start-Sleep -Seconds 3
    if ($selectedProfileConfig) {
        Invoke-DemoScreenshotTest -Serial $serial -TestClass $selectedProfileConfig.testClass -Flavor $Flavor -InstrumentationExtras @{ demoRecording = "true" }
    } else {
        Write-Host "Registrazione manuale in corso... Premi INVIO per fermare."
        Read-Host
    }
} finally {
    adb -s $serial shell "pkill -2 screenrecord"
    if ($recordProc) { $recordProc | Wait-Process -Timeout 10; Stop-Process -Id $recordProc.Id -Force -ErrorAction SilentlyContinue }
    Start-Sleep -Seconds 2
}

Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "pull", $remoteVideoPath, $outputPath)
adb -s $serial shell rm $remoteVideoPath
Normalize-Video -Path $outputPath -OutputRoot $OutputRoot -StartSec $AutomaticTrimStartSeconds -EndSec $AutomaticTrimEndSeconds

# Distribuzione Automatica
if (-not $NoDistribute) {
    $marketingVideoPath = Join-Path $root "marketing/videos/play-store"
    if (-not (Test-Path $marketingVideoPath)) { New-Item -ItemType Directory -Path $marketingVideoPath -Force | Out-Null }
    Copy-Item -Path $outputPath -Destination (Join-Path $marketingVideoPath "pro-workflow.mp4") -Force

    if ($AssetProfile -eq "store-pro" -or $AssetProfile -eq "operator-intake") {
        $landingVideoPath = Join-Path $root "landing/assets/videos"
        if (-not (Test-Path $landingVideoPath)) { New-Item -ItemType Directory -Path $landingVideoPath -Force | Out-Null }
        Copy-Item -Path $outputPath -Destination (Join-Path $landingVideoPath "intake-flow.mp4") -Force
    }
}

Restore-DeviceGlobalSettings
Write-Host "`nVideo registrato e distribuito: $outputPath" -ForegroundColor Green
