<#
.SYNOPSIS
  Esegue Gradle Wrapper con ambiente Android/JDK supportato.

.USAGE
  powershell -ExecutionPolicy Bypass -File scripts\powershell\android\gradle.ps1 -- :app:testFullDebugUnitTest
#>

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$supportScript = Join-Path $PSScriptRoot "..\support\android_env.ps1"
. $supportScript
$resolvedEnvironment = Set-AndroidCliEnvironment

if ($null -eq $resolvedEnvironment -or -not $resolvedEnvironment.JavaHome) {
    throw "Nessun JDK supportato rilevato. Installare Android Studio JBR o JDK 17/21; Java 26 viene ignorato."
}

$gradlew = Join-Path $root "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    throw "gradlew.bat non trovato nella root rilevata: $root"
}

$arguments = @()
foreach ($argument in $args) {
    if ($argument -ne "--") {
        $arguments += $argument
    }
}

Push-Location $root
try {
    & $gradlew @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle fallito con exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
