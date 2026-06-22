<#
.SYNOPSIS
  Genera gli asset per la campagna social (Asus ROG Flow).

.USAGE
  .\scripts\windows\generate_social_campaign.ps1 [-OnlyScreenshots] [-OnlyVideo]
#>

param(
    [string]$AvdName = "Pixel_7",
    [switch]$UseConnectedDevice,
    [ValidateSet("Full", "Demo")]
    [string]$Flavor = "Full",
    [switch]$SkipBuild,
    [switch]$OnlyScreenshots,
    [switch]$OnlyVideo
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$socialAssetsDir = Join-Path $root "social_assets"

# Support scripts
$environmentScript = Join-Path $PSScriptRoot "..\support\android_env.ps1"
if (Test-Path $environmentScript) { . $environmentScript; Set-AndroidCliEnvironment | Out-Null }
$demoSupportScript = Join-Path $PSScriptRoot "support\android_demo.ps1"
if (Test-Path $demoSupportScript) { . $demoSupportScript } else { throw "Helper demo Android non trovato." }

$doSS = $true; $doVideo = $true
if ($OnlyScreenshots) { $doVideo = $false }
if ($OnlyVideo) { $doSS = $false }

Write-Host "--- Generazione Asset Social Campaign (Asus ROG) ---" -ForegroundColor Cyan

# 1. Preparazione Directory
if (-not (Test-Path $socialAssetsDir)) { New-Item -ItemType Directory -Path $socialAssetsDir | Out-Null }

try {
    $serial = Resolve-DemoDeviceSerial -AndroidHome $env:ANDROID_HOME -AvdName $AvdName -UseConnectedDevice:$UseConnectedDevice
    Invoke-DemoApkBuildAndInstall -RootPath $root -Serial $serial -Flavor $Flavor -SkipBuild:$SkipBuild

    # 2. Screenshot Flow
    if ($doSS) {
        Write-Host "`n[1/2] Generazione screenshot (Vertical & Square)..." -ForegroundColor Yellow
        $tempSS = Join-Path ([System.IO.Path]::GetTempPath()) ("onlytechtake-social-ss-" + [System.Guid]::NewGuid().ToString("N"))
        New-Item -ItemType Directory -Path $tempSS | Out-Null

        $pkgSuffix = if ($Flavor -eq "Demo") { ".demo" } else { "" }
        $packageName = "com.onlytechtake.repairapp$pkgSuffix.debug"
        $devicePath = "/sdcard/Android/data/$packageName/files/social-campaign"

        Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-rf", $devicePath)
        Invoke-DemoScreenshotTest -Serial $serial -TestClass "com.onlytechtake.repairapp.demo.SocialCampaignScreenshotTest" -Flavor $Flavor
        Pull-DeviceDirectory -Serial $serial -DevicePath $devicePath -TargetRoot $tempSS -PulledDirectoryName "social-campaign"

        $vDir = Join-Path $socialAssetsDir "vertical"; $sDir = Join-Path $socialAssetsDir "square"
        if (Test-Path $vDir) { Remove-Item $vDir -Recurse -Force }; New-Item -ItemType Directory -Path $vDir | Out-Null
        if (Test-Path $sDir) { Remove-Item $sDir -Recurse -Force }; New-Item -ItemType Directory -Path $sDir | Out-Null

        Get-ChildItem -Path $tempSS -Filter "*.png" | ForEach-Object {
            Convert-ToStoryPng -SourcePath $_.FullName -TargetPath (Join-Path $vDir $_.Name)
            Convert-ToSquarePng -SourcePath $_.FullName -TargetPath (Join-Path $sDir $_.Name)
        }
        Remove-Item $tempSS -Recurse -Force
        Write-Host "Screenshot salvati." -ForegroundColor Green
    }

    # 3. Video Flow
    if ($doVideo) {
        Write-Host "`n[2/2] Registrazione video demo continuo..." -ForegroundColor Yellow
        $recorderScript = Join-Path $PSScriptRoot "record_demo_device.ps1"
        $recorderParams = @{
            Flavor = $Flavor
            OutputFile = "demo-social-campaign.mp4"
            OutputRoot = $socialAssetsDir
            SkipBuild = $true
            StartImmediately = $true
            ScenarioId = "social-campaign"
            AvdName = $AvdName
        }
        if ($UseConnectedDevice) { $recorderParams["UseConnectedDevice"] = $true }
        & $recorderScript @recorderParams
    }

} finally {
    Restore-DeviceGlobalSettings
}

Write-Host "`n--- Asset Pronti in: $socialAssetsDir ---" -ForegroundColor Green
