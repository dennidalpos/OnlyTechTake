<#
.SYNOPSIS
  Genera tutti gli asset demo: screenshot verticali e video MP4 montati.

.USAGE
  .\scripts\windows\create_demo_assets.ps1
  .\scripts\windows\create_demo_assets.ps1 -UseConnectedDevice -SkipVideo

.DESCRIPTION
  Lo script unifica la generazione di screenshot e video. Esegue gli scenari
  strumentati demo, processa le immagini per il marketing e, se ffmpeg è presente,
  monta i video MP4 basati sugli story JSON configurati nello script.
#>

param(
    [string]$AvdName = "OnlyTechTakeDemo",
    [switch]$UseConnectedDevice,
    [switch]$SkipBuild,
    [switch]$SkipVideo,
    [string]$OutputRoot,
    [string]$AssetId,
    [string]$CaseId
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

$environmentScript = Join-Path $PSScriptRoot "..\support\android_env.ps1"
if (Test-Path $environmentScript) {
    . $environmentScript
    Set-AndroidCliEnvironment | Out-Null
}

$demoSupportScript = Join-Path $PSScriptRoot "support\android_demo.ps1"
if (Test-Path $demoSupportScript) {
    . $demoSupportScript
} else {
    throw "Helper demo Android non trovato: $demoSupportScript"
}

if (-not $OutputRoot) {
    $OutputRoot = Join-Path $root "reports/demo-assets"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputRoot)) {
    $OutputRoot = Join-Path $root $OutputRoot
}

# Configurazioni asset
$assetsConfig = @(
    [PSCustomObject]@{
        Id = "operator-intake"
        TestClass = "com.onlytechtake.repairapp.demo.OperatorIntakeDemoScreenshotTest"
        DeviceDirectory = "/sdcard/Android/data/com.onlytechtake.repairapp.debug/files/demo-screenshots"
        PulledDirectoryName = "demo-screenshots"
        StoryJson = "demo/video/operator-intake.story.json"
        Cases = @(
            [PSCustomObject]@{
                Id = "full"
                SourceDirectory = "."
                TargetDirectory = "screenshots/01-operatore-banco"
                ExpectedFiles = @(
                    "01-login.png", "02-home.png", "03-new-customer.png",
                    "04-category-problem.png", "05-signature-capture.png",
                    "06-order-created.png", "07-whatsapp-preview.png",
                    "08-in-progress.png", "09-diagnosis-result.png",
                    "10-pdf-documents.png", "11-ready-for-pickup.png",
                    "12-closed-timeline.png"
                )
            }
        )
    },
    [PSCustomObject]@{
        Id = "social-stories"
        TestClass = "com.onlytechtake.repairapp.demo.SocialStoryDemoScreenshotTest"
        DeviceDirectory = "/sdcard/Android/data/com.onlytechtake.repairapp.debug/files/demo-social-screenshots"
        PulledDirectoryName = "demo-social-screenshots"
        StoryJson = $null
        Cases = @(
            [PSCustomObject]@{
                Id = "order-intake"
                TestMethod = "capturesOrderIntakeToClosureStories"
                SourceDirectory = "01-commessa-nuovo-cliente-chiusura"
                TargetDirectory = "screenshots/02-commessa-nuovo-cliente-chiusura"
            },
            [PSCustomObject]@{
                Id = "config"
                TestMethod = "capturesCategoryAndTemplateStories"
                SourceDirectory = "02-categorie-template"
                TargetDirectory = "screenshots/03-categorie-template"
            },
            [PSCustomObject]@{
                Id = "customers"
                TestMethod = "capturesPrivateAndBusinessCustomerStories"
                SourceDirectory = "03-clienti-privato-azienda"
                TargetDirectory = "screenshots/04-clienti-privato-azienda"
            },
            [PSCustomObject]@{
                Id = "maintenance"
                TestMethod = "capturesMaintenanceStories"
                SourceDirectory = "05-manutenzione-backup"
                TargetDirectory = "screenshots/05-manutenzione-backup"
            }
        )
    }
)

# Selezione interattiva se non specificato AssetId o CaseId
if (-not $AssetId -and -not $CaseId) {
    Write-Host "`n--- Selezione Flusso Demo ---" -ForegroundColor Yellow
    Write-Host "0. Tutto (default)"
    $i = 1
    $menuItems = @()
    foreach ($asset in $assetsConfig) {
        if ($asset.Id -eq "social-stories") {
            foreach ($case in $asset.Cases) {
                Write-Host "$i. Social: $($case.Id) ($($case.TestMethod))"
                $menuItems += [PSCustomObject]@{ Asset = $asset; Case = $case }
                $i++
            }
        } else {
            Write-Host "$i. $($asset.Id)"
            $menuItems += [PSCustomObject]@{ Asset = $asset; Case = $null }
            $i++
        }
    }

    $choice = Read-Host "`nScegli il flusso da eseguire [0-$($i-1)]"
    if ($choice -ne "0" -and $choice -match "^\d+$" -and [int]$choice -lt $i) {
        $selected = $menuItems[[int]$choice - 1]
        $AssetId = $selected.Asset.Id
        $CaseId = if ($selected.Case) { $selected.Case.Id } else { $null }
    }
}

function Process-VideoGeneration {

    param($Config, $PulledPath, $AssetOutputRoot)

    $storyPath = Join-Path $root $Config.StoryJson
    if (-not (Test-Path $storyPath)) {
        Write-Warning "Configurazione video non trovata: $storyPath"
        return
    }

    if ($null -eq (Get-Command "ffmpeg" -ErrorAction SilentlyContinue)) {
        Write-Warning "ffmpeg non trovato. Salto generazione video per $($Config.Id)."
        return
    }

    Write-Host "Generazione video per $($Config.Id)..." -ForegroundColor Cyan
    $story = Get-Content -Path $storyPath -Raw | ConvertFrom-Json
    $videoDir = Join-Path $AssetOutputRoot "videos/automated"
    Reset-ManagedAssetDirectory -Path $videoDir
    $slidesDir = Join-Path $videoDir "temp_slides"
    New-Item -ItemType Directory -Path $slidesDir -Force | Out-Null

    $concatPath = Join-Path $videoDir "slides.txt"
    if (Test-Path $concatPath) { Remove-Item $concatPath }

    try {
        $index = 1
        foreach ($scene in $story.scenes) {
            $sourceImg = Join-Path $PulledPath $scene.file
            if (-not (Test-Path $sourceImg)) { continue }

            $slidePath = Join-Path $slidesDir ("slide_{0:D2}.png" -f $index)
            New-CaptionedSlide -SourcePath $sourceImg -Caption $scene.caption -TargetPath $slidePath -Width $story.width -Height $story.height

            Add-Content -Path $concatPath -Value ("file '{0}'" -f (Convert-ToConcatPath $slidePath))
            Add-Content -Path $concatPath -Value ("duration {0}" -f ([string]$scene.durationSeconds).Replace(",", "."))
            $index++
        }
        # Ultimo frame per evitare troncamenti ffmpeg
        $lastSlide = Join-Path $slidesDir ("slide_{0:D2}.png" -f ($index - 1))
        Add-Content -Path $concatPath -Value ("file '{0}'" -f (Convert-ToConcatPath $lastSlide))

        $outputVideo = Join-Path $videoDir $story.outputFile
        Invoke-Checked -FilePath "ffmpeg" -Arguments @(
            "-y", "-f", "concat", "-safe", "0", "-i", $concatPath,
            "-vcodec", "libx264", "-preset", "ultrafast", "-crf", "22",
            "-pix_fmt", "yuv420p", "-movflags", "+faststart", $outputVideo
        )

        Write-Host "Video creato: $outputVideo" -ForegroundColor Green
    } finally {
        if (Test-Path $slidesDir) { Remove-Item $slidesDir -Recurse -Force }
        if (Test-Path $concatPath) { Remove-Item $concatPath -Force }
    }
}

function Reset-ManagedAssetDirectory {
    param([Parameter(Mandatory = $true)][string]$Path)

    Assert-PathInsideRoot -Path $Path -RootPath $root
    if (Test-Path $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
    New-Item -ItemType Directory -Path $Path -Force | Out-Null
}

trap {
    Restore-DeviceGlobalSettings
    throw $_
}

Assert-PathInsideRoot -Path $OutputRoot -RootPath $root
if (-not $AssetId) {
    foreach ($managedDirectory in @("screenshots", "videos/automated")) {
        $managedPath = Join-Path $OutputRoot $managedDirectory
        Reset-ManagedAssetDirectory -Path $managedPath
    }
    if (-not (Test-Path $OutputRoot)) {
        New-Item -ItemType Directory -Path $OutputRoot | Out-Null
    }
} elseif (-not (Test-Path $OutputRoot)) {
    New-Item -ItemType Directory -Path $OutputRoot | Out-Null
}

$serial = Resolve-DemoDeviceSerial -AndroidHome $env:ANDROID_HOME -AvdName $AvdName -UseConnectedDevice:$UseConnectedDevice
Invoke-DemoApkBuildAndInstall -RootPath $root -Serial $serial -SkipBuild:$SkipBuild

foreach ($asset in $assetsConfig) {
    if ($AssetId -and $AssetId -ne $asset.Id) { continue }

    Write-Host "`n--- Elaborazione Asset: $($asset.Id) ---" -ForegroundColor Cyan

    $testTarget = $asset.TestClass
    $activeCases = $asset.Cases

    if ($CaseId) {
        $case = $asset.Cases | Where-Object { $_.Id -eq $CaseId }
        if ($case) {
            if ($case.TestMethod) {
                $testTarget = "$($asset.TestClass)#$($case.TestMethod)"
            }
            $activeCases = @($case)
        }
    }

    # Esecuzione Test
    Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-rf", $asset.DeviceDirectory)
    $testTargets = @(
        if (-not $CaseId) {
            $activeCases |
                Where-Object { $_.TestMethod } |
                ForEach-Object { "$($asset.TestClass)#$($_.TestMethod)" }
        }
    )
    if ($testTargets.Count -eq 0) {
        $testTargets = @($testTarget)
    }
    foreach ($target in $testTargets) {
        Invoke-DemoScreenshotTest -Serial $serial -TestClass $target
    }

    # Recupero e Processamento Screenshot
    $pulledRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("onlytechtake-assets-" + [System.Guid]::NewGuid().ToString("N"))
    try {
        Pull-DeviceDirectory -Serial $serial -DevicePath $asset.DeviceDirectory -TargetRoot $pulledRoot -PulledDirectoryName $asset.PulledDirectoryName

        foreach ($case in $activeCases) {
            $sourceCase = if ($case.SourceDirectory -eq ".") { $pulledRoot } else { Join-Path $pulledRoot $case.SourceDirectory }
            $targetCase = Join-Path $OutputRoot $case.TargetDirectory
            Reset-ManagedAssetDirectory -Path $targetCase

            if (Test-Path $sourceCase) {
                Get-ChildItem -Path $sourceCase -Filter "*.png" | ForEach-Object {
                    $targetPath = Join-Path $targetCase $_.Name
                    Convert-ToStoryPng -SourcePath $_.FullName -TargetPath $targetPath
                }
            } else {
                Write-Warning "Cartella sorgente non trovata: $sourceCase"
            }
        }

        # Generazione Video se previsto
        if (-not $SkipVideo -and $asset.StoryJson -and (-not $CaseId)) {
            Process-VideoGeneration -Config $asset -PulledPath $pulledRoot -AssetOutputRoot $OutputRoot
        }
    } finally {
        if (Test-Path $pulledRoot) { Remove-Item $pulledRoot -Recurse -Force }
    }
}

Restore-DeviceGlobalSettings
Write-Host "`nAsset demo generati in: $OutputRoot" -ForegroundColor Green
