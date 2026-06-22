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
    [ValidateSet("Full", "Demo")]
    [string]$Flavor = "Full",
    [switch]$SkipBuild,
    [switch]$SkipVideo,
    [string]$OutputRoot,
    [string]$AssetId,
    [string]$CaseId
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

$packageNameSuffix = switch ($Flavor) {
    "Full" { "" }
    "Demo" { ".demo" }
    "Showcase" { ".showcase" }
}
$packageName = "com.onlytechtake.repairapp$($packageNameSuffix).debug"

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
    $OutputRoot = Join-Path $root "reports/demo-assets/$Flavor"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputRoot)) {
    $OutputRoot = Join-Path $root $OutputRoot
}

# Configurazioni asset
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
                Flavors = @("Full", "Demo")
                SourceDirectory = "."
                TargetDirectory = "social-assets"
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
                Flavors = @("Full", "Demo")
                SourceDirectory = "."
                TargetDirectory = "screenshots/01-operatore-banco"
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
                TargetDirectory = "screenshots/02-commessa-nuovo-cliente-chiusura"
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
                TargetDirectory = "screenshots/03-categorie-template"
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
                TargetDirectory = "screenshots/04-clienti-privato-azienda"
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
                TargetDirectory = "screenshots/05-manutenzione-backup"
                ExpectedFiles = @(
                    "01-gestione-memoria.png", "02-backup-dati.png", "03-export-excel.png"
                )
            },
            [PSCustomObject]@{
                Id = "demo-limit"
                Flavors = @("Demo")
                TestMethod = "capturesDemoLimitStory"
                SourceDirectory = "06-demo-limit"
                TargetDirectory = "screenshots/06-demo-limit"
                ExpectedFiles = @("01-demo-limit-error.png")
            }
        )
    }
)

if ($CaseId -and -not $AssetId) {
    $matchingAssets = @($assetsConfig | Where-Object { $_.Cases.Id -contains $CaseId })
    if ($matchingAssets.Count -ne 1) {
        throw "CaseId non valido o ambiguo: $CaseId"
    }
    $AssetId = $matchingAssets[0].Id
}

# Selezione interattiva se non specificato AssetId o CaseId
if (-not $AssetId -and -not $CaseId) {
    Write-Host "`n--- Selezione Flusso Demo ---" -ForegroundColor Yellow
    Write-Host "0. Tutto (default)"
    $i = 1
    $menuItems = @()
    foreach ($asset in $assetsConfig) {
        if ($asset.Id -eq "social-stories") {
            foreach ($case in $asset.Cases | Where-Object { $_.Flavors -contains $Flavor }) {
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

function Assert-GeneratedCaseAssets {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string[]]$ExpectedFiles
    )

    $actualFiles = @(Get-ChildItem -LiteralPath $Path -Filter "*.png" -File | Select-Object -ExpandProperty Name)
    $missingFiles = @($ExpectedFiles | Where-Object { $_ -notin $actualFiles })
    $unexpectedFiles = @($actualFiles | Where-Object { $_ -notin $ExpectedFiles })
    if ($missingFiles.Count -gt 0 -or $unexpectedFiles.Count -gt 0) {
        throw "Asset non allineati in ${Path}. Mancanti: $($missingFiles -join ', '); inattesi: $($unexpectedFiles -join ', ')."
    }

    foreach ($fileName in $ExpectedFiles) {
        Assert-PngDimensions -Path (Join-Path $Path $fileName) -Width 1080 -Height 1920
    }
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
Invoke-DemoApkBuildAndInstall -RootPath $root -Serial $serial -Flavor $Flavor -SkipBuild:$SkipBuild

foreach ($asset in $assetsConfig) {
    if ($AssetId -and $AssetId -ne $asset.Id) { continue }

    Write-Host "`n--- Elaborazione Asset: $($asset.Id) ---" -ForegroundColor Cyan

    $testTarget = $asset.TestClass
    $activeCases = @($asset.Cases | Where-Object { $_.Flavors -contains $Flavor })

    if ($CaseId) {
        $case = $asset.Cases | Where-Object { $_.Id -eq $CaseId }
        if (-not $case) {
            throw "CaseId '$CaseId' non valido per l'asset '$($asset.Id)'."
        }
        if ($case -and $case.Flavors -notcontains $Flavor) {
            throw "Il caso '$CaseId' non e' disponibile per il flavor $Flavor. Flavor supportati: $($case.Flavors -join ', ')."
        }
        if ($case) {
            if ($case.TestMethod) {
                $testTarget = "$($asset.TestClass)#$($case.TestMethod)"
            }
            $activeCases = @($case)
        }
    }
    if ($activeCases.Count -eq 0) {
        throw "Nessun caso asset disponibile per $($asset.Id) sul flavor $Flavor."
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
        Invoke-DemoScreenshotTest -Serial $serial -TestClass $target -Flavor $Flavor
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
                throw "Cartella sorgente non trovata: $sourceCase"
            }
            Assert-GeneratedCaseAssets -Path $targetCase -ExpectedFiles $case.ExpectedFiles
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
