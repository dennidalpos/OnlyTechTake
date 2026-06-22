<#
.SYNOPSIS
  Installa e avvia l'app Android su un device collegato via adb.
#>

param(
    [ValidateSet("Full", "Demo", "Showcase")]
    [string]$Flavor = "Full",

    [ValidateSet("Debug", "Release")]
    [string]$BuildType = "Debug",

    [string]$ApkPath,

    [switch]$NoInstall,

    [switch]$CleanInstall
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$supportScript = Join-Path $PSScriptRoot "..\support\android_env.ps1"
. $supportScript
Set-AndroidCliEnvironment | Out-Null

function Test-Command($Name) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    return $null -ne $cmd
}

if (-not (Test-Command "adb")) {
    throw "adb non trovato nel PATH. Installa Android Platform Tools o esegui scripts\powershell\android\check_env.ps1."
}

if (-not $ApkPath) {
    $folder = Join-Path $root ("app\build\outputs\apk\" + $Flavor.ToLower() + "\" + $BuildType.ToLower())
    $apk = Get-ChildItem -Path $folder -Filter "*.apk" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $apk) {
        throw "Nessun APK trovato in $folder. Esegui prima scripts\powershell\android\build_installer.ps1."
    }
    $ApkPath = $apk.FullName
} elseif (-not [System.IO.Path]::IsPathRooted($ApkPath)) {
    $ApkPath = Join-Path $root $ApkPath
}

if (-not (Test-Path $ApkPath)) {
    throw "APK non trovato: $ApkPath"
}

adb devices

$packageName = "com.onlytechtake.repairapp"
if ($Flavor -eq "Demo") {
    $packageName = "$packageName.demo"
}
elseif ($Flavor -eq "Showcase") {
    $packageName = "$packageName.showcase"
}
if ($BuildType -eq "Debug") {
    $packageName = "$packageName.debug"
}

if ($CleanInstall) {
    adb uninstall $packageName
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Package non installato o uninstall non necessario." -ForegroundColor Yellow
    }
}

if (-not $NoInstall) {
    adb install -r $ApkPath
    if ($LASTEXITCODE -ne 0) {
        throw "Installazione adb fallita."
    }
}

$activityClass = "com.onlytechtake.repairapp.MainActivity"
adb shell am start -W -n "$packageName/$activityClass"
if ($LASTEXITCODE -ne 0) {
    throw "Avvio app fallito."
}
