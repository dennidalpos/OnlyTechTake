<#
.SYNOPSIS
  Esegue i controlli locali principali del progetto Android.

.USAGE
  Check locale standard senza signing release:
    .\scripts\windows\check_project.ps1 -Flavor Full -SkipReleaseBuild

  Check locale standard + instrumentation test su device collegato:
    .\scripts\windows\check_project.ps1 -Flavor Full -SkipReleaseBuild -AndroidTests

  Check completo con release firmata:
    .\scripts\windows\check_project.ps1 -Flavor Full

.DESCRIPTION
  Lo script rileva la root del progetto dal proprio percorso e usa solo
  PowerShell, Android SDK, JDK 17+ e Gradle Wrapper. I task Gradle sono
  lanciati in sequenza per evitare file lock su Windows.
  Senza -SkipReleaseBuild verifica anche il signing locale e genera la release.
  Il flavor predefinito e' Full; usare -Flavor Demo per il flavor demo.
#>

param(
    [ValidateSet("Full", "Demo")]
    [string]$Flavor = "Full",

    [switch]$AndroidTests,
    [switch]$SkipReleaseBuild
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

$supportScript = Join-Path $PSScriptRoot "..\support\android_env.ps1"
if (Test-Path $supportScript) {
    . $supportScript
    Set-AndroidCliEnvironment | Out-Null
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

function Invoke-GradleTask {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Tasks
    )

    $gradleArguments = @("--no-configuration-cache") + $Tasks
    Write-Host "Eseguo: .\gradlew.bat $($gradleArguments -join ' ')"
    & $script:gradlew @gradleArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle fallito: $($Tasks -join ' ')"
    }
}

$gradlew = Join-Path $root "gradlew.bat"
Require-File $gradlew "gradlew.bat non trovato nella root rilevata: $root"
Require-File (Join-Path $root "app\build.gradle.kts") "Modulo app Android non trovato."

Push-Location $root
try {
Write-Section "Controlli locali"
$debugVariant = "$Flavor" + "Debug"
$releaseVariant = "$Flavor" + "Release"
$lintTask = ":app:lint$debugVariant"
$compileTask = ":app:compile$debugVariant" + "Kotlin"
$testTask = ":app:test$debugVariant" + "UnitTest"
$assembleDebugTask = ":app:assemble$debugVariant"
$assembleReleaseTask = ":app:assemble$releaseVariant"
$connectedAndroidTestTask = ":app:connected$debugVariant" + "AndroidTest"

Invoke-GradleTask @($lintTask)
Invoke-GradleTask @($compileTask)
Invoke-GradleTask @($testTask)
Invoke-GradleTask @($assembleDebugTask)

if (-not $SkipReleaseBuild) {
    Write-Section "Verifica signing release"
    Assert-AndroidReleaseSigningConfigured -RootPath $root
    Write-Host "Signing release configurato." -ForegroundColor Green

    Invoke-GradleTask @($assembleReleaseTask)
}

if ($AndroidTests) {
    Write-Section "Instrumentation test"
    Invoke-GradleTask @($connectedAndroidTestTask)
}

Write-Section "Completato"
} finally {
    Pop-Location
}
