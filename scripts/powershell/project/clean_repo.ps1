<#
.SYNOPSIS
  Rimuove output temporanei e cache generate dentro il repository.

.USAGE
  Anteprima senza cancellare:
    .\scripts\powershell\project\clean_repo.ps1 -DryRun

  Pulizia standard:
    .\scripts\powershell\project\clean_repo.ps1

  Pulizia includendo cache IDE locali:
    .\scripts\powershell\project\clean_repo.ps1 -IncludeIde

.DESCRIPTION
  Lo script rileva la root del progetto dal proprio percorso e rimuove solo
  directory/file generati nel working tree. Non tocca cache globali Gradle,
  Android SDK, keystore, local.properties o altri file di configurazione locale.
#>

[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [switch]$DryRun,
    [switch]$IncludeIde,
    [switch]$StopGradle
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$excludedRelativePaths = @(
    "reports\demo-assets"
)

function Write-Section($Text) {
    Write-Host ""
    Write-Host "== $Text ==" -ForegroundColor Cyan
}

function Resolve-RepoPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RelativePath
    )

    $candidate = Join-Path $root $RelativePath
    $fullPath = [System.IO.Path]::GetFullPath($candidate)
    $rootWithSeparator = $root.TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar

    if ($fullPath -ne $root -and -not $fullPath.StartsWith($rootWithSeparator, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Percorso fuori dalla root del repository: $RelativePath"
    }

    $fullPath
}

function Get-RepoRelativePath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FullPath
    )

    $rootWithSeparator = $root.TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
    if ($FullPath.StartsWith($rootWithSeparator, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $FullPath.Substring($rootWithSeparator.Length)
    }

    if ($FullPath -eq $root) {
        return "."
    }

    throw "Percorso fuori dalla root del repository: $FullPath"
}

function Test-IsExcludedRepoPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FullPath
    )

    $normalizedPath = [System.IO.Path]::GetFullPath($FullPath).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)

    foreach ($relativePath in $excludedRelativePaths) {
        $excludedPath = (Resolve-RepoPath -RelativePath $relativePath).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
        $excludedPathWithSeparator = $excludedPath + [System.IO.Path]::DirectorySeparatorChar

        if ($normalizedPath -eq $excludedPath -or $normalizedPath.StartsWith($excludedPathWithSeparator, [System.StringComparison]::OrdinalIgnoreCase)) {
            return $true
        }
    }

    $false
}

function Remove-RepoItem {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RelativePath
    )

    $target = Resolve-RepoPath -RelativePath $RelativePath
    if (Test-IsExcludedRepoPath -FullPath $target) {
        Write-Host "Preservo: $RelativePath"
        return
    }

    if (-not (Test-Path -LiteralPath $target)) {
        Write-Host "Skip: $RelativePath"
        return
    }

    if ($DryRun) {
        Write-Host "DryRun: rimuoverei $RelativePath" -ForegroundColor Yellow
        return
    }

    if ($PSCmdlet.ShouldProcess($target, "Remove generated repository item")) {
        Remove-Item -LiteralPath $target -Recurse -Force
        Write-Host "Rimosso: $RelativePath" -ForegroundColor Green
    }
}

function Remove-RepoDirectoryContents {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RelativePath
    )

    $directory = Resolve-RepoPath -RelativePath $RelativePath
    if (-not (Test-Path -LiteralPath $directory -PathType Container)) {
        Write-Host "Skip: $RelativePath"
        return
    }

    Get-ChildItem -LiteralPath $directory -Force | ForEach-Object {
        $relativePath = Get-RepoRelativePath -FullPath $_.FullName
        if (Test-IsExcludedRepoPath -FullPath $_.FullName) {
            Write-Host "Preservo: $relativePath"
            return
        }

        if ($DryRun) {
            Write-Host "DryRun: rimuoverei $relativePath" -ForegroundColor Yellow
            return
        }

        if ($PSCmdlet.ShouldProcess($_.FullName, "Remove generated repository item")) {
            Remove-Item -LiteralPath $_.FullName -Recurse -Force
            Write-Host "Rimosso: $relativePath" -ForegroundColor Green
        }
    }
}

function Remove-RepoPattern {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RelativeDirectory,

        [Parameter(Mandatory = $true)]
        [string]$Filter
    )

    $directory = Resolve-RepoPath -RelativePath $RelativeDirectory
    if (-not (Test-Path -LiteralPath $directory -PathType Container)) {
        return
    }

    Get-ChildItem -LiteralPath $directory -Filter $Filter -File -Recurse -Force | ForEach-Object {
        $relativePath = Get-RepoRelativePath -FullPath $_.FullName
        if (Test-IsExcludedRepoPath -FullPath $_.FullName) {
            Write-Host "Preservo: $relativePath"
            return
        }

        if ($DryRun) {
            Write-Host "DryRun: rimuoverei $relativePath" -ForegroundColor Yellow
            return
        }

        if ($PSCmdlet.ShouldProcess($_.FullName, "Remove generated repository file")) {
            Remove-Item -LiteralPath $_.FullName -Force
            Write-Host "Rimosso: $relativePath" -ForegroundColor Green
        }
    }
}

Push-Location $root
try {
    Write-Section "Root repository"
    Write-Host $root

    if ($StopGradle) {
        Write-Section "Gradle daemon"
        $gradlew = Join-Path $root "gradlew.bat"
        if (Test-Path -LiteralPath $gradlew) {
            if ($DryRun) {
                Write-Host "DryRun: eseguirei .\gradlew.bat --stop" -ForegroundColor Yellow
            } else {
                & $gradlew --stop
                if ($LASTEXITCODE -ne 0) {
                    throw "gradlew.bat --stop fallito."
                }
            }
        } else {
            Write-Host "Skip: gradlew.bat non trovato"
        }
    }

    Write-Section "Directory generate"
    $paths = @(
        ".gradle",
        ".kotlin",
        "build",
        "app\build",
        ".cxx",
        "app\.cxx",
        ".externalNativeBuild",
        "app\.externalNativeBuild",
        "captures",
        "logs",
        "tmp",
        "demo\recordings",
        "demo\screenshots",
        "demo\videos"
    )

    if ($IncludeIde) {
        $paths += @(".idea")
    }

    foreach ($path in $paths) {
        Remove-RepoItem -RelativePath $path
    }

    Remove-RepoDirectoryContents -RelativePath "reports"

    Write-Section "Artifact Android generati"
    foreach ($pattern in @("*.apk", "*.aab", "*.ap_")) {
        Remove-RepoPattern -RelativeDirectory "." -Filter $pattern
    }

    Write-Section "Completato"
} finally {
    Pop-Location
}
