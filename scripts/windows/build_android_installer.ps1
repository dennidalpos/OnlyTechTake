<#
.SYNOPSIS
  Genera APK o AAB Android da Windows.

.USAGE
  Debug APK:
    .\scripts\windows\build_android_installer.ps1 -BuildType Debug -OutputType Apk

  Release APK:
    .\scripts\windows\build_android_installer.ps1 -BuildType Release -OutputType Apk

  Release AAB:
    .\scripts\windows\build_android_installer.ps1 -BuildType Release -OutputType Aab

  Pulizia + build:
    .\scripts\windows\build_android_installer.ps1 -BuildType Release -OutputType Apk -Clean

  Build forzata senza cancellare app/build:
    .\scripts\windows\build_android_installer.ps1 -BuildType Release -OutputType Apk -RerunTasks

  Build e installazione su device collegato:
    .\scripts\windows\build_android_installer.ps1 -BuildType Debug -OutputType Apk -Install

.PARAMETER BuildType
  Debug oppure Release.

.PARAMETER OutputType
  Apk oppure Aab.

.PARAMETER Clean
  Esegue gradlew clean prima della build.

.PARAMETER RerunTasks
  Aggiunge --rerun-tasks alla build Gradle per rigenerare gli output senza clean.

.PARAMETER Install
  Installa l'APK generato via adb. Valido solo per OutputType Apk.

.DESCRIPTION
  Lo script rileva la root del progetto dal proprio percorso e usa gradlew.bat.
  Non crea né salva password di keystore.
  La build release richiede keystore.properties dalla root.
#>

param(
    [ValidateSet("Debug", "Release")]
    [string]$BuildType = "Debug",

    [ValidateSet("Apk", "Aab")]
    [string]$OutputType = "Apk",

    [switch]$Clean,

    [switch]$RerunTasks,

    [switch]$Install
)

$ErrorActionPreference = "Stop"

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

function Require-File($Path, $Message) {
    if (-not (Test-Path $Path)) {
        throw $Message
    }
}

function Test-Command($Name) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    return $null -ne $cmd
}

$gradlew = Join-Path $root "gradlew.bat"

Write-Section "Ambiente Android"
if ($null -ne $resolvedEnvironment -and $resolvedEnvironment.JavaHome) {
    Write-Host "JAVA_HOME=$($resolvedEnvironment.JavaHome)"
} elseif ($env:JAVA_HOME) {
    Write-Host "JAVA_HOME=$env:JAVA_HOME"
} else {
    Write-Host "JAVA_HOME non impostato e nessun JDK 17+ rilevato automaticamente." -ForegroundColor Yellow
}

if ($null -ne $resolvedEnvironment -and $resolvedEnvironment.AndroidSdk) {
    Write-Host "ANDROID_HOME=$($resolvedEnvironment.AndroidSdk)"
} elseif ($env:ANDROID_HOME) {
    Write-Host "ANDROID_HOME=$env:ANDROID_HOME"
} elseif ($env:ANDROID_SDK_ROOT) {
    Write-Host "ANDROID_SDK_ROOT=$env:ANDROID_SDK_ROOT"
} else {
    Write-Host "ANDROID_HOME / ANDROID_SDK_ROOT non impostati e nessun SDK rilevato automaticamente." -ForegroundColor Yellow
}

Write-Section "Verifica progetto"
Require-File $gradlew "gradlew.bat non trovato nella root rilevata: $root"

if (-not (Test-Command "java")) {
    throw "Java non trovato nel PATH. Installa/configura JDK 17+ o Android Studio."
}

if (-not (Test-Command "javac")) {
    throw "javac non trovato nel PATH. Configura un JDK 17+ o Android Studio JBR."
}

if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) {
    throw "Android SDK non configurato. Imposta ANDROID_HOME/ANDROID_SDK_ROOT o installa Android SDK con Android Studio."
}

$androidSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }
if (-not (Test-AndroidSdkPlatform -Path $androidSdk -ApiLevel 36)) {
    throw "Android SDK Platform 36 non trovata. Installa android-36 da Android Studio SDK Manager."
}

if (-not (Test-AndroidBuildToolsInstalled -Path $androidSdk)) {
    throw "Android SDK Build Tools non trovati. Installa Android SDK Build Tools da Android Studio SDK Manager."
}

if ($Install -and $OutputType -ne "Apk") {
    throw "Installazione via adb disponibile solo per APK, non per AAB."
}

if ($Install -and -not (Test-Command "adb")) {
    throw "adb non trovato nel PATH. Aggiungi Android platform-tools al PATH."
}

if ($BuildType -eq "Release") {
    Write-Section "Verifica signing release"
    Assert-AndroidReleaseSigningConfigured -RootPath $root
    Write-Host "Signing release configurato." -ForegroundColor Green
}

Write-Host "Root progetto: $root"
Write-Host "BuildType: $BuildType"
Write-Host "OutputType: $OutputType"

Push-Location $root
try {
if ($Clean) {
    Write-Section "Clean"
    Write-Host "Fermo eventuali daemon Gradle per ridurre i file lock su Windows."
    & $gradlew --stop
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle --stop fallito."
    }

    & $gradlew --no-configuration-cache clean
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle clean fallito."
    }
}

Write-Section "Build"

$task = $null

if ($OutputType -eq "Apk" -and $BuildType -eq "Debug") {
    $task = "assembleDebug"
} elseif ($OutputType -eq "Apk" -and $BuildType -eq "Release") {
    $task = "assembleRelease"
} elseif ($OutputType -eq "Aab" -and $BuildType -eq "Release") {
    $task = "bundleRelease"
} elseif ($OutputType -eq "Aab" -and $BuildType -eq "Debug") {
    $task = "bundleDebug"
}

$gradleArguments = @()
$gradleArguments += "--no-configuration-cache"
if ($RerunTasks) {
    $gradleArguments += "--rerun-tasks"
}
$gradleArguments += $task

Write-Host "Eseguo: .\gradlew.bat $($gradleArguments -join ' ')"
& $gradlew @gradleArguments

if ($LASTEXITCODE -ne 0) {
    throw "Build Gradle fallita."
}
} finally {
    Pop-Location
}

Write-Section "Ricerca output"

$outputFile = $null

if ($OutputType -eq "Apk") {
    $folder = Join-Path $root ("app\build\outputs\apk\" + $BuildType.ToLower())
    $outputFile = Get-ChildItem -Path $folder -Filter "*.apk" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
} else {
    $folder = Join-Path $root ("app\build\outputs\bundle\" + $BuildType.ToLower())
    $outputFile = Get-ChildItem -Path $folder -Filter "*.aab" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

if ($null -eq $outputFile) {
    throw "Build completata ma file output non trovato."
}

Write-Host "File generato:" -ForegroundColor Green
Write-Host $outputFile.FullName

if ($Install) {
    Write-Section "Installazione via adb"
    adb devices
    adb install -r $outputFile.FullName
    if ($LASTEXITCODE -ne 0) {
        throw "Installazione adb fallita."
    }
    Write-Host "Installazione completata." -ForegroundColor Green
}

Write-Section "Completato"
