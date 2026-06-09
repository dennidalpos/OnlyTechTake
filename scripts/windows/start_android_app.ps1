<#
.SYNOPSIS
  Compatibilita': delega a scripts\android\start_app.ps1.
#>

$ErrorActionPreference = "Stop"

$script = Join-Path $PSScriptRoot "..\android\start_app.ps1"
& $script @args
exit $LASTEXITCODE
