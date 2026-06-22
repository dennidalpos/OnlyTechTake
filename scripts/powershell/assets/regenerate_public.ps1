# Script per rigenerare solo asset pubblici (Full e Demo)
$ErrorActionPreference = "Stop"

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
    powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "create_demo_assets.ps1") -AssetProfile $p.id -Flavor $p.flavor -UseConnectedDevice -NonInteractive -SkipBuild
}

Write-Host "`nRigenerazione asset pubblici completata!" -ForegroundColor Green
