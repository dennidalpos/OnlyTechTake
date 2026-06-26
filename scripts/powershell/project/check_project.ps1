<#
.SYNOPSIS
  Esegue i controlli locali principali del progetto Android.

.USAGE
  Check locale standard senza signing release:
    .\scripts\powershell\project\check_project.ps1 -Flavor Full -SkipReleaseBuild

  Check locale standard + instrumentation test su device collegato:
    .\scripts\powershell\project\check_project.ps1 -Flavor Full -SkipReleaseBuild -AndroidTests

  Check completo con release firmata:
    .\scripts\powershell\project\check_project.ps1 -Flavor Full

.DESCRIPTION
  Lo script rileva la root del progetto dal proprio percorso e usa solo
  PowerShell, Android SDK, JDK 17+ e Gradle Wrapper. I task Gradle sono
  lanciati in sequenza per evitare file lock su Windows.
  Senza -SkipReleaseBuild verifica anche il signing locale e genera la release.
  Il flavor predefinito e' Full; sono supportati anche Demo e Showcase.
#>

param(
    [ValidateSet("Full", "Demo", "Showcase")]
    [string]$Flavor,

    [switch]$AndroidTests,
    [switch]$SkipReleaseBuild,
    [switch]$NonInteractive
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path

# Interactive Mode
if ($PSBoundParameters.Count -eq 0 -and -not $NonInteractive) {
    Write-Host "`n--- OnlyTechTake Project Health Check ---" -ForegroundColor Cyan
    Write-Host "Esegue i controlli principali (Lint, Compile, Unit Test, Assemble)."

    if (-not $Flavor) {
        Write-Host "`nScegli il Flavor:"
        Write-Host "1. Full (default)"
        Write-Host "2. Demo"
        Write-Host "3. Showcase"
        $fChoice = Read-Host "Scelta [1-3]"
        $Flavor = switch ($fChoice) { "2" { "Demo" }; "3" { "Showcase" }; default { "Full" } }
    }

    $tChoice = Read-Host "`nEseguire anche i test strumentati (richiede device)? [y/N]"
    if ($tChoice -eq 'y') { $AndroidTests = $true }

    $rChoice = Read-Host "`nSaltare la build Release? [Y/n]"
    if ($rChoice -ne 'n') { $SkipReleaseBuild = $true }
} else {
    if (-not $Flavor) { $Flavor = "Full" }
}

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

    # Spotless 8.x has serialization issues with Gradle 9.6 Configuration Cache.
    # Also provides better stability for sequential tasks on Windows.
    $gradleArguments = @("--no-configuration-cache") + $Tasks
    Write-Host "Eseguo: .\gradlew.bat $($gradleArguments -join ' ')" -ForegroundColor Gray
    & $script:gradlew @gradleArguments | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle fallito: $($Tasks -join ' ')"
    }
}

$gradlew = Join-Path $root "gradlew.bat"
$script:gradlew = $gradlew
Require-File $gradlew "gradlew.bat non trovato nella root rilevata: $root"
Require-File (Join-Path $root "app\build.gradle.kts") "Modulo app Android non trovato."

Push-Location $root
$sw = [System.Diagnostics.Stopwatch]::StartNew()
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

$sw.Stop()
Write-Section "Completato in $($sw.Elapsed.ToString('mm\:ss'))"
} finally {
    Pop-Location
}
