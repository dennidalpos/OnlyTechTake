<#
.SYNOPSIS
  Controlla ambiente Android per build CLI.

.USAGE
  powershell -ExecutionPolicy Bypass -File scripts\powershell\android\check_env.ps1
#>

param(
    [switch]$NonInteractive
)

$ErrorActionPreference = "Continue"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path

# Banner
if (-not $NonInteractive) {
    Write-Host "`n--- OnlyTechTake Environment Check ---" -ForegroundColor Cyan
    Write-Host "Verifica la configurazione di JDK, Android SDK e strumenti CLI."
}

$supportScript = Join-Path $PSScriptRoot "..\support\android_env.ps1"
. $supportScript
$resolvedEnvironment = Set-AndroidCliEnvironment

function Write-Section($Text) {
    Write-Host ""
    Write-Host "== $Text ==" -ForegroundColor Cyan
}

function Test-Command($Name) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    return $null -ne $cmd
}

$canRunGradle = $true
$requiredCompileSdk = if ($resolvedEnvironment.RequiredCompileSdk) { $resolvedEnvironment.RequiredCompileSdk } else { 37 }

Write-Section "Autodetect ambiente"
if ($null -ne $resolvedEnvironment -and $resolvedEnvironment.JavaHome) {
    Write-Host "JDK supportato rilevato: $($resolvedEnvironment.JavaHome) (Java $($resolvedEnvironment.JavaMajorVersion))" -ForegroundColor Green
} else {
    Write-Host "Nessun JDK 17/21 rilevato automaticamente. Java 26 o altre versioni non supportate vengono ignorate." -ForegroundColor Yellow
    $canRunGradle = $false
}

if ($null -ne $resolvedEnvironment -and $resolvedEnvironment.AndroidSdk) {
    Write-Host "Android SDK rilevato: $($resolvedEnvironment.AndroidSdk)" -ForegroundColor Green
} else {
    Write-Host "Nessun Android SDK rilevato automaticamente." -ForegroundColor Yellow
    $canRunGradle = $false
}

Write-Section "Java"
if (Test-Command "java") {
    & java -version 2>&1 | ForEach-Object { Write-Host $_ }
} else {
    Write-Host "Java non trovato nel PATH dopo autodetect." -ForegroundColor Red
    $canRunGradle = $false
}

Write-Section "JDK"
if (Test-Command "javac") {
    javac -version
} else {
    Write-Host "javac non trovato nel PATH: configura Android Studio JBR o JDK 17/21." -ForegroundColor Red
    $canRunGradle = $false
}

Write-Section "JAVA_HOME"
if ($env:JAVA_HOME) {
    Write-Host "JAVA_HOME=$env:JAVA_HOME" -ForegroundColor Green
} else {
    Write-Host "JAVA_HOME non impostato." -ForegroundColor Yellow
}

Write-Section "Gradle Wrapper"
$gradlew = Join-Path $root "gradlew.bat"
if (Test-Path $gradlew) {
    Write-Host "Trovato: $gradlew" -ForegroundColor Green
} else {
    Write-Host "gradlew.bat non trovato nella root rilevata: $root" -ForegroundColor Red
    $canRunGradle = $false
}

Write-Section "Android SDK"
$androidSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }
if ($androidSdk) {
    Write-Host "ANDROID_HOME=$androidSdk" -ForegroundColor Green

    if (Test-AndroidSdkPlatform -Path $androidSdk -ApiLevel $requiredCompileSdk) {
        Write-Host "Platform android-$requiredCompileSdk trovata." -ForegroundColor Green
    } else {
        Write-Host "Platform android-$requiredCompileSdk non trovata in $androidSdk\platforms." -ForegroundColor Red
        $canRunGradle = $false
    }

    if (Test-AndroidBuildToolsInstalled -Path $androidSdk) {
        Write-Host "Android SDK Build Tools trovati." -ForegroundColor Green
    } else {
        Write-Host "Android SDK Build Tools non trovati in $androidSdk\build-tools." -ForegroundColor Red
        $canRunGradle = $false
    }
} else {
    Write-Host "ANDROID_HOME / ANDROID_SDK_ROOT non impostati." -ForegroundColor Red
    $canRunGradle = $false
}

Write-Section "ADB"
if (Test-Command "adb") {
    adb version
    Write-Host ""
    Write-Host "Device collegati:" -ForegroundColor Cyan
    adb devices
} else {
    Write-Host "adb non trovato nel PATH. Installa Android Platform Tools se vuoi installare APK via USB." -ForegroundColor Yellow
}

Write-Section "Gradle Wrapper"
if ($canRunGradle) {
    Push-Location $root
    try {
        & $gradlew --version
        if ($LASTEXITCODE -ne 0) {
            $canRunGradle = $false
        }
    } finally {
        Pop-Location
    }
} else {
    Write-Host "Saltato: configura prima JDK 17/21, Android SDK e Gradle wrapper." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Controllo completato."

if (-not $NonInteractive) {
    Read-Host "`nPremi INVIO per uscire..."
}

if (-not $canRunGradle) {
    exit 1
}
