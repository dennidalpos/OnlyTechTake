<#
.SYNOPSIS
  Compatibilita': delega a scripts\android\check_env.ps1.
#>

$ErrorActionPreference = "Stop"

$script = Join-Path $PSScriptRoot "..\android\check_env.ps1"
& $script @args
exit $LASTEXITCODE
