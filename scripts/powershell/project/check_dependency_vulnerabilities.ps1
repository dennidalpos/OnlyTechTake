<#
.SYNOPSIS
  Controlla le dipendenze Maven risolte nei lockfile Gradle contro OSV.

.USAGE
  .\scripts\powershell\project\check_dependency_vulnerabilities.ps1

.DESCRIPTION
  Lo script legge i lockfile Gradle del progetto, interroga OSV API per
  coordinate Maven group:artifact:version e fallisce se trova vulnerabilita'.
  Richiede accesso internet, quindi resta separato dai check offline locali.
#>

param(
    [string]$OsvApiUrl = "https://api.osv.dev/v1/querybatch",
    [switch]$NonInteractive
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path

# Interactive Mode
if ($PSBoundParameters.Count -eq 0 -and -not $NonInteractive) {
    Write-Host "`n--- OnlyTechTake Dependency Vulnerability Check ---" -ForegroundColor Cyan
    Write-Host "Verifica la presenza di vulnerabilità note (OSV) nelle dipendenze del progetto."

    $confirm = Read-Host "`nAvviare la scansione (richiede internet)? [Y/n]"
    if ($confirm -eq 'n') { exit 0 }
}

function Write-Section($Text) {
    Write-Host ""
    Write-Host "== $Text ==" -ForegroundColor Cyan
}

function Read-GradleLockedDependency {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Lockfile
    )

    Get-Content $Lockfile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or $line.StartsWith("empty=")) {
            return
        }

        $coordinate = ($line -split "=", 2)[0]
        $parts = $coordinate -split ":"
        if ($parts.Count -lt 3) {
            return
        }

        [PSCustomObject]@{
            Name = "$($parts[0]):$($parts[1])"
            Version = $parts[2]
            Coordinate = $coordinate
        }
    }
}

function Split-Batches {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Items,

        [int]$Size = 100
    )

    for ($index = 0; $index -lt $Items.Count; $index += $Size) {
        $end = [Math]::Min($index + $Size - 1, $Items.Count - 1)
        ,$Items[$index..$end]
    }
}

$lockfiles = @(
    Join-Path $root "settings-gradle.lockfile"
    Join-Path $root "gradle.lockfile"
    Join-Path $root "app\gradle.lockfile"
) | Where-Object { Test-Path $_ }

if ($lockfiles.Count -eq 0) {
    throw "Nessun lockfile Gradle trovato. Eseguire prima .\gradlew.bat --write-locks ..."
}

Write-Section "Lettura lockfile"
$dependencies = $lockfiles |
    ForEach-Object { Read-GradleLockedDependency -Lockfile $_ } |
    Group-Object Coordinate |
    ForEach-Object { $_.Group | Select-Object -First 1 } |
    Sort-Object Coordinate

if ($dependencies.Count -eq 0) {
    throw "Nessuna dipendenza Maven trovata nei lockfile Gradle."
}

Write-Host "Dipendenze Maven uniche: $($dependencies.Count)"

$findings = New-Object System.Collections.Generic.List[object]

Write-Section "Query OSV"
foreach ($batch in Split-Batches -Items $dependencies -Size 100) {
    $queries = @(
        foreach ($dependency in $batch) {
            @{
                package = @{
                    ecosystem = "Maven"
                    name = $dependency.Name
                }
                version = $dependency.Version
            }
        }
    )

    $body = @{ queries = $queries } | ConvertTo-Json -Depth 8
    $response = Invoke-RestMethod -Method Post -Uri $OsvApiUrl -ContentType "application/json" -Body $body

    for ($i = 0; $i -lt $batch.Count; $i++) {
        $result = $response.results[$i]
        if ($null -eq $result -or $null -eq $result.vulns) {
            continue
        }

        foreach ($vulnerability in $result.vulns) {
            $findings.Add([PSCustomObject]@{
                Coordinate = $batch[$i].Coordinate
                Id = $vulnerability.id
                Summary = $vulnerability.summary
            }) | Out-Null
        }
    }
}

if ($findings.Count -gt 0) {
    Write-Host "Vulnerabilita' trovate:" -ForegroundColor Red
    $findings | Format-Table -AutoSize
    exit 2
}

Write-Host "Nessuna vulnerabilita' OSV trovata per le dipendenze bloccate." -ForegroundColor Green
