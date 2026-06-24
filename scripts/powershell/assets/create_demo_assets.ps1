<#
.SYNOPSIS
  Genera asset screenshot/video da profili JSON flavor-aware.

.USAGE
  Generazione Pro su device fisico:
    .\scripts\powershell\assets\create_demo_assets.ps1 -UseConnectedDevice -AssetProfile store-pro

.DESCRIPTION
  Lo script centralizza la generazione di asset visuali basandosi su profili JSON.
  Esegue gli scenari strumentati, processa le immagini e distribuisce gli asset
  nelle cartelle marketing e landing page.
  Tutti gli output originali sono in asset/<AssetProfile>.
#>

param(
    [string]$AvdName = "OnlyTechTakeDemo",
    [switch]$UseConnectedDevice,
    [ValidateSet("Full", "Demo", "Showcase")]
    [string]$Flavor = "Full",
    [string]$AssetProfile,
    [string]$Platform,
    [string]$Variant,
    [switch]$SkipBuild,
    [switch]$SkipVideo,
    [string]$OutputRoot,
    [string]$AssetId,
    [string]$CaseId,
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
    Write-Host "`n--- OnlyTechTake Asset Creator ---" -ForegroundColor Cyan
}

# Risoluzione AssetProfile
if (-not $AssetProfile -and $AssetId) { $AssetProfile = $AssetId }

$profilesDir = Join-Path $root "demo/assets/profiles"
$availableProfiles = Get-ChildItem -Path $profilesDir -Filter "*.asset.json" | ForEach-Object { $_.BaseName.Replace(".asset", "") }

if (-not $AssetProfile -and -not $NonInteractive) {
    Write-Host "`n--- Selezione Profilo Asset ---" -ForegroundColor Yellow
    for ($i = 0; $i -lt $availableProfiles.Count; $i++) {
        Write-Host "$($i + 1). $($availableProfiles[$i])"
    }
    $choice = Read-Host "`nScegli il profilo [1-$($availableProfiles.Count)]"
    if ($choice -match "^\d+$" -and [int]$choice -ge 1 -and [int]$choice -le $availableProfiles.Count) {
        $AssetProfile = $availableProfiles[[int]$choice - 1]
    }
}

if (-not $AssetProfile) { throw "AssetProfile mancante." }

# Caricamento configurazione profilo
$profilePath = Join-Path $profilesDir "$AssetProfile.asset.json"
if (-not (Test-Path $profilePath)) { throw "Profilo asset non trovato: $profilePath" }
$config = Get-Content -Path $profilePath -Raw | ConvertFrom-Json

if ($config.flavor -and ($PSBoundParameters.ContainsKey('Flavor') -eq $false)) {
    $Flavor = $config.flavor
}

$packageNameSuffix = switch ($Flavor) { "Full" { "" }; "Demo" { ".demo" }; "Showcase" { ".showcase" } }
$packageName = "com.onlytechtake.repairapp$($packageNameSuffix).debug"

if ($config.deviceDirectory) {
    $config.deviceDirectory = $config.deviceDirectory.Replace("{PACKAGE_NAME}", $packageName)
}

# Output Root (Gerarchia Orizzontale)
if (-not $OutputRoot) {
    $OutputRoot = Join-Path $root "asset/$AssetProfile"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputRoot)) {
    $OutputRoot = Join-Path $root $OutputRoot
}

# Detect device and build
$serial = Resolve-DemoDeviceSerial -AndroidHome $env:ANDROID_HOME -AvdName $AvdName -UseConnectedDevice:$UseConnectedDevice
Invoke-DemoApkBuildAndInstall -RootPath $root -Serial $serial -Flavor $Flavor -SkipBuild:$SkipBuild

trap { Restore-DeviceGlobalSettings; throw $_ }

# Esecuzione Test
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-rf", $config.deviceDirectory)
$activeCases = @($config.cases)
if ($CaseId) { $activeCases = @($activeCases | Where-Object { $_.id -eq $CaseId }) }

foreach ($case in $activeCases) {
    $targetTest = if ($case.testMethod) { "$($config.testClass)#$($case.testMethod)" } else { $config.testClass }
    Invoke-DemoScreenshotTest -Serial $serial -TestClass $targetTest -Flavor $Flavor
}

# Recupero e Elaborazione
$pulledRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("onlytechtake-assets-" + [System.Guid]::NewGuid().ToString("N"))
try {
    Pull-DeviceDirectory -Serial $serial -DevicePath $config.deviceDirectory -TargetRoot $pulledRoot -PulledDirectoryName $config.pulledDirectoryName

    if (-not (Test-Path $OutputRoot)) { New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null }

    foreach ($case in $activeCases) {
        $src = if ($case.sourceDirectory -eq ".") { $pulledRoot } else { Join-Path $pulledRoot $case.sourceDirectory }

        $items = Get-ChildItem -Path $src -Filter "*.png"
        foreach ($item in $items) {
            $targetFile = Join-Path $OutputRoot $item.Name
            Copy-Item -Path $item.FullName -Destination $targetFile -Force
        }
    }

    # Distribuzione Automatica
    if (-not $NoDistribute) {
        Write-Host "Distribuzione asset in corso..." -ForegroundColor Cyan

        # 1. Marketing (Play Store)
        $marketingPath = switch ($AssetProfile) {
            "store-pro" { "marketing/screenshots/play-store/pro" }
            "store-demo" { "marketing/screenshots/play-store/demo" }
            "social-pro" { "marketing/screenshots/social" }
            default { "marketing/screenshots/workflows/$AssetProfile" }
        }
        $fullMarketingPath = Join-Path $root $marketingPath
        if (-not (Test-Path $fullMarketingPath)) { New-Item -ItemType Directory -Path $fullMarketingPath -Force | Out-Null }
        Copy-Item -Path "$OutputRoot\*.png" -Destination $fullMarketingPath -Force

        # 2. Landing Page (solo per profili principali)
        if ($AssetProfile -eq "store-pro") {
            $landingPath = Join-Path $root "landing/assets/screenshots"
            if (-not (Test-Path $landingPath)) { New-Item -ItemType Directory -Path $landingPath -Force | Out-Null }
            Copy-Item -Path "$OutputRoot\*.png" -Destination $landingPath -Force
            Write-Host "Landing page aggiornata con nuovi screenshot." -ForegroundColor Gray
        }
    }

} finally {
    if (Test-Path $pulledRoot) { Remove-Item $pulledRoot -Recurse -Force }
}

Restore-DeviceGlobalSettings
Write-Host "`nAsset completati e distribuiti." -ForegroundColor Green
