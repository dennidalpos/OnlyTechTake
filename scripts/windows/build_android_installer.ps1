<#
.SYNOPSIS
  Compatibilita': delega a scripts\android\build_installer.ps1.
#>

$ErrorActionPreference = "Stop"

$script = Join-Path $PSScriptRoot "..\android\build_installer.ps1"
& $script @args
exit $LASTEXITCODE
