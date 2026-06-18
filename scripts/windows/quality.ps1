<#
.SYNOPSIS
  Esegue linting, formatting check e beautification del progetto.

.USAGE
  Check qualita' standard:
    .\scripts\windows\quality.ps1

  Applica formattazione e rilancia i check:
    .\scripts\windows\quality.ps1 -Apply

  Modalita' non interattiva per CI:
    .\scripts\windows\quality.ps1 -NoPrompt

.DESCRIPTION
  Lo script usa Spotless/ktlint per Kotlin e file testuali, piu' Android Lint
  su tutte le varianti debug e release. Senza -Apply non modifica file; se
  Spotless fallisce in modalita' interattiva propone di eseguire l'autofix.
#>

param(
    [switch]$Apply,
    [switch]$NoPrompt,
    [switch]$SkipLint
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
Require-File $gradlew "gradlew.bat non trovato nella root rilevata: $root"
Require-File (Join-Path $root "app\build.gradle.kts") "Modulo app Android non trovato."

Push-Location $root
try {
    Write-Section "Formatting"
    if ($Apply) {
        $null = Invoke-GradleTask @("spotlessApply")
        $null = Invoke-GradleTask @("spotlessCheck")
    } else {
        $spotlessExitCode = Invoke-GradleTask @("spotlessCheck") -AllowFailure
        if ($spotlessExitCode -ne 0) {
            if ($NoPrompt) {
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
        $lintTasks = @(
            ":app:lintFullDebug",
            ":app:lintFullRelease",
            ":app:lintDemoDebug",
            ":app:lintDemoRelease",
            ":app:lintShowcaseDebug",
            ":app:lintShowcaseRelease"
        )
        $null = Invoke-GradleTask $lintTasks
    }

    Write-Section "Completato"
} finally {
    Pop-Location
}
