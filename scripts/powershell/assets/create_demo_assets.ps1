<#
.SYNOPSIS
  Genera asset screenshot/video da profili JSON flavor-aware.

.USAGE
  Generazione Pro su device fisico:
    .\scripts\powershell\assets\create_demo_assets.ps1 -UseConnectedDevice -AssetProfile store-pro

  Generazione specifica:
    .\scripts\powershell\assets\create_demo_assets.ps1 -AssetProfile social-pro -Platform instagram -Variant vertical

.DESCRIPTION
  Lo script centralizza la generazione di asset visuali basandosi su profili JSON.
  Esegue gli scenari strumentati, processa le immagini secondo la piattaforma
  (verticale, quadrata, landscape) e monta i video tramite ffmpeg.
  Tutti gli output sono in reports/generated-assets/<AssetProfile>/<Platform>/<Variant>.
  Gli asset pubblici principali usano Full; Demo resta limitata ai profili Play Store.
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
    Write-Host "`n--- OnlyTechTake Demo Asset Creator ---" -ForegroundColor Cyan
    Write-Host "Genera screenshot e video demo basati su profili asset."
}

# Risoluzione AssetProfile
if (-not $AssetProfile -and $AssetId) {
    $AssetProfile = $AssetId
}

$profilesDir = Join-Path $root "demo/assets/profiles"
$availableProfiles = Get-ChildItem -Path $profilesDir -Filter "*.asset.json" | ForEach-Object { $_.BaseName.Replace(".asset", "") }

# Selezione interattiva se mancante
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

if (-not $AssetProfile) {
    throw "AssetProfile mancante."
}

# Caricamento configurazione profilo
$profilePath = Join-Path $profilesDir "$AssetProfile.asset.json"
if (-not (Test-Path $profilePath)) {
    throw "Profilo asset non trovato: $profilePath"
}
$config = Get-Content -Path $profilePath -Raw | ConvertFrom-Json

# Sovrascrittura flavor da config se non specificato; se specificato, deve combaciare.
if ($config.flavor) {
    if ($PSBoundParameters.ContainsKey('Flavor') -eq $false) {
        $Flavor = $config.flavor
    } elseif ($Flavor -ne $config.flavor) {
        throw "Il profilo '$AssetProfile' richiede Flavor '$($config.flavor)', non '$Flavor'."
    }
}

$packageNameSuffix = switch ($Flavor) {
    "Full" { "" }
    "Demo" { ".demo" }
    "Showcase" { ".showcase" }
}
$packageName = "com.onlytechtake.repairapp$($packageNameSuffix).debug"

# Post-processing placeholders
if ($config.deviceDirectory) {
    $config.deviceDirectory = $config.deviceDirectory.Replace("{PACKAGE_NAME}", $packageName)
}

# Output Root
if (-not $OutputRoot) {
    $OutputRoot = Join-Path $root "reports/generated-assets/$AssetProfile"
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

    foreach ($case in $activeCases) {
        $src = if ($case.sourceDirectory -eq ".") { $pulledRoot } else { Join-Path $pulledRoot $case.sourceDirectory }

        if ($case.expectedFiles) {
            $missingFiles = @(
                $case.expectedFiles |
                    Where-Object { -not (Test-Path (Join-Path $src $_)) }
            )
            if ($missingFiles.Count -gt 0) {
                throw "Acquisizione incompleta per '$($case.id)': file mancanti: $($missingFiles -join ', ')"
            }
        }

        # Mappatura output
        $outputs = @()
        if ($case.outputs) {
            $outputs = @($case.outputs)
        } else {
            # Fallback legacy mapping
            if ($case.targetDirectory) { $outputs += [PSCustomObject]@{ platform = "play-store"; variant = "vertical"; subDir = "." } }
            if ($case.squareDirectory) { $outputs += [PSCustomObject]@{ platform = "social"; variant = "square"; subDir = "." } }
            if ($case.landscapeDirectory) { $outputs += [PSCustomObject]@{ platform = "presentation"; variant = "landscape"; subDir = "." } }
        }

        # Filtro per parametri riga di comando
        if ($Platform) { $outputs = @($outputs | Where-Object { $_.platform -eq $Platform }) }
        if ($Variant) { $outputs = @($outputs | Where-Object { $_.variant -eq $Variant }) }

        foreach ($out in $outputs) {
            $sub = if ($out.subDir -and $out.subDir -ne ".") { $out.subDir } else { "" }
            $dest = Join-Path $OutputRoot "$($out.platform)/$($out.variant)/$sub"
            Write-Host "Generazione in: $(Get-RepoRelativePath -FullPath $dest -RootPath $root)" -ForegroundColor Gray
            if (-not (Test-Path $dest)) { New-Item -ItemType Directory -Path $dest -Force | Out-Null }

            $items = Get-ChildItem -Path $src -Filter "*.png"
            foreach ($item in $items) {
                $p = $item.FullName
                $n = $item.Name
                $targetFile = Join-Path $dest $n
                if ($out.variant -eq "vertical") {
                    Convert-ToStoryPng -SourcePath $p -TargetPath $targetFile
                } elseif ($out.variant -eq "square") {
                    Convert-ToSquarePng -SourcePath $p -TargetPath $targetFile
                } elseif ($out.variant -eq "landscape") {
                    Convert-ToLandscapePng -SourcePath $p -TargetPath $targetFile
                } else {
                    Copy-Item -Path $p -Destination $targetFile -Force
                }
            }
        }
    }

    # Video Automatizzato
    if (-not $SkipVideo -and $config.storyJson) {
        $videoDest = Join-Path $OutputRoot "videos/automated"
        New-Item -ItemType Directory -Path $videoDest -Force | Out-Null
        $storyPath = Join-Path $root $config.storyJson
        if ((Test-Path $storyPath) -and (Get-Command "ffmpeg" -ErrorAction SilentlyContinue)) {
            Write-Host "Generazione video montato..." -ForegroundColor Gray
            $story = Get-Content -Path $storyPath -Raw | ConvertFrom-Json
            $slidesDir = Join-Path $videoDest "temp_slides"; New-Item -ItemType Directory -Path $slidesDir -Force | Out-Null
            $concatPath = Join-Path $videoDest "slides.txt"
            try {
                $idx = 1
                foreach ($scene in $story.scenes) {
                    $sImg = Join-Path $pulledRoot $scene.file
                    if (-not (Test-Path $sImg)) { continue }
                    $slide = Join-Path $slidesDir ("slide_{0:D2}.png" -f $idx)
                    New-CaptionedSlide -SourcePath $sImg -Caption $scene.caption -TargetPath $slide -Width $story.width -Height $story.height
                    Add-Content -Path $concatPath -Value ("file '{0}'" -f (Convert-ToConcatPath $slide))
                    Add-Content -Path $concatPath -Value ("duration {0}" -f ([string]$scene.durationSeconds).Replace(",", "."))
                    $idx++
                }
                Add-Content -Path $concatPath -Value ("file '{0}'" -f (Convert-ToConcatPath (Join-Path $slidesDir ("slide_{0:D2}.png" -f ($idx - 1)))))
                Invoke-Checked -FilePath "ffmpeg" -Arguments @("-y", "-f", "concat", "-safe", "0", "-i", $concatPath, "-vcodec", "libx264", "-pix_fmt", "yuv420p", "-movflags", "+faststart", (Join-Path $videoDest $story.outputFile))
            } finally {
                Remove-Item $slidesDir -Recurse -Force; Remove-Item $concatPath -Force
            }
        }
    }
} finally {
    if (Test-Path $pulledRoot) { Remove-Item $pulledRoot -Recurse -Force }
}

Restore-DeviceGlobalSettings
Write-Host "`nAsset completati in: $OutputRoot" -ForegroundColor Green
