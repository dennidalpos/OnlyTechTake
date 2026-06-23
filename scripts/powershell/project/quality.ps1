<#
.SYNOPSIS
  Esegue formatting check/apply e Android Lint per un progetto Android/Gradle.

.USAGE
  Check standard:
    .\scripts\powershell\project\quality_optimized.ps1 -NonInteractive

  Applica formattazione e poi esegue lint:
    .\scripts\powershell\project\quality_optimized.ps1 -Apply

  Solo formattazione, senza lint:
    .\scripts\powershell\project\quality_optimized.ps1 -SkipLint

  Lint di un modulo diverso:
    .\scripts\powershell\project\quality_optimized.ps1 -Module ':feature:camera'

  Lint task espliciti, se vuoi controllare varianti specifiche:
    .\scripts\powershell\project\quality_optimized.ps1 -LintTasks ':app:lintFullDebug', ':app:lintFullRelease'

.DESCRIPTION
  Default ottimizzato: rispetta la configurazione Gradle del progetto e NON forza
  --no-configuration-cache. Se una build ha problemi con la configuration cache,
  usa -ConfigurationCache Off. Se vuoi forzarla, usa -ConfigurationCache On.
#>

[CmdletBinding()]
param(
    [switch]$Apply,
    [Alias('NoPrompt')]
    [switch]$NonInteractive,
    [switch]$SkipFormat,
    [switch]$SkipLint,
    [string]$Module = ':app',
    [string[]]$LintTasks,
    [ValidateSet('Default', 'On', 'Off')]
    [string]$ConfigurationCache = 'Default',
    [switch]$RefreshDependencies,
    [switch]$GradleContinue,
    [switch]$Stacktrace,
    [switch]$Info
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Write-Section {
    param([Parameter(Mandatory = $true)][string]$Text)
    Write-Host ''
    Write-Host "== $Text ==" -ForegroundColor Cyan
}

function Find-RepoRoot {
    param([Parameter(Mandatory = $true)][string]$StartDirectory)

    $current = (Resolve-Path -LiteralPath $StartDirectory).Path
    while ($true) {
        $hasWrapper = Test-Path -LiteralPath (Join-Path $current 'gradlew.bat') -PathType Leaf
        $hasSettings =
            (Test-Path -LiteralPath (Join-Path $current 'settings.gradle.kts') -PathType Leaf) -or
            (Test-Path -LiteralPath (Join-Path $current 'settings.gradle') -PathType Leaf)

        if ($hasWrapper -and $hasSettings) {
            return $current
        }

        $parent = Split-Path -Path $current -Parent
        if ([string]::IsNullOrWhiteSpace($parent) -or $parent -eq $current) {
            throw "Root Gradle non trovata partendo da: $StartDirectory"
        }
        $current = $parent
    }
}

function Require-File {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Message
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw $Message
    }
}

function Read-YesNo {
    param([Parameter(Mandatory = $true)][string]$Prompt)

    while ($true) {
        $answer = Read-Host "$Prompt [S/N]"
        switch -Regex ($answer.Trim()) {
            '^(s|si|sì|y|yes)$' { return $true }
            '^(n|no)$' { return $false }
        }
        Write-Host 'Risposta non valida. Usa S o N.' -ForegroundColor Yellow
    }
}

function Get-GradleCommonArguments {
    $arguments = @()

    switch ($ConfigurationCache) {
        'On'  { $arguments += '--configuration-cache' }
        'Off' { $arguments += '--no-configuration-cache' }
    }

    if ($RefreshDependencies) { $arguments += '--refresh-dependencies' }
    if ($GradleContinue) { $arguments += '--continue' }
    if ($Stacktrace) { $arguments += '--stacktrace' }
    if ($Info) { $arguments += '--info' }

    return $arguments
}

function Invoke-Gradle {
    param(
        [Parameter(Mandatory = $true)][string[]]$Tasks,
        [switch]$AllowFailure
    )

    $arguments = (Get-GradleCommonArguments) + $Tasks
    Write-Host "Eseguo: .\gradlew.bat $($arguments -join ' ')"
    & $script:GradleWrapper @arguments | Out-Host
    $exitCode = $LASTEXITCODE

    if ($exitCode -ne 0 -and -not $AllowFailure) {
        throw "Gradle fallito: $($Tasks -join ' ')"
    }

    return $exitCode
}

$script:Root = Find-RepoRoot -StartDirectory $PSScriptRoot
$script:GradleWrapper = Join-Path $script:Root 'gradlew.bat'

$supportScript = Join-Path $PSScriptRoot '..\support\android_env.ps1'
if (Test-Path -LiteralPath $supportScript -PathType Leaf) {
    . $supportScript
    Set-AndroidCliEnvironment | Out-Null
}

Require-File $script:GradleWrapper "gradlew.bat non trovato nella root rilevata: $script:Root"

if ($PSBoundParameters.Count -eq 0 -and -not $NonInteractive) {
    Write-Host "`n--- Android/Gradle Quality Check ---" -ForegroundColor Cyan
    Write-Host 'Esegue Spotless e Android Lint.'

    $applyChoice = Read-Host "`nApplicare automaticamente le correzioni di formattazione? [y/N]"
    if ($applyChoice -eq 'y') { $Apply = $true }

    $skipLintChoice = Read-Host "`nSaltare Android Lint? [y/N]"
    if ($skipLintChoice -eq 'y') { $SkipLint = $true }
}

Push-Location $script:Root
try {
    Write-Section 'Root repository'
    Write-Host $script:Root

    if (-not $SkipFormat) {
        Write-Section 'Formatting'
        if ($Apply) {
            $null = Invoke-Gradle -Tasks @('spotlessApply', 'spotlessCheck')
        } else {
            $spotlessExitCode = Invoke-Gradle -Tasks @('spotlessCheck') -AllowFailure
            if ($spotlessExitCode -ne 0) {
                if ($NonInteractive) {
                    throw 'spotlessCheck fallito. Rilancia con -Apply per applicare la formattazione.'
                }

                if (Read-YesNo 'spotlessCheck ha trovato file da formattare. Vuoi eseguire spotlessApply ora?') {
                    $null = Invoke-Gradle -Tasks @('spotlessApply', 'spotlessCheck')
                } else {
                    throw 'spotlessCheck fallito e autofix non eseguito.'
                }
            }
        }
    }

    if (-not $SkipLint) {
        Write-Section 'Android Lint'
        $effectiveLintTasks = @()
        if ($PSBoundParameters.ContainsKey('LintTasks') -and $LintTasks.Count -gt 0) {
            $effectiveLintTasks = $LintTasks
        } else {
            $effectiveLintTasks = @("${Module}:lint")
        }

        $null = Invoke-Gradle -Tasks $effectiveLintTasks
    }

    Write-Section 'Completato'
} finally {
    Pop-Location
}
