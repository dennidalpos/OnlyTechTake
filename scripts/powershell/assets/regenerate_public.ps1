# Script per rigenerare solo asset pubblici (Full e Demo)
$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$reportsDir = Join-Path $root "reports/generated-assets"

# 1. Cleanup
if (Test-Path $reportsDir) {
    Write-Host "Pulizia vecchi asset in $reportsDir..." -ForegroundColor Yellow
    Remove-Item -Path $reportsDir -Recurse -Force
}

$profiles = @(
    @{ id = "store-pro"; flavor = "Full" },
    @{ id = "store-demo"; flavor = "Demo" },
    @{ id = "social-pro"; flavor = "Full" },
    @{ id = "social-campaign"; flavor = "Full" },
    @{ id = "operator-intake"; flavor = "Full" },
    @{ id = "social-stories"; flavor = "Full" }
)

foreach ($p in $profiles) {
    Write-Host "`n=== Rigenerazione Profilo: $($p.id) ($($p.flavor)) ===" -ForegroundColor Cyan
    # Screenshot
    powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "create_demo_assets.ps1") -AssetProfile $p.id -Flavor $p.flavor -UseConnectedDevice -NonInteractive -SkipBuild

    # Video (solo per profili selezionati)
    if ($p.id -eq "operator-intake") {
        Write-Host "Registrazione video per $($p.id)..." -ForegroundColor Yellow
        powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "record_demo_device.ps1") -AssetProfile $p.id -Flavor $p.flavor -UseConnectedDevice -NonInteractive -SkipBuild -Platform play-store -Variant vertical
    }
}

# 2. Sincronizzazione Landing Page e GitHub (Marketing)
Write-Host "`n=== Sincronizzazione Asset ===" -ForegroundColor Cyan

$sourceStorePro = Join-Path $reportsDir "full/store-pro/play-store/vertical"
$landingScreenshots = Join-Path $root "landing/assets/screenshots"
$marketingScreenshots = Join-Path $root "marketing/screenshots/play-store/pro"

if (Test-Path $sourceStorePro) {
    Write-Host "Copia screenshot Pro in landing e marketing..." -ForegroundColor Gray
    if (-not (Test-Path $landingScreenshots)) { New-Item -ItemType Directory -Path $landingScreenshots -Force }
    if (-not (Test-Path $marketingScreenshots)) { New-Item -ItemType Directory -Path $marketingScreenshots -Force }
    Copy-Item -Path "$sourceStorePro\*.png" -Destination $landingScreenshots -Force
    Copy-Item -Path "$sourceStorePro\*.png" -Destination $marketingScreenshots -Force
}

# Video (se presenti)
$sourceVideos = Join-Path $reportsDir "full/operator-intake/play-store/vertical"
$landingVideos = Join-Path $root "landing/assets/videos"
$marketingVideos = Join-Path $root "marketing/videos/play-store"

if (Test-Path $sourceVideos) {
    Write-Host "Copia video in landing e marketing..." -ForegroundColor Gray
    if (-not (Test-Path $landingVideos)) { New-Item -ItemType Directory -Path $landingVideos -Force }
    if (-not (Test-Path $marketingVideos)) { New-Item -ItemType Directory -Path $marketingVideos -Force }

    $videoFile = Get-ChildItem -Path $sourceVideos -Filter "*.mp4" | Select-Object -First 1
    if ($videoFile) {
        Copy-Item -Path $videoFile.FullName -Destination (Join-Path $landingVideos "intake-flow.mp4") -Force
        Copy-Item -Path $videoFile.FullName -Destination (Join-Path $marketingVideos "pro-workflow.mp4") -Force
    }
}

Write-Host "`nRigenerazione e sincronizzazione asset completata!" -ForegroundColor Green
