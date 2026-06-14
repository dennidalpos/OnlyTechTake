<#
.SYNOPSIS
  Genera tutti gli asset demo: screenshot verticali e video MP4 montati.

.USAGE
  .\scripts\windows\create_demo_assets.ps1
  .\scripts\windows\create_demo_assets.ps1 -UseConnectedDevice -SkipVideo

.DESCRIPTION
  Lo script unifica la generazione di screenshot e video. Esegue gli scenari
  strumentati demo, processa le immagini per il marketing e, se ffmpeg è presente,
  monta i video MP4 basati sui file .story.json trovati nella cartella demo.
#>

param(
    [string]$AvdName = "OnlyTechTakeDemo",
    [switch]$UseConnectedDevice,
    [switch]$SkipBuild,
    [switch]$SkipVideo,
    [string]$OutputRoot
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
    $OutputRoot = Join-Path $root "reports\demo-assets"
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
                SourceDirectory = "."
                TargetDirectory = "screenshots/01-operatore-banco"
                ExpectedFiles = @(
                    "01-login.png", "02-home.png", "03-new-customer.png",
                    "04-category-problem.png", "05-order-created.png", "06-in-progress.png",
                    "07-diagnosis-result.png", "08-repairing.png", "09-ready-for-pickup.png",
                    "10-closed-timeline.png"
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
                SourceDirectory = "01-commessa-nuovo-cliente-chiusura"
                TargetDirectory = "screenshots/02-commessa-nuovo-cliente-chiusura"
            },
            [PSCustomObject]@{
                SourceDirectory = "02-categorie-template"
                TargetDirectory = "screenshots/03-categorie-template"
            },
            [PSCustomObject]@{
                SourceDirectory = "03-clienti-privato-azienda"
                TargetDirectory = "screenshots/04-clienti-privato-azienda"
            }
        )
    }
)

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
    $videoDir = Join-Path $AssetOutputRoot "video"
    $slidesDir = Join-Path $videoDir "temp_slides"
    New-Item -ItemType Directory -Path $slidesDir -Force | Out-Null

    $concatPath = Join-Path $videoDir "slides.txt"
    if (Test-Path $concatPath) { Remove-Item $concatPath }

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

    Remove-Item $slidesDir -Recurse -Force
    Write-Host "Video creato: $outputVideo" -ForegroundColor Green
}

trap {
    Restore-DeviceGlobalSettings
    throw $_
}

Assert-PathInsideRoot -Path $OutputRoot -RootPath $root
if (Test-Path $OutputRoot) { Remove-Item -LiteralPath $OutputRoot -Recurse -Force }
New-Item -ItemType Directory -Path $OutputRoot | Out-Null

$serial = Resolve-DemoDeviceSerial -AndroidHome $env:ANDROID_HOME -AvdName $AvdName -UseConnectedDevice:$UseConnectedDevice
Invoke-DemoApkBuildAndInstall -RootPath $root -Serial $serial -SkipBuild:$SkipBuild

foreach ($asset in $assetsConfig) {
    Write-Host "`n--- Elaborazione Asset: $($asset.Id) ---" -ForegroundColor Cyan

    # Esecuzione Test
    Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-rf", $asset.DeviceDirectory)
    Invoke-DemoScreenshotTest -Serial $serial -TestClass $asset.TestClass

    # Recupero e Processamento Screenshot
    $pulledRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("onlytechtake-assets-" + [System.Guid]::NewGuid().ToString("N"))
    try {
        Pull-DeviceDirectory -Serial $serial -DevicePath $asset.DeviceDirectory -TargetRoot $pulledRoot -PulledDirectoryName $asset.PulledDirectoryName

        foreach ($case in $asset.Cases) {
            $sourceCase = if ($case.SourceDirectory -eq ".") { $pulledRoot } else { Join-Path $pulledRoot $case.SourceDirectory }
            $targetCase = Join-Path $OutputRoot $case.TargetDirectory
            New-Item -ItemType Directory -Path $targetCase -Force | Out-Null

            Get-ChildItem -Path $sourceCase -Filter "*.png" | ForEach-Object {
                $targetPath = Join-Path $targetCase $_.Name
                Convert-ToStoryPng -SourcePath $_.FullName -TargetPath $targetPath
            }
        }

        # Generazione Video se previsto
        if (-not $SkipVideo -and $asset.StoryJson) {
            Process-VideoGeneration -Config $asset -PulledPath $pulledRoot -AssetOutputRoot $OutputRoot
        }
    } finally {
        if (Test-Path $pulledRoot) { Remove-Item $pulledRoot -Recurse -Force }
    }
}

Restore-DeviceGlobalSettings
Write-Host "`nAsset demo generati in: $OutputRoot" -ForegroundColor Green
