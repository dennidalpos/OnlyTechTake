<#
.SYNOPSIS
  Genera tutti gli screenshot demo verticali, divisi per caso d'uso.

.USAGE
  .\scripts\windows\create_demo_screenshots.ps1
  .\scripts\windows\create_demo_screenshots.ps1 -UseConnectedDevice

.DESCRIPTION
  Lo script usa un AVD dedicato chiamato OnlyTechTakeDemo oppure un singolo
  device USB, installa APK debug e test APK, pulisce i dati dell'app, esegue
  gli scenari strumentati demo e produce solo PNG 1080x1920.
#>

param(
    [string]$AvdName = "OnlyTechTakeDemo",
    [switch]$UseConnectedDevice,
    [string]$OutputRoot
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

$environmentScript = Join-Path $PSScriptRoot "support\android_env.ps1"
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
    $OutputRoot = Join-Path $root "reports\demo-screenshots"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputRoot)) {
    $OutputRoot = Join-Path $root $OutputRoot
}

$screenshotSources = @(
    [PSCustomObject]@{
        TestClass = "com.onlytechtake.repairapp.demo.OperatorIntakeDemoScreenshotTest"
        DeviceDirectory = "/sdcard/Android/data/com.onlytechtake.repairapp/files/demo-screenshots"
        PulledDirectoryName = "demo-screenshots"
        Cases = @(
            [PSCustomObject]@{
                SourceDirectory = "."
                TargetDirectory = "01-operatore-banco"
                ExpectedFiles = @(
                    "01-login.png",
                    "02-home.png",
                    "03-new-customer.png",
                    "04-category-problem.png",
                    "05-order-created.png",
                    "06-in-progress.png",
                    "07-diagnosis-result.png",
                    "08-repairing.png",
                    "09-ready-for-pickup.png",
                    "10-closed-timeline.png"
                )
            }
        )
    },
    [PSCustomObject]@{
        TestClass = "com.onlytechtake.repairapp.demo.SocialStoryDemoScreenshotTest"
        DeviceDirectory = "/sdcard/Android/data/com.onlytechtake.repairapp/files/demo-social-screenshots"
        PulledDirectoryName = "demo-social-screenshots"
        Cases = @(
            [PSCustomObject]@{
                SourceDirectory = "01-commessa-nuovo-cliente-chiusura"
                TargetDirectory = "02-commessa-nuovo-cliente-chiusura"
                ExpectedFiles = $null
            },
            [PSCustomObject]@{
                SourceDirectory = "02-categorie-template"
                TargetDirectory = "03-categorie-template"
                ExpectedFiles = $null
            },
            [PSCustomObject]@{
                SourceDirectory = "03-clienti-privato-azienda"
                TargetDirectory = "04-clienti-privato-azienda"
                ExpectedFiles = $null
            }
        )
    }
)

function Convert-CaseScreenshots {
    param(
        [Parameter(Mandatory = $true)][string]$SourceRoot,
        [Parameter(Mandatory = $true)][string]$TargetRoot,
        [Parameter(Mandatory = $true)][object]$Case
    )

    $sourceCase = if ($Case.SourceDirectory -eq ".") {
        $SourceRoot
    } else {
        Join-Path $SourceRoot $Case.SourceDirectory
    }
    if (-not (Test-Path $sourceCase)) {
        throw "Caso screenshot mancante: $($Case.SourceDirectory)"
    }

    $targetCase = Join-Path $TargetRoot $Case.TargetDirectory
    New-Item -ItemType Directory -Path $targetCase | Out-Null

    $sourceFiles = @(Get-ChildItem -Path $sourceCase -Filter "*.png" -File | Sort-Object Name)
    if ($sourceFiles.Count -eq 0) {
        throw "Nessun PNG generato per il caso $($Case.SourceDirectory)."
    }

    if ($null -ne $Case.ExpectedFiles) {
        $actualNames = @($sourceFiles | Select-Object -ExpandProperty Name)
        $missingFiles = @($Case.ExpectedFiles | Where-Object { $actualNames -notcontains $_ })
        $unexpectedFiles = @($actualNames | Where-Object { $Case.ExpectedFiles -notcontains $_ })
        if ($missingFiles.Count -gt 0) {
            throw "Screenshot mancanti per $($Case.TargetDirectory): $($missingFiles -join ', ')"
        }
        if ($unexpectedFiles.Count -gt 0) {
            throw "Screenshot inattesi per $($Case.TargetDirectory): $($unexpectedFiles -join ', ')"
        }
    }

    foreach ($sourceFile in $sourceFiles) {
        $targetPath = Join-Path $targetCase $sourceFile.Name
        Convert-ToStoryPng -SourcePath $sourceFile.FullName -TargetPath $targetPath
        Assert-PngDimensions -Path $targetPath -Width 1080 -Height 1920
    }

    $unexpected = @(Get-ChildItem -Path $targetCase -File | Where-Object { $_.Extension -ne ".png" })
    if ($unexpected.Count -gt 0) {
        throw "File non PNG presenti in ${targetCase}: $($unexpected.Name -join ', ')"
    }
}

trap {
    Restore-DeviceGlobalSettings
    throw $_
}

Assert-PathInsideRoot -Path $OutputRoot -RootPath $root
if (Test-Path $OutputRoot) {
    Remove-Item -LiteralPath $OutputRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $OutputRoot | Out-Null

$serial = Resolve-DemoDeviceSerial -AndroidHome $env:ANDROID_HOME -AvdName $AvdName -UseConnectedDevice:$UseConnectedDevice
Invoke-DemoApkBuildAndInstall -RootPath $root -Serial $serial

Add-Type -AssemblyName System.Drawing

foreach ($source in $screenshotSources) {
    Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-rf", $source.DeviceDirectory)
    Invoke-DemoScreenshotTest -Serial $serial -TestClass $source.TestClass -FailureMessage "Scenario strumentato screenshot fallito: $($source.TestClass)"

    $pulledRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("onlytechtake-converted-source-" + [System.Guid]::NewGuid().ToString("N"))
    try {
        Pull-DeviceDirectory -Serial $serial -DevicePath $source.DeviceDirectory -TargetRoot $pulledRoot -PulledDirectoryName $source.PulledDirectoryName
        foreach ($case in $source.Cases) {
            Convert-CaseScreenshots -SourceRoot $pulledRoot -TargetRoot $OutputRoot -Case $case
        }
    } finally {
        if (Test-Path $pulledRoot) {
            Remove-Item -LiteralPath $pulledRoot -Recurse -Force
        }
    }
}

Write-Host "Screenshot demo generati:"
Write-Host $OutputRoot

Restore-DeviceGlobalSettings
