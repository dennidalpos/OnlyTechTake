param(
    [string]$Locale = ""
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$assetDir = Join-Path $root "asset"

# Carica helper
. (Join-Path $PSScriptRoot "..\support\android_env.ps1")
Set-AndroidCliEnvironment | Out-Null
. (Join-Path $PSScriptRoot "..\support\android_demo.ps1")

# 1. Rigenerazione Brand Identity (solo se non specifico locale)
if ([string]::IsNullOrWhiteSpace($Locale)) {
    Write-Host "Rigenerazione asset brand identity..." -ForegroundColor Cyan
    Invoke-Checked -FilePath "powershell" -Arguments @("-ExecutionPolicy", "Bypass", "-File", (Join-Path $PSScriptRoot "generate_static_brands.ps1"))
}

# 2. Cleanup locale
if ([string]::IsNullOrWhiteSpace($Locale) -and (Test-Path $assetDir)) {
    Write-Host "Pulizia vecchi asset in $assetDir..." -ForegroundColor Yellow
    Remove-Item -Path $assetDir -Recurse -Force
}

# Cleanup destinazioni pubbliche per evitare file orfani (vecchi nomi o schermate rimosse)
$publicPaths = @(
    "landing/assets/screenshots",
    "marketing/screenshots/play-store/pro",
    "marketing/screenshots/play-store/demo",
    "marketing/screenshots/social",
    "marketing/screenshots/workflows"
)

if ([string]::IsNullOrWhiteSpace($Locale)) {
    foreach ($p in $publicPaths) {
        $fullPath = Join-Path $root $p
        if (Test-Path $fullPath) {
            Write-Host "Pulizia destinazione pubblica: $p" -ForegroundColor Gray
            Get-ChildItem -Path $fullPath -Filter "*.png" -Recurse | Remove-Item -Force
            Get-ChildItem -Path $fullPath -Filter "*.mp4" -Recurse | Remove-Item -Force
        }
    }
}

# 3. Discovery Dinamico dei Profili
$profilesDir = Join-Path $root "demo/assets/profiles"
$profiles = Get-ChildItem -Path $profilesDir -Filter "*.asset.json" | ForEach-Object {
    $config = Get-Content $_.FullName -Raw | ConvertFrom-Json
    @{
        id = $config.id
        flavor = if ($config.flavor) { $config.flavor } else { "Full" }
        skipTest = [bool]$config.skipTest
    }
}

# Ordine consigliato per dipendenze Mixed Management (social-posts deve venire dopo social-campaign)
$priorityIds = @("workflow-setup", "social-campaign", "social-posts", "store-pro")
$profiles = $profiles | Sort-Object {
    $idx = [array]::IndexOf($priorityIds, $_.id)
    if ($idx -lt 0) { 100 } else { $idx }
}

# Definiamo le lingue.
$targetLocales = if ([string]::IsNullOrWhiteSpace($Locale)) { @("en", "it") } else { @($Locale) }

# Assicuriamoci che l'app e i test siano buildati
Write-Host "Salto build preliminare APK (Assunto già fatto)..." -ForegroundColor Yellow

foreach ($locale in $targetLocales) {
    Write-Host "`n>>>>>> LINGUA: $($locale.ToUpper()) <<<<<<" -ForegroundColor White -BackgroundColor DarkBlue

    foreach ($p in $profiles) {
        Write-Host "`n=== Rigenerazione Profilo: $($p.id) ($($p.flavor)) [$locale] ===" -ForegroundColor Cyan

        # Ogni flusso di acquisizione deve avere il suo video (se non è un profilo di post-elaborazione)
        $skipTest = $p.skipTest

        if (-not $skipTest) {
            Write-Host "Registrazione video per $($p.id)..." -ForegroundColor Yellow
            $videoArgs = @("-ExecutionPolicy", "Bypass", "-File", (Join-Path $PSScriptRoot "record_demo_device.ps1"), "-AssetProfile", $p.id, "-Flavor", $p.flavor, "-UseConnectedDevice", "-NonInteractive", "-SkipBuild", "-Locale", $locale)
            if ($p.id -like "workflow-*") {
                $videoArgs += "-WhatsAppOptimized"
            }

            Invoke-Checked -FilePath "powershell" -Arguments $videoArgs

            Write-Host "Estrazione screenshot per $($p.id) (SkipTest)..." -ForegroundColor Gray
            Invoke-Checked -FilePath "powershell" -Arguments @("-ExecutionPolicy", "Bypass", "-File", (Join-Path $PSScriptRoot "create_demo_assets.ps1"), "-AssetProfile", $p.id, "-Flavor", $p.flavor, "-UseConnectedDevice", "-NonInteractive", "-SkipBuild", "-SkipTest", "-Locale", $locale)
        } else {
            Write-Host "Generazione asset post-elaborati per $($p.id) (SkipTest)..." -ForegroundColor Yellow
            Invoke-Checked -FilePath "powershell" -Arguments @("-ExecutionPolicy", "Bypass", "-File", (Join-Path $PSScriptRoot "create_demo_assets.ps1"), "-AssetProfile", $p.id, "-Flavor", $p.flavor, "-UseConnectedDevice", "-NonInteractive", "-SkipBuild", "-SkipTest", "-Locale", $locale)
        }
    }
}

# 3. Cleanup finale asset temporanei
if (Test-Path $assetDir) {
    Write-Host "Pulizia finale asset temporanei..." -ForegroundColor Yellow
    Remove-Item -Path $assetDir -Recurse -Force
}

Write-Host "`nRigenerazione e distribuzione asset completata!" -ForegroundColor Green
