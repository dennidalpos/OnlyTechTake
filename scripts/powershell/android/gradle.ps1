<#
.SYNOPSIS
  Esegue Gradle Wrapper con ambiente Android/JDK supportato.

.USAGE
  powershell -ExecutionPolicy Bypass -File scripts\powershell\android\gradle.ps1 -- :app:testFullDebugUnitTest
#>

param(
    [switch]$NonInteractive
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path

# Interactive Mode
$arguments = @()
foreach ($argument in $args) {
    if ($argument -ne "--") { $arguments += $argument }
}

if ($arguments.Count -eq 0 -and -not $NonInteractive) {
    Write-Host "`n--- OnlyTechTake Gradle Wrapper ---" -ForegroundColor Cyan
    Write-Host "Esegue i task Gradle con l'ambiente configurato."
    Write-Host "`nTask comuni:"
    Write-Host "1. clean (Pulisce build)"
    Write-Host "2. :app:assembleFullDebug (Build APK Debug)"
    Write-Host "3. :app:testFullDebugUnitTest (Unit Test)"
    Write-Host "4. :app:lintFullDebug (Analisi statica)"
    Write-Host "5. --stop (Ferma daemon Gradle)"
    Write-Host "6. Manuale (Inserisci task)"

    $choice = Read-Host "Scelta [1-6]"
    switch ($choice) {
        "1" { $arguments = @("clean") }
        "2" { $arguments = @(":app:assembleFullDebug") }
        "3" { $arguments = @(":app:testFullDebugUnitTest") }
        "4" { $arguments = @(":app:lintFullDebug") }
        "5" { $arguments = @("--stop") }
        "6" {
            $manual = Read-Host "Inserisci i task (es. clean assembleDebug)"
            $arguments = $manual -split " "
        }
        default { throw "Nessun task selezionato." }
    }
}

$supportScript = Join-Path $PSScriptRoot "..\support\android_env.ps1"
if (Test-Path $supportScript) {
    . $supportScript
    Set-AndroidCliEnvironment | Out-Null
}

$gradlew = Join-Path $root "gradlew.bat"

Push-Location $root
try {
    & $gradlew @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle fallito con exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
