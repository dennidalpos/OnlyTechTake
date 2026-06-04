<#
.SYNOPSIS
  Installa e avvia l'app Android su un device collegato via adb.

.USAGE
  Installa e avvia APK debug:
    .\scripts\windows\start_android_app.ps1

  Avvia senza reinstallare:
    .\scripts\windows\start_android_app.ps1 -NoInstall

  Usa un APK specifico:
    .\scripts\windows\start_android_app.ps1 -ApkPath app\build\outputs\apk\debug\app-debug.apk

.DESCRIPTION
  Lo script rileva la root del progetto dal proprio percorso. Un ApkPath
  relativo viene risolto rispetto alla root del progetto.
#>

param(
    [ValidateSet("Debug", "Release")]
    [string]$BuildType = "Debug",

    [string]$ApkPath,

    [switch]$NoInstall
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

$supportScript = Join-Path $PSScriptRoot "support\android_env.ps1"
if (Test-Path $supportScript) {
    . $supportScript
    Set-AndroidCliEnvironment | Out-Null
}

function Test-Command($Name) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    return $null -ne $cmd
}

if (-not (Test-Command "adb")) {
    throw "adb non trovato nel PATH. Installa Android platform-tools o esegui check_android_env.ps1."
}

if (-not $ApkPath) {
    $folder = Join-Path $root ("app\build\outputs\apk\" + $BuildType.ToLower())
    $apk = Get-ChildItem -Path $folder -Filter "*.apk" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $apk) {
        throw "Nessun APK trovato in $folder. Esegui prima build_android_installer.ps1."
    }
    $ApkPath = $apk.FullName
} elseif (-not [System.IO.Path]::IsPathRooted($ApkPath)) {
    $ApkPath = Join-Path $root $ApkPath
}

if (-not (Test-Path $ApkPath)) {
    throw "APK non trovato: $ApkPath"
}

adb devices

if (-not $NoInstall) {
    adb install -r $ApkPath
    if ($LASTEXITCODE -ne 0) {
        throw "Installazione adb fallita."
    }
}

adb shell am start -W -n "com.onlytechtake.repairapp/.MainActivity"
if ($LASTEXITCODE -ne 0) {
    throw "Avvio app fallito."
}
