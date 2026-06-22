<#
.SYNOPSIS
  Avvia solo il flusso di registrazione video per la campagna social.
#>
param([string]$AvdName = "Pixel_7", [switch]$SkipBuild)
$PSScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$script = Join-Path $PSScriptRoot "generate_social_campaign.ps1"
$params = @{
    OnlyVideo = $true
    AvdName = $AvdName
    SkipBuild = $SkipBuild
}
& $script @params
