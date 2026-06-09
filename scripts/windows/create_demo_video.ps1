<#
.SYNOPSIS
  Genera screenshot e MP4 demo del flusso operatore al banco.

.USAGE
  .\scripts\windows\create_demo_video.ps1

.DESCRIPTION
  Lo script usa un AVD dedicato chiamato OnlyTechTakeDemo, installa APK debug
  e test APK, pulisce i dati dell'app su quell'emulatore, esegue lo scenario
  strumentato e monta gli screenshot in un MP4 con caption.
#>

param(
    [string]$AvdName = "OnlyTechTakeDemo",
    [switch]$UseConnectedDevice,
    [string]$StoryPath,
    [string]$OutputRoot
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$supportScript = Join-Path $PSScriptRoot "support\android_env.ps1"
if (Test-Path $supportScript) {
    . $supportScript
    Set-AndroidCliEnvironment | Out-Null
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

function Assert-Command($Name) {
    if ($null -eq (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "$Name non trovato nel PATH."
    }
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,

        [string[]]$Arguments = @()
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Comando fallito: $FilePath $($Arguments -join ' ')"
    }
}

function Invoke-InstrumentationChecked {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $output = & adb @Arguments 2>&1
    $output | ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -ne 0 -or ($output -join "`n") -match "FAILURES!!!|There was \d+ failure|INSTRUMENTATION_FAILED|Process crashed") {
        throw "Scenario strumentato demo fallito."
    }
}

$deviceSettingsToRestore = @()

function Set-DeviceGlobalSettingTemporary {
    param(
        [Parameter(Mandatory = $true)][string]$Serial,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Value
    )

    $previous = (adb -s $Serial shell settings get global $Name 2>$null).Trim()
    $script:deviceSettingsToRestore += [PSCustomObject]@{
        Serial = $Serial
        Name = $Name
        Previous = $previous
    }
    adb -s $Serial shell settings put global $Name $Value | Out-Null
}

function Restore-DeviceGlobalSettings {
    for ($index = $script:deviceSettingsToRestore.Count - 1; $index -ge 0; $index--) {
        $setting = $script:deviceSettingsToRestore[$index]
        if ($setting.Previous -eq "null" -or [string]::IsNullOrWhiteSpace($setting.Previous)) {
            adb -s $setting.Serial shell settings delete global $setting.Name | Out-Null
        } else {
            adb -s $setting.Serial shell settings put global $setting.Name $setting.Previous | Out-Null
        }
    }
    $script:deviceSettingsToRestore = @()
}

trap {
    Restore-DeviceGlobalSettings
    throw
}

function Get-EmulatorSerialForAvd {
    param([Parameter(Mandatory = $true)][string]$Name)

    $devices = adb devices | Select-Object -Skip 1 |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -match "^emulator-\d+\s+device$" } |
        ForEach-Object { ($_ -split "\s+")[0] }

    foreach ($serial in $devices) {
        $avdOutput = adb -s $serial emu avd name 2>$null
        $runningName = $avdOutput | Where-Object { $_ -and $_ -ne "OK" } | Select-Object -First 1
        if ($runningName -eq $Name) {
            return $serial
        }
    }

    return $null
}

function Get-ConnectedDeviceSerial {
    $devices = adb devices | Select-Object -Skip 1 |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -match "^\S+\s+device$" } |
        ForEach-Object { ($_ -split "\s+")[0] } |
        Where-Object { -not $_.StartsWith("emulator-") }

    $deviceCount = @($devices).Count
    if ($deviceCount -eq 0) {
        throw "Nessun device fisico collegato rilevato da adb."
    }
    if ($deviceCount -gt 1) {
        throw "Piu' device fisici collegati. Lascia collegato un solo device o usa l'emulatore demo."
    }

    return @($devices)[0]
}

function Wait-ForBoot {
    param([Parameter(Mandatory = $true)][string]$Serial)

    adb -s $Serial wait-for-device
    $deadline = (Get-Date).AddMinutes(4)
    do {
        $bootCompleted = (adb -s $Serial shell getprop sys.boot_completed 2>$null).Trim()
        if ($bootCompleted -eq "1") {
            adb -s $Serial shell input keyevent 82 | Out-Null
            return
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "Timeout in attesa del boot dell'emulatore $Serial."
}

function Assert-PathInsideRoot {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$RootPath
    )

    $fullPath = [System.IO.Path]::GetFullPath($Path)
    $fullRoot = [System.IO.Path]::GetFullPath($RootPath)
    if (-not $fullPath.StartsWith($fullRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Percorso output fuori dalla root progetto: $fullPath"
    }
}

function New-CaptionedSlide {
    param(
        [Parameter(Mandatory = $true)][string]$SourcePath,
        [Parameter(Mandatory = $true)][string]$Caption,
        [Parameter(Mandatory = $true)][string]$TargetPath,
        [Parameter(Mandatory = $true)][int]$Width,
        [Parameter(Mandatory = $true)][int]$Height
    )

    $source = [System.Drawing.Bitmap]::FromFile($SourcePath)
    $slide = New-Object System.Drawing.Bitmap($Width, $Height)
    $graphics = [System.Drawing.Graphics]::FromImage($slide)
    try {
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.Clear([System.Drawing.Color]::White)

        $scale = [Math]::Min($Width / $source.Width, $Height / $source.Height)
        $drawWidth = [int]($source.Width * $scale)
        $drawHeight = [int]($source.Height * $scale)
        $drawX = [int](($Width - $drawWidth) / 2)
        $drawY = [int](($Height - $drawHeight) / 2)
        $graphics.DrawImage($source, $drawX, $drawY, $drawWidth, $drawHeight)

        $captionHeight = 170
        $overlayBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(190, 17, 24, 39))
        $graphics.FillRectangle($overlayBrush, 0, $Height - $captionHeight, $Width, $captionHeight)
        $overlayBrush.Dispose()

        $font = New-Object System.Drawing.Font("Segoe UI", 38, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
        $captionBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
        $captionRect = New-Object System.Drawing.RectangleF(72, ($Height - $captionHeight + 42), ($Width - 144), 110)
        $format = New-Object System.Drawing.StringFormat
        $format.Trimming = [System.Drawing.StringTrimming]::EllipsisWord
        $graphics.DrawString($Caption, $font, $captionBrush, $captionRect, $format)
        $format.Dispose()
        $captionBrush.Dispose()
        $font.Dispose()

        $slide.Save($TargetPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $slide.Dispose()
        $source.Dispose()
    }
}

function Convert-ToConcatPath {
    param([Parameter(Mandatory = $true)][string]$Path)
    return [System.IO.Path]::GetFullPath($Path).Replace("\", "/").Replace("'", "'\''")
}

Assert-Command "ffmpeg"
Assert-Command "adb"

if (-not (Test-Path $StoryPath)) {
    throw "Story config non trovata: $StoryPath"
}

if ($UseConnectedDevice) {
    $serial = Get-ConnectedDeviceSerial
    Wait-ForBoot -Serial $serial
    Write-Host "Uso device fisico $serial. I dati dell'app demo verranno puliti su questo device."
    Write-Host "Disattivo temporaneamente la verifica APK/Play Protect per installazioni adb su questo device."
    Set-DeviceGlobalSettingTemporary -Serial $serial -Name "package_verifier_enable" -Value "0"
    Set-DeviceGlobalSettingTemporary -Serial $serial -Name "verifier_verify_adb_installs" -Value "0"
    Set-DeviceGlobalSettingTemporary -Serial $serial -Name "upload_apk_enable" -Value "0"
} else {
    $emulatorPath = Join-Path $env:ANDROID_HOME "emulator\emulator.exe"
    if (-not (Test-Path $emulatorPath)) {
        throw "emulator.exe non trovato in ANDROID_HOME. Esegui prima check_android_env.ps1."
    }

    $availableAvds = & $emulatorPath -list-avds
    if ($availableAvds -notcontains $AvdName) {
        throw @"
AVD '$AvdName' non trovato.
Crea un emulatore dedicato da Android Studio Device Manager con nome '$AvdName'.
Consigliato: API 36, profilo Pixel/Tablet stabile, risoluzione 1920x1080 o simile.
Lo script non installa system image o modifica l'SDK automaticamente.
Per usare temporaneamente un device fisico collegato, esegui con -UseConnectedDevice.
"@
    }

    $serial = Get-EmulatorSerialForAvd -Name $AvdName
    if (-not $serial) {
        Write-Host "Avvio emulatore $AvdName..."
        Start-Process -FilePath $emulatorPath -ArgumentList @("-avd", $AvdName, "-no-snapshot-save") -WindowStyle Hidden | Out-Null
        Start-Sleep -Seconds 5
        $deadline = (Get-Date).AddMinutes(2)
        do {
            $serial = Get-EmulatorSerialForAvd -Name $AvdName
            if ($serial) { break }
            Start-Sleep -Seconds 2
        } while ((Get-Date) -lt $deadline)
    }

    if (-not $serial) {
        throw "Emulatore $AvdName non rilevato dopo l'avvio."
    }

    Wait-ForBoot -Serial $serial
    Write-Host "Uso emulatore $serial ($AvdName)."
}

Assert-PathInsideRoot -Path $OutputRoot -RootPath $root
if (Test-Path $OutputRoot) {
    Remove-Item -LiteralPath $OutputRoot -Recurse -Force
}
$screenshotsDir = Join-Path $OutputRoot "screenshots"
$slidesDir = Join-Path $OutputRoot "slides"
New-Item -ItemType Directory -Path $screenshotsDir, $slidesDir | Out-Null

$gradle = Join-Path $root "gradlew.bat"
Invoke-Checked -FilePath $gradle -Arguments @("--no-configuration-cache", ":app:assembleDebug", ":app:assembleDebugAndroidTest")

$debugApk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$testApk = Join-Path $root "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
if (-not (Test-Path $debugApk)) { throw "APK debug non trovato: $debugApk" }
if (-not (Test-Path $testApk)) { throw "Test APK non trovato: $testApk" }

Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "install", "-r", $debugApk)
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "install", "-r", $testApk)
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "pm", "clear", "com.onlytechtake.repairapp")

$testClass = "com.onlytechtake.repairapp.demo.OperatorIntakeDemoScreenshotTest"
$runner = "com.onlytechtake.repairapp.test/androidx.test.runner.AndroidJUnitRunner"
Invoke-InstrumentationChecked -Arguments @(
    "-s",
    $serial,
    "shell",
    "am",
    "instrument",
    "-w",
    "-e",
    "class",
    $testClass,
    $runner
)

$deviceScreenshots = "/sdcard/Android/data/com.onlytechtake.repairapp/files/demo-screenshots"
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "pull", $deviceScreenshots, $screenshotsDir)

$pulledDir = Join-Path $screenshotsDir "demo-screenshots"
if (Test-Path $pulledDir) {
    Get-ChildItem -Path $pulledDir -Filter "*.png" | Move-Item -Destination $screenshotsDir
    Remove-Item -LiteralPath $pulledDir -Recurse -Force
}

$story = Get-Content -Path $StoryPath -Raw | ConvertFrom-Json
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
