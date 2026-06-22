<#
.SYNOPSIS
  Genera tutti gli asset (demo e social): screenshot verticali/square e video montati.

.USAGE
  Generazione completa su device fisico:
    .\scripts\powershell\assets\create_demo_assets.ps1 -UseConnectedDevice

  Generazione parziale solo social:
    .\scripts\powershell\assets\create_demo_assets.ps1 -AssetId social-campaign

.DESCRIPTION
  Lo script centralizza la generazione di asset visuali. Esegue gli scenari
  strumentati, processa le immagini (verticali per storie, quadrate per feed)
  e monta i video tramite ffmpeg. Tutti gli output sono in reports/generated-assets/.
#>

param(
    [string]$AvdName = "OnlyTechTakeDemo",
    [switch]$UseConnectedDevice,
    [ValidateSet("Full", "Demo")]
    [string]$Flavor = "Full",
    [switch]$SkipBuild,
    [switch]$SkipVideo,
    [string]$OutputRoot,
    [string]$AssetId,
    [string]$CaseId,
    [switch]$NonInteractive
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path

# Banner
if (-not $NonInteractive) {
    Write-Host "`n--- OnlyTechTake Demo Asset Creator ---" -ForegroundColor Cyan
    Write-Host "Genera screenshot e video demo per il marketing."
}

$packageNameSuffix = switch ($Flavor) {
    "Full" { "" }
    "Demo" { ".demo" }
    "Showcase" { ".showcase" }
}
$packageName = "com.onlytechtake.repairapp$($packageNameSuffix).debug"

# Load helpers
. (Join-Path $PSScriptRoot "..\support\android_env.ps1")
Set-AndroidCliEnvironment | Out-Null
. (Join-Path $PSScriptRoot "..\support\android_demo.ps1")

# Centralized output path
if (-not $OutputRoot) {
    $OutputRoot = Join-Path $root "reports/generated-assets/$Flavor"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputRoot)) {
    $OutputRoot = Join-Path $root $OutputRoot
}

# Asset Configuration
$assetsConfig = @(
    [PSCustomObject]@{
        Id = "social-campaign"
        Flavors = @("Full", "Demo")
        TestClass = "com.onlytechtake.repairapp.demo.SocialCampaignScreenshotTest"
        DeviceDirectory = "/sdcard/Android/data/$packageName/files/social-campaign"
        PulledDirectoryName = "social-campaign"
        StoryJson = $null
        Cases = @(
            [PSCustomObject]@{
                Id = "campaign-flow"
                SourceDirectory = "."
                TargetDirectory = "social/vertical"
                SquareDirectory = "social/square"
                ExpectedFiles = @(
                    "01-home.png", "02-accoglienza.png", "03-diagnosi.png",
                    "04-firma.png", "05-whatsapp.png", "06-successo.png"
                )
            }
        )
    },
    [PSCustomObject]@{
        Id = "operator-intake"
        Flavors = @("Full", "Demo")
        TestClass = "com.onlytechtake.repairapp.demo.OperatorIntakeDemoScreenshotTest"
        DeviceDirectory = "/sdcard/Android/data/$packageName/files/demo-screenshots"
        PulledDirectoryName = "demo-screenshots"
        StoryJson = "demo/video/operator-intake.story.json"
        Cases = @(
            [PSCustomObject]@{
                Id = "full"
                SourceDirectory = "."
                TargetDirectory = "demo/screenshots/01-operatore-banco"
                ExpectedFiles = @(
                    "01-login.png", "02-home.png", "03-new-customer.png",
                    "04-category-problem.png", "05-signature-capture.png",
                    "06-order-created.png", "06b-back-to-top-orders.png",
                    "07-whatsapp-preview.png", "08-in-progress.png",
                    "09-diagnosis-result.png", "10-pdf-documents.png",
                    "11-ready-for-pickup.png", "12-closed-timeline.png"
                )
            }
        )
    },
    [PSCustomObject]@{
        Id = "social-stories"
        TestClass = "com.onlytechtake.repairapp.demo.SocialStoryDemoScreenshotTest"
        DeviceDirectory = "/sdcard/Android/data/$packageName/files/demo-social-screenshots"
        PulledDirectoryName = "demo-social-screenshots"
        StoryJson = $null
        Cases = @(
            [PSCustomObject]@{
                Id = "order-intake"
                Flavors = @("Full", "Demo")
                TestMethod = "capturesOrderIntakeToClosureStories"
                SourceDirectory = "01-commessa-nuovo-cliente-chiusura"
                TargetDirectory = "demo/screenshots/02-commessa-nuovo-cliente-chiusura"
                ExpectedFiles = @(
                    "01-login.png", "02-dashboard-home.png", "03-nuovo-cliente.png",
                    "04-categoria-problema.png", "05-commessa-aperta.png",
                    "06-presa-in-carico.png", "07-diagnosi-risultato.png",
                    "08-riparazione.png", "09-pronta-ritiro.png", "10-chiusura-timeline.png"
                )
            },
            [PSCustomObject]@{
                Id = "config"
                Flavors = @("Full", "Demo")
                TestMethod = "capturesCategoryAndTemplateStories"
                SourceDirectory = "02-categorie-template"
                TargetDirectory = "demo/screenshots/03-categorie-template"
                ExpectedFiles = @(
                    "01-dashboard-admin.png", "02-config-hub.png",
                    "03-creazione-categoria.png", "04-categoria-salvata.png",
                    "05-template-associazioni.png", "05a-template-tooltip.png",
                    "05b-creazione-template.png", "06-template-salvato.png"
                )
            },
            [PSCustomObject]@{
                Id = "customers"
                Flavors = @("Full", "Demo")
                TestMethod = "capturesPrivateAndBusinessCustomerStories"
                SourceDirectory = "03-clienti-privato-azienda"
                TargetDirectory = "demo/screenshots/04-clienti-privato-azienda"
                ExpectedFiles = @(
                    "01-archivio-clienti.png", "02-cliente-privato.png",
                    "03-privato-salvato.png", "04-cliente-azienda.png",
                    "05-azienda-salvata.png"
                )
            },
            [PSCustomObject]@{
                Id = "maintenance"
                Flavors = @("Full", "Demo")
                TestMethod = "capturesMaintenanceStories"
                SourceDirectory = "05-manutenzione-backup"
                TargetDirectory = "demo/screenshots/05-manutenzione-backup"
                ExpectedFiles = @(
                    "01-gestione-memoria.png", "02-backup-dati.png", "03-export-excel.png"
                )
            },
            [PSCustomObject]@{
                Id = "demo-limit"
                Flavors = @("Demo")
                TestMethod = "capturesDemoLimitStory"
                SourceDirectory = "06-demo-limit"
                TargetDirectory = "demo/screenshots/06-demo-limit"
                ExpectedFiles = @("01-demo-limit-error.png")
            }
        )
    }
)

# Helpers
function Reset-ManagedDirectory {
    param([Parameter(Mandatory = $true)][string]$Path)
    Assert-PathInsideRoot -Path $Path -RootPath $root
    if (Test-Path $Path) { Remove-Item -LiteralPath $Path -Recurse -Force }
    New-Item -ItemType Directory -Path $Path -Force | Out-Null
}

function Assert-Files {
    param([string]$Path, [string[]]$Expected)
    $actual = @(Get-ChildItem -LiteralPath $Path -Filter "*.png" -File | Select-Object -ExpandProperty Name)
    $missing = @($Expected | Where-Object { $_ -notin $actual })
    if ($missing.Count -gt 0) {
        $msg = $missing -join ', '
        throw "File mancanti in $Path : $msg"
    }
}

# Main selection
if (-not $AssetId -and -not $CaseId -and -not $NonInteractive) {
    Write-Host "`n--- Selezione Asset da Rigenerare ---" -ForegroundColor Yellow
    Write-Host "0. Tutto (default)"
    $i = 1
    $menuItems = @()
    foreach ($asset in $assetsConfig) {
        if ($asset.Id -eq "social-stories") {
            foreach ($case in $asset.Cases | Where-Object { $_.Flavors -contains $Flavor }) {
                Write-Host "$i. Social: $($case.Id)"
                $menuItems += [PSCustomObject]@{ Asset = $asset; Case = $case }
                $i++
            }
        } else {
            Write-Host "$i. $($asset.Id)"
            $menuItems += [PSCustomObject]@{ Asset = $asset; Case = $null }
            $i++
        }
    }
    $choice = Read-Host "`nScelta [0-$($i-1)]"
    if ($choice -ne "0" -and $choice -match "^\d+$" -and [int]$choice -lt $i) {
        $selected = $menuItems[[int]$choice - 1]
        $AssetId = $selected.Asset.Id
        $CaseId = if ($selected.Case) { $selected.Case.Id } else { $null }
    }
}

trap { Restore-DeviceGlobalSettings; throw $_ }

# Run
$serial = Resolve-DemoDeviceSerial -AndroidHome $env:ANDROID_HOME -AvdName $AvdName -UseConnectedDevice:$UseConnectedDevice
Invoke-DemoApkBuildAndInstall -RootPath $root -Serial $serial -Flavor $Flavor -SkipBuild:$SkipBuild

foreach ($asset in $assetsConfig) {
    if ($AssetId -and $AssetId -ne $asset.Id) { continue }
    Write-Host "`n>>> Elaborazione Asset: $($asset.Id)" -ForegroundColor Cyan

    $activeCases = @($asset.Cases | Where-Object { -not $_.Flavors -or ($_.Flavors -contains $Flavor) })
    if ($CaseId) {
        $activeCases = @($activeCases | Where-Object { $_.Id -eq $CaseId })
    }
    if ($activeCases.Count -eq 0) { continue }

    # Execution
    Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-rf", $asset.DeviceDirectory)
    foreach ($case in $activeCases) {
        $targetTest = if ($case.TestMethod) { "$($asset.TestClass)#$($case.TestMethod)" } else { $asset.TestClass }
        Invoke-DemoScreenshotTest -Serial $serial -TestClass $targetTest -Flavor $Flavor
    }

    # Retrieval
    $pulledRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("onlytechtake-assets-" + [System.Guid]::NewGuid().ToString("N"))
    try {
        Pull-DeviceDirectory -Serial $serial -DevicePath $asset.DeviceDirectory -TargetRoot $pulledRoot -PulledDirectoryName $asset.PulledDirectoryName

        foreach ($case in $activeCases) {
            $src = if ($case.SourceDirectory -eq ".") { $pulledRoot } else { Join-Path $pulledRoot $case.SourceDirectory }
            $destVertical = Join-Path $OutputRoot $case.TargetDirectory
            Write-Host "Salvataggio in: $(Get-RepoRelativePath -FullPath $destVertical -RootPath $root)" -ForegroundColor Gray
            Reset-ManagedDirectory -Path $destVertical

            Get-ChildItem -Path $src -Filter "*.png" | ForEach-Object {
                Convert-ToStoryPng -SourcePath $_.FullName -TargetPath (Join-Path $destVertical $_.Name)
            }
            Assert-Files -Path $destVertical -Expected $case.ExpectedFiles

            if ($case.SquareDirectory) {
                $destSquare = Join-Path $OutputRoot $case.SquareDirectory
                Reset-ManagedDirectory -Path $destSquare
                Get-ChildItem -Path $src -Filter "*.png" | ForEach-Object {
                    Convert-ToSquarePng -SourcePath $_.FullName -TargetPath (Join-Path $destSquare $_.Name)
                }
            }
        }

        # Automated video
        if (-not $SkipVideo -and $asset.StoryJson -and (-not $CaseId)) {
            $videoDest = Join-Path $OutputRoot "demo/videos/automated"
            if (-not (Test-Path $videoDest)) { New-Item -ItemType Directory -Path $videoDest -Force | Out-Null }

            $storyPath = Join-Path $root $asset.StoryJson
            if ((Test-Path $storyPath) -and (Get-Command "ffmpeg" -ErrorAction SilentlyContinue)) {
                Write-Host "Generazione video montato..." -ForegroundColor Gray
                $story = Get-Content -Path $storyPath -Raw | ConvertFrom-Json
                $slidesDir = Join-Path $videoDest "temp_slides"
                New-Item -ItemType Directory -Path $slidesDir -Force | Out-Null
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
                    $last = Join-Path $slidesDir ("slide_{0:D2}.png" -f ($idx - 1))
                    Add-Content -Path $concatPath -Value ("file '{0}'" -f (Convert-ToConcatPath $last))
                    $outV = Join-Path $videoDest $story.outputFile
                    Invoke-Checked -FilePath "ffmpeg" -Arguments @("-y", "-f", "concat", "-safe", "0", "-i", $concatPath, "-vcodec", "libx264", "-preset", "ultrafast", "-crf", "22", "-pix_fmt", "yuv420p", "-movflags", "+faststart", $outV)
                } finally {
                    if (Test-Path $slidesDir) { Remove-Item $slidesDir -Recurse -Force }
                    if (Test-Path $concatPath) { Remove-Item $concatPath -Force }
                }
            }
        }
    } finally {
        if (Test-Path $pulledRoot) { Remove-Item $pulledRoot -Recurse -Force }
    }
}

# Special handling for social video if needed
if (($AssetId -eq "social-campaign" -or -not $AssetId) -and -not $SkipVideo) {
    Write-Host "`n>>> Registrazione Video Social..." -ForegroundColor Cyan
    $socVidDest = Join-Path $OutputRoot "social/videos"
    if (-not (Test-Path $socVidDest)) { New-Item -ItemType Directory -Path $socVidDest -Force | Out-Null }
    & (Join-Path $PSScriptRoot "record_demo_device.ps1") -Flavor $Flavor -ScenarioId "social-campaign" -OutputFile "social-campaign.mp4" -OutputRoot $socVidDest -SkipBuild -StartImmediately
}

Restore-DeviceGlobalSettings
Write-Host "`nAsset rigenerati in: $OutputRoot" -ForegroundColor Green
