<#
.SYNOPSIS
  Genera APK o AAB Android e opzionalmente installa APK debug/release su device.
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
$supportScript = Join-Path $PSScriptRoot "..\support\android_env.ps1"
. $supportScript
$resolvedEnvironment = Set-AndroidCliEnvironment

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
} else {
    throw "Nessun JDK supportato rilevato. Installare Android Studio JBR o JDK 17/21; Java 26 viene ignorato."
}

if ($null -ne $resolvedEnvironment -and $resolvedEnvironment.AndroidSdk) {
    Write-Host "ANDROID_HOME=$($resolvedEnvironment.AndroidSdk)"
} else {
    throw "Android SDK non configurato. Imposta ANDROID_HOME/ANDROID_SDK_ROOT o installa Android SDK con Android Studio."
}

Write-Section "Verifica progetto"
Require-File $gradlew "gradlew.bat non trovato nella root rilevata: $root"

if (-not (Test-Command "java")) {
    throw "Java non trovato nel PATH dopo autodetect."
}

if (-not (Test-Command "javac")) {
    throw "javac non trovato nel PATH dopo autodetect."
}

$androidSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }
if (-not (Test-AndroidSdkPlatform -Path $androidSdk -ApiLevel 37)) {
    throw "Android SDK Platform 37 non trovata. Installa android-37 da Android Studio SDK Manager."
}

if (-not (Test-AndroidBuildToolsInstalled -Path $androidSdk)) {
    throw "Android SDK Build Tools non trovati. Installa Android SDK Build Tools da Android Studio SDK Manager."
}

if ($Install -and $OutputType -ne "Apk") {
    throw "Installazione via adb disponibile solo per APK, non per AAB."
}

if ($Install -and -not (Test-Command "adb")) {
    throw "adb non trovato nel PATH. Installa Android Platform Tools."
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

    $gradleArguments = @("--no-configuration-cache")
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
