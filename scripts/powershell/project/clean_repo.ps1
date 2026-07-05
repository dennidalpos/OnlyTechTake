<#
.SYNOPSIS
  Pulizia sicura degli output generati di un progetto Android/Gradle su Windows.

.USAGE
  Anteprima:
    .\scripts\powershell\project\clean_repo_optimized.ps1 -DryRun

  Pulizia standard, senza toccare cache globali e metadati IDE:
    .\scripts\powershell\project\clean_repo_optimized.ps1

  Pulizia profonda delle cache Gradle/Kotlin locali al repository:
    .\scripts\powershell\project\clean_repo_optimized.ps1 -Deep -StopGradle

  Reset mirato dei metadati Android Studio locali al progetto, non dell'intera .idea:
    .\scripts\powershell\project\clean_repo_optimized.ps1 -ResetIdeProjectMetadata

  Pulizia cache di sistema Android Studio in %LOCALAPPDATA%\Google\AndroidStudio*:
    .\scripts\powershell\project\clean_repo_optimized.ps1 -PurgeAndroidStudioCaches -StopGradle

.DESCRIPTION
  Default conservativo: rimuove output generati nel working tree, ma NON cancella
  .gradle, .idea, local.properties, keystore, SDK o cache globali Gradle.

  Usa -Deep solo quando sospetti cache Gradle/Kotlin corrotte nel repository.
  Usa -PurgeAndroidStudioCaches solo con Android Studio chiuso.
#>

[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = 'Medium')]
param(
    [switch]$DryRun,
    [switch]$Deep,
    [Alias('IncludeIde')]
    [switch]$ResetIdeProjectMetadata,
    [switch]$PurgeAndroidStudioCaches,
    [switch]$StopGradle,
    [switch]$NonInteractive,
    [switch]$FailOnError
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:Failures = New-Object System.Collections.Generic.List[string]
$script:ExcludedRelativePaths = @(
    'reports\demo-assets',
    'landing\build'
)

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

$script:Root = Find-RepoRoot -StartDirectory $PSScriptRoot

function Resolve-RepoPath {
    param([Parameter(Mandatory = $true)][string]$RelativePath)

    $candidate = Join-Path $script:Root $RelativePath
    $fullPath = [System.IO.Path]::GetFullPath($candidate)
    $rootFull = [System.IO.Path]::GetFullPath($script:Root).TrimEnd(
        [System.IO.Path]::DirectorySeparatorChar,
        [System.IO.Path]::AltDirectorySeparatorChar
    )
    $rootWithSeparator = $rootFull + [System.IO.Path]::DirectorySeparatorChar

    if ($fullPath -ne $rootFull -and -not $fullPath.StartsWith($rootWithSeparator, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Percorso fuori dalla root del repository: $RelativePath"
    }

    return $fullPath
}

function Get-RepoRelativePath {
    param([Parameter(Mandatory = $true)][string]$FullPath)

    $full = [System.IO.Path]::GetFullPath($FullPath)
    $rootFull = [System.IO.Path]::GetFullPath($script:Root).TrimEnd(
        [System.IO.Path]::DirectorySeparatorChar,
        [System.IO.Path]::AltDirectorySeparatorChar
    )
    $rootWithSeparator = $rootFull + [System.IO.Path]::DirectorySeparatorChar

    if ($full -eq $rootFull) { return '.' }
    if ($full.StartsWith($rootWithSeparator, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $full.Substring($rootWithSeparator.Length)
    }

    throw "Percorso fuori dalla root del repository: $FullPath"
}

function Test-IsExcludedRepoPath {
    param([Parameter(Mandatory = $true)][string]$FullPath)

    $normalizedPath = [System.IO.Path]::GetFullPath($FullPath).TrimEnd(
        [System.IO.Path]::DirectorySeparatorChar,
        [System.IO.Path]::AltDirectorySeparatorChar
    )

    foreach ($relativePath in $script:ExcludedRelativePaths) {
        $excludedPath = (Resolve-RepoPath -RelativePath $relativePath).TrimEnd(
            [System.IO.Path]::DirectorySeparatorChar,
            [System.IO.Path]::AltDirectorySeparatorChar
        )
        $excludedPathWithSeparator = $excludedPath + [System.IO.Path]::DirectorySeparatorChar

        if ($normalizedPath -eq $excludedPath -or $normalizedPath.StartsWith($excludedPathWithSeparator, [System.StringComparison]::OrdinalIgnoreCase)) {
            return $true
        }
    }

    return $false
}

function Remove-PathSafe {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$DisplayPath,
        [string]$Reason = 'Remove generated item'
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        Write-Host "Skip: $DisplayPath"
        return
    }

    if ($DryRun) {
        Write-Host "DryRun: rimuoverei $DisplayPath" -ForegroundColor Yellow
        return
    }

    if (-not $PSCmdlet.ShouldProcess($Path, $Reason)) {
        return
    }

    try {
        Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction Stop
        Write-Host "Rimosso: $DisplayPath" -ForegroundColor Green
    } catch {
        $message = "Non rimosso: $DisplayPath - $($_.Exception.Message)"
        $script:Failures.Add($message)
        Write-Warning $message
    }
}

function Remove-RepoItem {
    param([Parameter(Mandatory = $true)][string]$RelativePath)

    $target = Resolve-RepoPath -RelativePath $RelativePath
    if (Test-IsExcludedRepoPath -FullPath $target) {
        Write-Host "Preservo: $RelativePath"
        return
    }

    Remove-PathSafe -Path $target -DisplayPath $RelativePath -Reason 'Remove generated repository item'
}

function Remove-GeneratedDirectoriesByName {
    param([Parameter(Mandatory = $true)][string[]]$Names)

    # Optimization: avoid searching inside .git and other large hidden directories
    $directories = Get-ChildItem -LiteralPath $script:Root -Directory -Force -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -ne '.git' } |
        ForEach-Object {
            Get-ChildItem -Path $_.FullName -Directory -Recurse -Force -ErrorAction SilentlyContinue
        } |
        Where-Object { $Names -contains $_.Name }

    # Also check root level directories
    $rootDirs = Get-ChildItem -LiteralPath $script:Root -Directory -Force -ErrorAction SilentlyContinue |
        Where-Object { $Names -contains $_.Name }

    $allDirs = @()
    if ($null -ne $directories) { $allDirs += $directories }
    if ($null -ne $rootDirs) { $allDirs += $rootDirs }

    $allDirs = $allDirs | Sort-Object { $_.FullName.Length } -Descending

    foreach ($directory in $allDirs) {
        if (Test-IsExcludedRepoPath -FullPath $directory.FullName) {
            Write-Host "Preservo: $(Get-RepoRelativePath -FullPath $directory.FullName)"
            continue
        }

        Remove-PathSafe `
            -Path $directory.FullName `
            -DisplayPath (Get-RepoRelativePath -FullPath $directory.FullName) `
            -Reason 'Remove generated repository directory'
    }
}

function Remove-RepoDirectoryContents {
    param([Parameter(Mandatory = $true)][string]$RelativePath)

    $directory = Resolve-RepoPath -RelativePath $RelativePath
    if (-not (Test-Path -LiteralPath $directory -PathType Container)) {
        Write-Host "Skip: $RelativePath"
        return
    }

    foreach ($child in Get-ChildItem -LiteralPath $directory -Force -ErrorAction SilentlyContinue) {
        $relativePath = Get-RepoRelativePath -FullPath $child.FullName
        if (Test-IsExcludedRepoPath -FullPath $child.FullName) {
            Write-Host "Preservo: $relativePath"
            continue
        }

        Remove-PathSafe -Path $child.FullName -DisplayPath $relativePath -Reason 'Remove generated repository item'
    }
}

function Stop-GradleDaemon {
    $gradlew = Join-Path $script:Root 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $gradlew -PathType Leaf)) {
        Write-Host 'Skip: gradlew.bat non trovato'
        return
    }

    if ($DryRun) {
        Write-Host 'DryRun: eseguirei .\gradlew.bat --stop' -ForegroundColor Yellow
        return
    }

    & $gradlew --stop
    if ($LASTEXITCODE -ne 0) {
        $message = 'gradlew.bat --stop fallito.'
        $script:Failures.Add($message)
        Write-Warning $message
    }
}

function Remove-AndroidStudioSystemCaches {
    if (-not $env:LOCALAPPDATA) {
        throw 'LOCALAPPDATA non definito: impossibile individuare le cache Android Studio.'
    }

    $studioProcesses = Get-Process -Name 'studio64', 'studio' -ErrorAction SilentlyContinue
    if ($studioProcesses) {
        $names = ($studioProcesses | Select-Object -ExpandProperty ProcessName -Unique) -join ', '
        $message = "Android Studio sembra aperto ($names). Chiudilo e rilancia lo script per evitare file bloccati."
        if ($FailOnError) { throw $message }
        Write-Warning $message
        return
    }

    $googleLocal = Join-Path $env:LOCALAPPDATA 'Google'
    if (-not (Test-Path -LiteralPath $googleLocal -PathType Container)) {
        Write-Host "Skip: $googleLocal"
        return
    }

    $targets = Get-ChildItem -LiteralPath $googleLocal -Directory -Filter 'AndroidStudio*' -ErrorAction SilentlyContinue
    if (-not $targets) {
        Write-Host 'Skip: nessuna directory AndroidStudio* in %LOCALAPPDATA%\Google'
        return
    }

    $cacheChildNames = @('caches', 'index', 'compile-server', 'jcef_cache')
    foreach ($studioDir in $targets) {
        foreach ($childName in $cacheChildNames) {
            $cachePath = Join-Path $studioDir.FullName $childName
            $display = "%LOCALAPPDATA%\Google\$($studioDir.Name)\$childName"
            Remove-PathSafe -Path $cachePath -DisplayPath $display -Reason 'Remove Android Studio system cache'
        }
    }
}

if ($PSBoundParameters.Count -eq 0 -and -not $NonInteractive) {
    Write-Host "`n--- Repo Cleaner Android/Gradle ---" -ForegroundColor Cyan
    Write-Host 'Pulisce gli output generati mantenendo i sorgenti e le configurazioni.'

    $DryRun = (Read-Host "`nEseguire solo anteprima (DryRun)? [Y/n]").ToLower() -ne 'n'
    $Deep = (Read-Host "Pulizia profonda cache locali (.gradle, .kotlin)? [y/N]").ToLower() -eq 'y'
    $ResetIdeProjectMetadata = (Read-Host "Reset metadati IDE locali (workspace, shelf)? [y/N]").ToLower() -eq 'y'
    $StopGradle = (Read-Host "Fermare i daemon Gradle? [y/N]").ToLower() -eq 'y'

    if (-not $DryRun) {
        $PurgeAndroidStudioCaches = (Read-Host "Pulire cache globali Android Studio? (Richiede IDE chiuso) [y/N]").ToLower() -eq 'y'

        Write-Host "`nATTENZIONE: Stai per avviare la pulizia REALE." -ForegroundColor Yellow
        if ((Read-Host "Confermi di voler procedere? [y/N]").ToLower() -ne 'y') {
            Write-Host "Operazione annullata." -ForegroundColor Red
            exit 0
        }
    }
}

Push-Location $script:Root
try {
    Write-Section 'Root repository'
    Write-Host $script:Root

    if ($StopGradle) {
        Write-Section 'Gradle daemon'
        Stop-GradleDaemon
    }

    Write-Section 'Directory generate standard'
    Remove-GeneratedDirectoriesByName -Names @('build', '.cxx', '.externalNativeBuild')

    foreach ($relativePath in @('captures', 'logs', 'tmp', 'demo\recordings', 'demo\screenshots', 'demo\videos')) {
        Remove-RepoItem -RelativePath $relativePath
    }

    if (Test-Path -LiteralPath (Resolve-RepoPath -RelativePath 'reports') -PathType Container) {
        Remove-RepoDirectoryContents -RelativePath 'reports'
    }

    if ($Deep) {
        Write-Section 'Cache locali Gradle/Kotlin nel repository'
        foreach ($relativePath in @('.gradle', '.kotlin')) {
            Remove-RepoItem -RelativePath $relativePath
        }
    }

    if ($ResetIdeProjectMetadata) {
        Write-Section 'Metadati IDE locali al progetto'
        foreach ($relativePath in @('.idea\caches', '.idea\workspace.xml', '.idea\shelf')) {
            Remove-RepoItem -RelativePath $relativePath
        }
    }

    if ($PurgeAndroidStudioCaches) {
        Write-Section 'Cache di sistema Android Studio'
        Remove-AndroidStudioSystemCaches
    }

    Write-Section 'Completato'
    if ($script:Failures.Count -gt 0) {
        Write-Warning "Pulizia completata con $($script:Failures.Count) elemento/i non rimossi. Chiudi Android Studio, terminali e processi Gradle, poi riprova."
        if ($FailOnError) {
            throw ($script:Failures -join [Environment]::NewLine)
        }
    }
} finally {
    Pop-Location
}
