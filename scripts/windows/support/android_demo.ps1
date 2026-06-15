Add-Type -AssemblyName System.Drawing

function Assert-Command {
    param([Parameter(Mandatory = $true)][string]$Name)

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
        [string[]]$Arguments,

        [string]$FailureMessage = "Scenario strumentato demo fallito."
    )

    $output = & adb @Arguments 2>&1
    $output | ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -ne 0 -or ($output -join "`n") -match "FAILURES!!!|There was \d+ failure|INSTRUMENTATION_FAILED|Process crashed") {
        throw $FailureMessage
    }
}

$script:DeviceSettingsToRestore = @()

function Set-DeviceGlobalSettingTemporary {
    param(
        [Parameter(Mandatory = $true)][string]$Serial,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Value
    )

    $previous = (adb -s $Serial shell settings get global $Name 2>$null).Trim()
    $script:DeviceSettingsToRestore += [PSCustomObject]@{
        Serial = $Serial
        Name = $Name
        Previous = $previous
    }
    adb -s $Serial shell settings put global $Name $Value | Out-Null
}

function Restore-DeviceGlobalSettings {
    for ($index = $script:DeviceSettingsToRestore.Count - 1; $index -ge 0; $index--) {
        $setting = $script:DeviceSettingsToRestore[$index]
        if ($setting.Previous -eq "null" -or [string]::IsNullOrWhiteSpace($setting.Previous)) {
            adb -s $setting.Serial shell settings delete global $setting.Name | Out-Null
        } else {
            adb -s $setting.Serial shell settings put global $setting.Name $setting.Previous | Out-Null
        }
    }
    $script:DeviceSettingsToRestore = @()
}

function Get-RunningEmulatorName {
    param([Parameter(Mandatory = $true)][string]$Serial)

    $name = (adb -s $Serial shell getprop ro.boot.qemu.avd_name 2>$null).Trim()
    if ([string]::IsNullOrWhiteSpace($name)) {
        $name = (adb -s $Serial shell getprop ro.kernel.qemu.avd_name 2>$null).Trim()
    }
    if ([string]::IsNullOrWhiteSpace($name)) {
        $avdOutput = adb -s $Serial emu avd name 2>$null
        $name = $avdOutput | Where-Object { $_ -and $_ -ne "OK" } | Select-Object -First 1
    }
    return $name
}

function Get-EmulatorSerialForAvd {
    param([Parameter(Mandatory = $true)][string]$Name)

    $devices = adb devices | Select-Object -Skip 1 |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -match "^emulator-\d+\s+device$" } |
        ForEach-Object { ($_ -split "\s+")[0] }

    foreach ($serial in $devices) {
        $runningName = Get-RunningEmulatorName -Serial $serial
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

    throw "Timeout in attesa del boot del device $Serial."
}

function Resolve-DemoDeviceSerial {
    param(
        [Parameter(Mandatory = $true)][string]$AndroidHome,
        [string]$AvdName = "OnlyTechTakeDemo",
        [switch]$UseConnectedDevice
    )

    Assert-Command "adb"

    if ($UseConnectedDevice) {
        $serial = Get-ConnectedDeviceSerial
        Wait-ForBoot -Serial $serial
        Write-Host "Uso device fisico $serial. I dati dell'app demo verranno puliti su questo device."
        Write-Host "Disattivo temporaneamente la verifica APK/Play Protect per installazioni adb su questo device."
        Set-DeviceGlobalSettingTemporary -Serial $serial -Name "package_verifier_enable" -Value "0"
        Set-DeviceGlobalSettingTemporary -Serial $serial -Name "verifier_verify_adb_installs" -Value "0"
        Set-DeviceGlobalSettingTemporary -Serial $serial -Name "upload_apk_enable" -Value "0"
        return $serial
    }

    $emulatorPath = Join-Path $AndroidHome "emulator\emulator.exe"
    if (-not (Test-Path $emulatorPath)) {
        throw "emulator.exe non trovato in ANDROID_HOME. Esegui prima check_android_env.ps1."
    }

    # 1. Vediamo se l'emulatore richiesto è già in esecuzione
    $serial = Get-EmulatorSerialForAvd -Name $AvdName
    if ($serial) {
        Wait-ForBoot -Serial $serial
        Write-Host "Uso emulatore in esecuzione $serial ($AvdName)."
        return $serial
    }

    # 2. Se non è in esecuzione, vediamo se esiste tra i definiti
    $availableAvds = & $emulatorPath -list-avds
    if ($availableAvds -contains $AvdName) {
        Write-Host "Avvio emulatore $AvdName..."
        Start-Process -FilePath $emulatorPath -ArgumentList @("-avd", $AvdName, "-no-snapshot-save") -WindowStyle Hidden | Out-Null
        Start-Sleep -Seconds 5
        $deadline = (Get-Date).AddMinutes(2)
        do {
            $serial = Get-EmulatorSerialForAvd -Name $AvdName
            if ($serial) { break }
            Start-Sleep -Seconds 2
        } while ((Get-Date) -lt $deadline)

        if (-not $serial) {
            throw "Emulatore $AvdName non rilevato dopo l'avvio."
        }

        Wait-ForBoot -Serial $serial
        Write-Host "Uso emulatore $serial ($AvdName)."
        return $serial
    }

    # 3. Se non esiste l'AVD specifico, cerchiamo se c'è UN SOLO emulatore già avviato (fallback automatico)
    $runningEmulators = adb devices | Select-Object -Skip 1 |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -match "^emulator-\d+\s+device$" } |
        ForEach-Object { ($_ -split "\s+")[0] }

    if (@($runningEmulators).Count -eq 1) {
        $serial = $runningEmulators[0]
        $actualName = Get-RunningEmulatorName -Serial $serial
        Write-Warning "AVD '$AvdName' non trovato, ma ho rilevato un emulatore in esecuzione: $serial ($actualName)."
        Write-Warning "Uso questo emulatore per la sessione demo."
        Wait-ForBoot -Serial $serial
        return $serial
    }

    # 4. Fallimento finale con suggerimenti
    $avdList = if ($availableAvds) { $availableAvds -join ", " } else { "nessuno" }
    throw @"
AVD '$AvdName' non trovato e nessun emulatore alternativo rilevato in esecuzione.
AVD configurati sul sistema: $avdList.

Crea un emulatore dedicato da Android Studio Device Manager con nome '$AvdName'.
Consigliato: API 36, profilo Pixel/Tablet stabile, risoluzione 1920x1080 o simile.
Lo script non installa system image o modifica l'SDK automaticamente.
Per usare temporaneamente un device fisico collegato, esegui con -UseConnectedDevice.
"@
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

function Invoke-DemoApkBuildAndInstall {
    param(
        [Parameter(Mandatory = $true)][string]$RootPath,
        [Parameter(Mandatory = $true)][string]$Serial,
        [switch]$SkipBuild
    )

    if (-not $SkipBuild) {
        $gradle = Join-Path $RootPath "gradlew.bat"
        Push-Location $RootPath
        try {
            # Removed --no-configuration-cache to speed up repeated builds
            Invoke-Checked -FilePath $gradle -Arguments @(":app:assembleFullDebug", ":app:assembleFullDebugAndroidTest")
        } finally {
            Pop-Location
        }
    } else {
        Write-Host "Salto build APK (richiesto -SkipBuild)."
    }

    $packageName = "com.onlytechtake.repairapp.debug"
    $debugApk = Join-Path $RootPath "app\build\outputs\apk\full\debug\app-full-debug.apk"
    $testApk = Join-Path $RootPath "app\build\outputs\apk\androidTest\full\debug\app-full-debug-androidTest.apk"
    if (-not (Test-Path $debugApk)) { throw "APK debug non trovato: $debugApk" }
    if (-not (Test-Path $testApk)) { throw "Test APK non trovato: $testApk" }

    Write-Host "Installazione APK su $Serial..."
    Invoke-Checked -FilePath "adb" -Arguments @("-s", $Serial, "install", "-r", "-d", $debugApk)
    Invoke-Checked -FilePath "adb" -Arguments @("-s", $Serial, "install", "-r", "-d", $testApk)
    Invoke-Checked -FilePath "adb" -Arguments @("-s", $Serial, "shell", "pm", "clear", $packageName)
}

function Invoke-DemoScreenshotTest {
    param(
        [Parameter(Mandatory = $true)][string]$Serial,
        [Parameter(Mandatory = $true)][string]$TestClass,
        [string]$FailureMessage = "Scenario strumentato demo fallito."
    )

    $runner = "com.onlytechtake.repairapp.debug.test/androidx.test.runner.AndroidJUnitRunner"
    Invoke-InstrumentationChecked -FailureMessage $FailureMessage -Arguments @(
        "-s",
        $Serial,
        "shell",
        "am",
        "instrument",
        "-w",
        "-e",
        "class",
        $TestClass,
        $runner
    )
}

function Pull-DeviceDirectory {
    param(
        [Parameter(Mandatory = $true)][string]$Serial,
        [Parameter(Mandatory = $true)][string]$DevicePath,
        [Parameter(Mandatory = $true)][string]$TargetRoot,
        [Parameter(Mandatory = $true)][string]$PulledDirectoryName
    )

    $rawRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("onlytechtake-demo-" + [System.Guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Path $rawRoot | Out-Null
    try {
        Invoke-Checked -FilePath "adb" -Arguments @("-s", $Serial, "pull", $DevicePath, $rawRoot)
        $pulledRoot = Join-Path $rawRoot $PulledDirectoryName
        if (-not (Test-Path $pulledRoot)) {
            throw "Directory screenshot non trovata dopo adb pull: $pulledRoot"
        }

        if (Test-Path $TargetRoot) {
            Remove-Item -LiteralPath $TargetRoot -Recurse -Force
        }
        New-Item -ItemType Directory -Path $TargetRoot | Out-Null
        Copy-Item -Path (Join-Path $pulledRoot "*") -Destination $TargetRoot -Recurse
    } finally {
        if (Test-Path $rawRoot) {
            Remove-Item -LiteralPath $rawRoot -Recurse -Force
        }
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

function Assert-PngDimensions {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][int]$Width,
        [Parameter(Mandatory = $true)][int]$Height
    )

    $image = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        if ($image.Width -ne $Width -or $image.Height -ne $Height) {
            throw "Dimensioni non valide per ${Path}: $($image.Width)x$($image.Height)."
        }
    } finally {
        $image.Dispose()
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
        # Overlay barra testi centrato al video e della larghezza del video (drawWidth)
        $graphics.FillRectangle($overlayBrush, $drawX, $Height - $captionHeight, $drawWidth, $captionHeight)
        $overlayBrush.Dispose()

        $font = New-Object System.Drawing.Font("Segoe UI", 32, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
        $captionBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)

        # Margine interno testo proporzionale alla larghezza video (drawWidth)
        $hMargin = [int]($drawWidth * 0.12)
        if ($hMargin -lt 30) { $hMargin = 30 }

        # Il rettangolo del testo deve stare dentro drawX e drawWidth
        $captionRect = New-Object System.Drawing.RectangleF(($drawX + $hMargin), ($Height - $captionHeight + 35), ($drawWidth - 2 * $hMargin), 130)

        $format = New-Object System.Drawing.StringFormat
        $format.Alignment = [System.Drawing.StringAlignment]::Center
        $format.LineAlignment = [System.Drawing.StringAlignment]::Center
        $format.Trimming = [System.Drawing.StringTrimming]::EllipsisWord
        $format.FormatFlags = [System.Drawing.StringFormatFlags]::LineLimit

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
