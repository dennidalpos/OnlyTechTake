<#
.SYNOPSIS
  Esegue linting, formatting check e beautification del progetto.

.USAGE
  Check qualita' standard:
    .\scripts\powershell\project\quality.ps1

  Applica formattazione e rilancia i check:
    .\scripts\powershell\project\quality.ps1 -Apply

  Modalita' non interattiva per CI:
    .\scripts\powershell\project\quality.ps1 -NoPrompt

.DESCRIPTION
  Lo script usa Spotless/ktlint per Kotlin e file testuali, piu' Android Lint
  su tutte le varianti debug e release. Senza -Apply non modifica file; se
  Spotless fallisce in modalita' interattiva propone di eseguire l'autofix.
#>

param(
    [switch]$Apply,
    [switch]$NonInteractive,
    [switch]$SkipLint,
    [string]$Flavor = "Full",
    [string]$BuildType = "Debug",
    [switch]$AllVariants
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path

# Interactive Mode
if ($PSBoundParameters.Count -eq 0 -and -not $NonInteractive) {
    Write-Host "`n--- OnlyTechTake Code Quality Check ---" -ForegroundColor Cyan
    Write-Host "Verifica formattazione (Spotless) e analisi statica (Lint)."

    $Apply = (Read-Host "`nApplicare automaticamente le correzioni di formattazione? [y/N]").ToLower() -eq 'y'
    $SkipLint = (Read-Host "Saltare Android Lint (più veloce)? [y/N]").ToLower() -eq 'y'

    if (-not $SkipLint) {
        Write-Host "`nScegli variante per Lint:"
        Write-Host "1. Solo Full (Default)"
        Write-Host "2. Solo Demo"
        Write-Host "3. Solo Showcase"
        Write-Host "4. TUTTE le varianti (Lento)"
        $lChoice = Read-Host "Opzione [1-4]"

        switch ($lChoice) {
            "2" { $Flavor = "Demo" }
            "3" { $Flavor = "Showcase" }
            "4" { $AllVariants = $true }
            default { $Flavor = "Full" }
        }

        if (-not $AllVariants) {
            $bChoice = Read-Host "Build Type? [1. Debug (default) / 2. Release]"
            if ($bChoice -eq "2") { $BuildType = "Release" }
        }
    }
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
    if (-not (Test-Path -LiteralPath $Path)) {
        throw $Message
    }
}

function Invoke-GradleTask {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Tasks,

        [switch]$AllowFailure
    )

    $gradleArguments = @("--no-configuration-cache") + $Tasks
    Write-Host "Eseguo: .\gradlew.bat $($gradleArguments -join ' ')"
    & $script:gradlew @gradleArguments | Out-Host
    $exitCode = $LASTEXITCODE

    if ($exitCode -ne 0 -and -not $AllowFailure) {
        throw "Gradle fallito: $($Tasks -join ' ')"
    }

    $exitCode
}

function Read-YesNo($Prompt) {
    while ($true) {
        $answer = Read-Host "$Prompt [S/N]"
        switch -Regex ($answer.Trim()) {
            "^(s|si|y|yes)$" { return $true }
            "^(n|no)$" { return $false }
        }
        Write-Host "Risposta non valida. Usa S o N." -ForegroundColor Yellow
    }
}

$gradlew = Join-Path $root "gradlew.bat"
$script:gradlew = $gradlew
Require-File $gradlew "gradlew.bat non trovato nella root rilevata: $root"
Require-File (Join-Path $root "app\build.gradle.kts") "Modulo app Android non trovato."

Push-Location $root
$sw = [System.Diagnostics.Stopwatch]::StartNew()
try {
    Write-Section "Formatting"
    if ($Apply) {
        $null = Invoke-GradleTask @("spotlessApply")
        $null = Invoke-GradleTask @("spotlessCheck")
    } else {
        $spotlessExitCode = Invoke-GradleTask @("spotlessCheck") -AllowFailure
        if ($spotlessExitCode -ne 0) {
            if ($NonInteractive) {
                throw "spotlessCheck fallito. Esegui lo script con -Apply per applicare la formattazione."
            }

            if (Read-YesNo "spotlessCheck ha trovato file da formattare. Vuoi eseguire spotlessApply ora?") {
                $null = Invoke-GradleTask @("spotlessApply")
                $null = Invoke-GradleTask @("spotlessCheck")
            } else {
                throw "spotlessCheck fallito e autofix non eseguito."
            }
        }
    }

    if (-not $SkipLint) {
        Write-Section "Android Lint"

        $lintTasks = @()
        if ($AllVariants) {
            $lintTasks = @(
                ":app:lintFullDebug",
                ":app:lintFullRelease",
                ":app:lintDemoDebug",
                ":app:lintDemoRelease",
                ":app:lintShowcaseDebug",
                ":app:lintShowcaseRelease"
            )
        } else {
            # Default optimization: run lint only on the selected or default variant
            $taskName = ":app:lint$($Flavor)$($BuildType)"
            Write-Host "Running optimized lint for: $Flavor$BuildType" -ForegroundColor Gray
            $lintTasks = @($taskName)
        }

        $null = Invoke-GradleTask $lintTasks
    }

    $sw.Stop()
    Write-Section "Completato in $($sw.Elapsed.ToString('mm\:ss'))"
} finally {
    Pop-Location
}
