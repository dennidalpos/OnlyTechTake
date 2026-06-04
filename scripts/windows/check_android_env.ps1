<#
.SYNOPSIS
  Controlla ambiente Android su Windows.

.USAGE
  .\scripts\windows\check_android_env.ps1

.DESCRIPTION
  Verifica Java, Gradle wrapper, Android SDK e adb.
  Lo script rileva la root del progetto dal proprio percorso.
#>

$ErrorActionPreference = "Continue"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

$supportScript = Join-Path $PSScriptRoot "support\android_env.ps1"
if (Test-Path $supportScript) {
    . $supportScript
    $resolvedEnvironment = Set-AndroidCliEnvironment
} else {
    throw "Helper ambiente Android non trovato: $supportScript"
}

function Write-Section($Text) {
    Write-Host ""
    Write-Host "== $Text ==" -ForegroundColor Cyan
}

function Test-Command($Name) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    return $null -ne $cmd
}

$canRunGradle = $true
$requiredCompileSdk = 36

Write-Section "Autodetect ambiente"
if ($null -ne $resolvedEnvironment -and $resolvedEnvironment.JavaHome) {
    Write-Host "JDK rilevato: $($resolvedEnvironment.JavaHome) (Java $($resolvedEnvironment.JavaMajorVersion))" -ForegroundColor Green
} else {
    Write-Host "Nessun JDK 17+ rilevato automaticamente." -ForegroundColor Yellow
}

if ($null -ne $resolvedEnvironment -and $resolvedEnvironment.AndroidSdk) {
    Write-Host "Android SDK rilevato: $($resolvedEnvironment.AndroidSdk)" -ForegroundColor Green
} else {
    Write-Host "Nessun Android SDK rilevato automaticamente." -ForegroundColor Yellow
}

Write-Section "Java"
if (Test-Command "java") {
    & java -version 2>&1 | ForEach-Object { Write-Host $_ }
} else {
    Write-Host "Java non trovato nel PATH." -ForegroundColor Red
    Write-Host "Installa Android Studio/JDK oppure configura JAVA_HOME."
    $canRunGradle = $false
}

Write-Section "JDK"
if (Test-Command "javac") {
    javac -version
} else {
    Write-Host "javac non trovato nel PATH: sembra configurata una JRE, non un JDK." -ForegroundColor Red
    Write-Host "Android Gradle Plugin richiede JDK 17 o superiore. Installa JDK 17+ o usa il JBR di Android Studio."
    $canRunGradle = $false
}

Write-Section "JAVA_HOME"
if ($env:JAVA_HOME) {
    Write-Host "JAVA_HOME=$env:JAVA_HOME" -ForegroundColor Green
} else {
    Write-Host "JAVA_HOME non impostato. Può andare bene se Android Studio gestisce JDK, ma per CLI è consigliato impostarlo." -ForegroundColor Yellow
}

Write-Section "Gradle Wrapper"
$gradlew = Join-Path $root "gradlew.bat"
if (Test-Path $gradlew) {
    Write-Host "Trovato: $gradlew" -ForegroundColor Green
} else {
    Write-Host "gradlew.bat non trovato nella root rilevata." -ForegroundColor Red
    Write-Host "Root rilevata: $root"
    $canRunGradle = $false
}

Write-Section "Android SDK"
if ($env:ANDROID_HOME) {
    Write-Host "ANDROID_HOME=$env:ANDROID_HOME" -ForegroundColor Green
} elseif ($env:ANDROID_SDK_ROOT) {
    Write-Host "ANDROID_SDK_ROOT=$env:ANDROID_SDK_ROOT" -ForegroundColor Green
} else {
    Write-Host "ANDROID_HOME / ANDROID_SDK_ROOT non impostati." -ForegroundColor Yellow
    Write-Host "Android Studio può comunque funzionare, ma per CLI è consigliato impostare il percorso SDK."
}

$androidSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }
if ($androidSdk) {
    if (Test-AndroidSdkPlatform -Path $androidSdk -ApiLevel $requiredCompileSdk) {
        Write-Host "Platform android-$requiredCompileSdk trovata." -ForegroundColor Green
    } else {
        Write-Host "Platform android-$requiredCompileSdk non trovata in $androidSdk\platforms." -ForegroundColor Red
        Write-Host "Installa Android SDK Platform $requiredCompileSdk da Android Studio SDK Manager."
        $canRunGradle = $false
    }

    if (Test-AndroidBuildToolsInstalled -Path $androidSdk) {
        Write-Host "Android SDK Build Tools trovati." -ForegroundColor Green
    } else {
        Write-Host "Android SDK Build Tools non trovati in $androidSdk\build-tools." -ForegroundColor Red
        Write-Host "Installa Android SDK Build Tools da Android Studio SDK Manager."
        $canRunGradle = $false
    }
} else {
    $canRunGradle = $false
}

Write-Section "ADB"
if (Test-Command "adb") {
    adb version
    Write-Host ""
    Write-Host "Device collegati:" -ForegroundColor Cyan
    adb devices
} else {
    Write-Host "adb non trovato nel PATH." -ForegroundColor Yellow
    Write-Host "Aggiungi platform-tools al PATH se vuoi installare APK via USB."
}

Write-Section "Gradle Wrapper"
if ($canRunGradle) {
    Push-Location $root
    try {
        & $gradlew --version
    } finally {
        Pop-Location
    }
} else {
    Write-Host "Saltato: configura prima JDK 17 e gradlew.bat." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Controllo completato."

if (-not $canRunGradle) {
    exit 1
}
