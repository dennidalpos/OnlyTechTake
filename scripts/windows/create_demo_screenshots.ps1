<#
.SYNOPSIS
  Genera screenshot demo verticali per social stories, divisi per caso.

.USAGE
  .\scripts\windows\create_demo_screenshots.ps1

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
$supportScript = Join-Path $PSScriptRoot "support\android_env.ps1"
if (Test-Path $supportScript) {
    . $supportScript
    Set-AndroidCliEnvironment | Out-Null
}

if (-not $OutputRoot) {
    $OutputRoot = Join-Path $root "reports\demo-screenshots"
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
        throw "Scenario strumentato screenshot fallito."
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
    throw $_
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

function Convert-ToStoryPng {
    param(
        [Parameter(Mandatory = $true)][string]$SourcePath,
        [Parameter(Mandatory = $true)][string]$TargetPath
    )

    $source = [System.Drawing.Bitmap]::FromFile($SourcePath)
    $story = New-Object System.Drawing.Bitmap(1080, 1920)
    $graphics = [System.Drawing.Graphics]::FromImage($story)
    try {
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.Clear([System.Drawing.Color]::FromArgb(246, 247, 249))

        $maxWidth = 1000
        $maxHeight = 1840
        $scale = [Math]::Min($maxWidth / $source.Width, $maxHeight / $source.Height)
        $drawWidth = [int]($source.Width * $scale)
        $drawHeight = [int]($source.Height * $scale)
        $drawX = [int]((1080 - $drawWidth) / 2)
        $drawY = [int]((1920 - $drawHeight) / 2)

        $shadowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(28, 0, 0, 0))
        $graphics.FillRectangle($shadowBrush, $drawX + 10, $drawY + 10, $drawWidth, $drawHeight)
        $shadowBrush.Dispose()
        $graphics.DrawImage($source, $drawX, $drawY, $drawWidth, $drawHeight)

        $story.Save($TargetPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $story.Dispose()
        $source.Dispose()
    }
}

function Assert-StoryDimensions {
    param([Parameter(Mandatory = $true)][string]$Path)

    $image = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        if ($image.Width -ne 1080 -or $image.Height -ne 1920) {
            throw "Dimensioni non valide per ${Path}: $($image.Width)x$($image.Height)."
        }
    } finally {
        $image.Dispose()
    }
}

Assert-Command "adb"

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
New-Item -ItemType Directory -Path $OutputRoot | Out-Null

$gradle = Join-Path $root "gradlew.bat"
Invoke-Checked -FilePath $gradle -Arguments @("--no-configuration-cache", ":app:assembleDebug", ":app:assembleDebugAndroidTest")

$debugApk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$testApk = Join-Path $root "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
if (-not (Test-Path $debugApk)) { throw "APK debug non trovato: $debugApk" }
if (-not (Test-Path $testApk)) { throw "Test APK non trovato: $testApk" }

Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "install", "-r", $debugApk)
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "install", "-r", $testApk)
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "pm", "clear", "com.onlytechtake.repairapp")
Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "shell", "rm", "-rf", "/sdcard/Android/data/com.onlytechtake.repairapp/files/demo-social-screenshots")

$testClass = "com.onlytechtake.repairapp.demo.SocialStoryDemoScreenshotTest"
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

$rawRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("onlytechtake-demo-screenshots-" + [System.Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $rawRoot | Out-Null
try {
    $deviceScreenshots = "/sdcard/Android/data/com.onlytechtake.repairapp/files/demo-social-screenshots"
    Invoke-Checked -FilePath "adb" -Arguments @("-s", $serial, "pull", $deviceScreenshots, $rawRoot)

    $pulledRoot = Join-Path $rawRoot "demo-social-screenshots"
    if (-not (Test-Path $pulledRoot)) {
        throw "Directory screenshot non trovata dopo adb pull: $pulledRoot"
    }

    Add-Type -AssemblyName System.Drawing

    $expectedCases = @(
        "01-commessa-nuovo-cliente-chiusura",
        "02-categorie-template",
        "03-clienti-privato-azienda"
    )

    foreach ($caseName in $expectedCases) {
        $sourceCase = Join-Path $pulledRoot $caseName
        if (-not (Test-Path $sourceCase)) {
            throw "Caso screenshot mancante: $caseName"
        }

        $targetCase = Join-Path $OutputRoot $caseName
        New-Item -ItemType Directory -Path $targetCase | Out-Null

        $sourceFiles = @(Get-ChildItem -Path $sourceCase -Filter "*.png" -File | Sort-Object Name)
        if ($sourceFiles.Count -eq 0) {
            throw "Nessun PNG generato per il caso $caseName."
        }

        foreach ($sourceFile in $sourceFiles) {
            $targetPath = Join-Path $targetCase $sourceFile.Name
            Convert-ToStoryPng -SourcePath $sourceFile.FullName -TargetPath $targetPath
            Assert-StoryDimensions -Path $targetPath
        }

        $unexpected = @(Get-ChildItem -Path $targetCase -File | Where-Object { $_.Extension -ne ".png" })
        if ($unexpected.Count -gt 0) {
            throw "File non PNG presenti in ${targetCase}: $($unexpected.Name -join ', ')"
        }
    }
} finally {
    if (Test-Path $rawRoot) {
        Remove-Item -LiteralPath $rawRoot -Recurse -Force
    }
}

Write-Host "Screenshot demo generati:"
Write-Host $OutputRoot

Restore-DeviceGlobalSettings
