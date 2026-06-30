# Script per rigenerare asset statici (PNG, ICO, Web) dai vettoriali aggiornati
# Richiede ImageMagick o Inkscape nel path per la conversione automatica se disponibile.
# In assenza di tool, elenca le operazioni necessarie per il designer.

$ErrorActionPreference = "Continue"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path

Write-Host "--- OnlyTechTake Static Asset Generator ---" -ForegroundColor Cyan

$sourceLogo = Join-Path $root "marketing/registered-assets/brand/identity/onlytechtake-logo.svg"
$sourceMark = Join-Path $root "landing/assets/brand/onlytechtake-mark.svg"

$targets = @(
    # Favicons & Web
    @{ src = $sourceMark; dest = "landing/assets/icons/favicon.svg" },
    @{ src = $sourceMark; dest = "landing/assets/icons/favicon-192.png"; size = "192x192" },
    @{ src = $sourceMark; dest = "landing/assets/icons/favicon-512.png"; size = "512x512" },
    @{ src = $sourceMark; dest = "landing/assets/icons/favicon.ico"; size = "48x48" },

    # Social & Marketing
    @{ src = $sourceLogo; dest = "marketing/registered-assets/brand/social/github-cover.png"; size = "1200x600" },
    @{ src = $sourceLogo; dest = "marketing/registered-assets/brand/social/github-social-preview.png"; size = "1280x640" },

    # App Preview
    @{ src = $sourceMark; dest = "landing/assets/icons/app-icon-preview.png"; size = "512x512"; bg = "#0D0D0D" }
)

Write-Host "`nVerifica sorgenti..." -ForegroundColor Yellow
if (-not (Test-Path $sourceLogo)) { Write-Warning "Sorgente logo non trovata: $sourceLogo" }
if (-not (Test-Path $sourceMark)) { Write-Warning "Sorgente mark non trovata: $sourceMark" }

Write-Host "`nTooling check..." -ForegroundColor Yellow
$magick = Get-Command magick -ErrorAction SilentlyContinue
if (-not $magick) {
    $commonPaths = @(
        "C:\Program Files\ImageMagick-*\magick.exe",
        "C:\Program Files (x86)\ImageMagick-*\magick.exe"
    )
    foreach ($p in $commonPaths) {
        $found = Get-ChildItem -Path $p -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            $magick = $found.FullName
            break
        }
    }
}
$inkscape = Get-Command inkscape -ErrorAction SilentlyContinue

if ($magick) {
    Write-Host "ImageMagick rilevato ($magick). Avvio conversione..." -ForegroundColor Green
    foreach ($t in $targets) {
        $destPath = Join-Path $root $t.dest
        $dir = Split-Path $destPath
        if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }

        Write-Host "Generazione: $($t.dest)" -ForegroundColor Gray
        if ($t.dest.EndsWith(".svg")) {
            Copy-Item -Path $t.src -Destination $destPath -Force
        } else {
            # Se e' la preview app, aggiungiamo il background Charcoal
            $bgParam = if ($t.bg) { @("-background", $t.bg) } else { @("-background", "none") }
            & $magick $bgParam -size $t.size $t.src $destPath
        }
    }
} else {
    Write-Host "`nATTENZIONE: ImageMagick non trovato nel PATH." -ForegroundColor Red
    Write-Host "Le seguenti conversioni devono essere eseguite manualmente (Export da Illustrator/Inkscape):" -ForegroundColor White
    foreach ($t in $targets) {
        Write-Host "- $($t.dest) (da $($t.src), Dimensione: $($t.size))"
    }
}

Write-Host "`nWorkflow completato." -ForegroundColor Green
