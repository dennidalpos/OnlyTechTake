<#
.SYNOPSIS
  Genera MP4 demo del flusso operatore al banco.

.USAGE
  .\scripts\windows\create_demo_video.ps1
  .\scripts\windows\create_demo_video.ps1 -UseConnectedDevice

.DESCRIPTION
  Lo script usa un AVD dedicato chiamato OnlyTechTakeDemo oppure un singolo
  device USB, installa APK debug e test APK, pulisce i dati dell'app, esegue
  lo scenario strumentato e monta gli screenshot in un MP4 con caption.
#>

param(
    [string]$AvdName = "OnlyTechTakeDemo",
    [switch]$UseConnectedDevice,
    [string]$StoryPath,
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

if (-not $StoryPath) {
    $StoryPath = Join-Path $root "demo\video\operator-intake.story.json"
} elseif (-not [System.IO.Path]::IsPathRooted($StoryPath)) {
    $StoryPath = Join-Path $root $StoryPath
}

if (-not $OutputRoot) {
    $OutputRoot = Join-Path $root "reports\demo-video\operator-intake"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputRoot)) {
    $OutputRoot = Join-Path $root $OutputRoot
}

trap {
    Restore-DeviceGlobalSettings
    throw $_
}

Assert-Command "ffmpeg"
Assert-Command "adb"

if (-not (Test-Path $StoryPath)) {
    throw "Story config non trovata: $StoryPath"
}

Assert-PathInsideRoot -Path $OutputRoot -RootPath $root
if (Test-Path $OutputRoot) {
    Remove-Item -LiteralPath $OutputRoot -Recurse -Force
}
$screenshotsDir = Join-Path $OutputRoot "screenshots"
$slidesDir = Join-Path $OutputRoot "slides"
New-Item -ItemType Directory -Path $screenshotsDir, $slidesDir | Out-Null

$serial = Resolve-DemoDeviceSerial -AndroidHome $env:ANDROID_HOME -AvdName $AvdName -UseConnectedDevice:$UseConnectedDevice
Invoke-DemoApkBuildAndInstall -RootPath $root -Serial $serial

$deviceScreenshots = "/sdcard/Android/data/com.onlytechtake.repairapp/files/demo-screenshots"
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-rf", $deviceScreenshots)
Invoke-DemoScreenshotTest -Serial $serial -TestClass "com.onlytechtake.repairapp.demo.OperatorIntakeDemoScreenshotTest"

Pull-DeviceDirectory -Serial $serial -DevicePath $deviceScreenshots -TargetRoot $screenshotsDir -PulledDirectoryName "demo-screenshots"

$story = Get-Content -Path $StoryPath -Raw | ConvertFrom-Json
$expectedScreenshots = @($story.scenes | ForEach-Object { $_.file })
$actualScreenshots = @(Get-ChildItem -Path $screenshotsDir -Filter "*.png" -File | Select-Object -ExpandProperty Name)
$missingScreenshots = @($expectedScreenshots | Where-Object { $actualScreenshots -notcontains $_ })
$unexpectedScreenshots = @($actualScreenshots | Where-Object { $expectedScreenshots -notcontains $_ })
if ($missingScreenshots.Count -gt 0) {
    throw "Screenshot demo mancanti: $($missingScreenshots -join ', ')"
}
if ($unexpectedScreenshots.Count -gt 0) {
    throw "Screenshot demo inattesi: $($unexpectedScreenshots -join ', ')"
}
if ($actualScreenshots.Count -ne $expectedScreenshots.Count) {
    throw "Numero screenshot demo non valido: attesi $($expectedScreenshots.Count), trovati $($actualScreenshots.Count)."
}

Add-Type -AssemblyName System.Drawing

$index = 1
$concatPath = Join-Path $OutputRoot "slides.txt"
if (Test-Path $concatPath) {
    Remove-Item -LiteralPath $concatPath -Force
}

foreach ($scene in $story.scenes) {
    $sourcePath = Join-Path $screenshotsDir $scene.file
    if (-not (Test-Path $sourcePath)) {
        throw "Screenshot mancante per la scena '$($scene.caption)': $sourcePath"
    }

    $slidePath = Join-Path $slidesDir ("slide_{0:D2}.png" -f $index)
    New-CaptionedSlide -SourcePath $sourcePath -Caption $scene.caption -TargetPath $slidePath -Width $story.width -Height $story.height

    Add-Content -Path $concatPath -Value ("file '{0}'" -f (Convert-ToConcatPath $slidePath))
    Add-Content -Path $concatPath -Value ("duration {0}" -f ([string]$scene.durationSeconds).Replace(",", "."))
    $index++
}

$lastSlide = Join-Path $slidesDir ("slide_{0:D2}.png" -f ($index - 1))
Add-Content -Path $concatPath -Value ("file '{0}'" -f (Convert-ToConcatPath $lastSlide))

$outputVideo = Join-Path $OutputRoot $story.outputFile
Invoke-Checked -FilePath "ffmpeg" -Arguments @(
    "-y",
    "-f",
    "concat",
    "-safe",
    "0",
    "-i",
    $concatPath,
    "-r",
    [string]$story.fps,
    "-pix_fmt",
    "yuv420p",
    "-movflags",
    "+faststart",
    $outputVideo
)

Write-Host "Demo generata:"
Write-Host "Screenshot: $screenshotsDir"
Write-Host "Video: $outputVideo"

Restore-DeviceGlobalSettings
