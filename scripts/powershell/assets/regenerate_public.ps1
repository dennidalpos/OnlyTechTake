# Script per rigenerare solo asset pubblici (Full e Demo)
$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$assetDir = Join-Path $root "asset"

# 1. Cleanup locale
if (Test-Path $assetDir) {
    Write-Host "Pulizia vecchi asset in $assetDir..." -ForegroundColor Yellow
    Remove-Item -Path $assetDir -Recurse -Force
}

# Cleanup destinazioni pubbliche per evitare file orfani (vecchi nomi o schermate rimosse)
$publicPaths = @(
    "landing/assets/screenshots",
    "marketing/screenshots/play-store/pro",
    "marketing/screenshots/play-store/demo"
)

foreach ($p in $publicPaths) {
    $fullPath = Join-Path $root $p
    if (Test-Path $fullPath) {
        Write-Host "Pulizia destinazione pubblica: $p" -ForegroundColor Gray
        Get-ChildItem -Path $fullPath -Filter "*.png" | Remove-Item -Force
        Get-ChildItem -Path $fullPath -Filter "*.mp4" | Remove-Item -Force
    }
}

$profiles = @(
    @{ id = "workflow-setup"; flavor = "Full" },
    @{ id = "store-pro"; flavor = "Full" },
    @{ id = "store-demo"; flavor = "Demo" },
    @{ id = "social-pro"; flavor = "Full" },
    @{ id = "social-campaign"; flavor = "Full" },
    @{ id = "operator-intake"; flavor = "Full" },
    @{ id = "workflow-management"; flavor = "Full" },
    @{ id = "workflow-admin-templates"; flavor = "Full" },
    @{ id = "workflow-backup"; flavor = "Full" },
    @{ id = "social-stories"; flavor = "Full" }
)

# Assicuriamoci che l'app e i test siano buildati
Write-Host "Build preliminare APK (App + Test)..." -ForegroundColor Cyan
./gradlew :app:assembleFullDebug :app:assembleFullDebugAndroidTest --no-configuration-cache
./gradlew :app:assembleDemoDebug :app:assembleDemoDebugAndroidTest --no-configuration-cache

foreach ($p in $profiles) {
    Write-Host "`n=== Rigenerazione Profilo: $($p.id) ($($p.flavor)) ===" -ForegroundColor Cyan
    # Screenshot
    powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "create_demo_assets.ps1") -AssetProfile $p.id -Flavor $p.flavor -UseConnectedDevice -NonInteractive -SkipBuild

    # Video (solo per profili selezionati)
    if ($p.id -eq "operator-intake") {
        Write-Host "Registrazione video per $($p.id)..." -ForegroundColor Yellow
        powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "record_demo_device.ps1") -AssetProfile $p.id -Flavor $p.flavor -UseConnectedDevice -NonInteractive -SkipBuild
    }
}

Write-Host "`nRigenerazione e distribuzione asset completata!" -ForegroundColor Green
