function Test-JavaHome {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $javac = Join-Path $Path "bin\javac.exe"
    return (Test-Path $javac)
}

function Get-JavaMajorVersion {
    param(
        [Parameter(Mandatory = $true)]
        [string]$JavaHome
    )

    $java = Join-Path $JavaHome "bin\java.exe"
    if (-not (Test-Path $java)) {
        return $null
    }

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $output = & $java -version 2>&1
    $ErrorActionPreference = $previousErrorActionPreference
    $versionLine = ($output | Select-Object -First 1).ToString()

    if ($versionLine -match '"1\.(\d+)\.') {
        return [int]$Matches[1]
    }

    if ($versionLine -match '"(\d+)\.') {
        return [int]$Matches[1]
    }

    return $null
}

function Get-CandidateJavaHomes {
    $candidates = @()

    if ($env:ProgramFiles) {
        $candidates += (Join-Path $env:ProgramFiles "Android\Android Studio\jbr")
        $candidates += (Join-Path $env:ProgramFiles "Android\Android Studio\jre")
    }

    if ($env:JAVA_HOME) {
        $candidates += $env:JAVA_HOME
    }

    $javaRoots = @()
    if ($env:ProgramFiles) {
        $javaRoots += (Join-Path $env:ProgramFiles "Java")
    }
    if (${env:ProgramFiles(x86)}) {
        $javaRoots += (Join-Path ${env:ProgramFiles(x86)} "Java")
    }

    foreach ($root in $javaRoots) {
        if (Test-Path $root) {
            $candidates += Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
                Select-Object -ExpandProperty FullName
        }
    }

    return $candidates |
        Where-Object { $_ -and (Test-Path $_) -and (Test-JavaHome $_) } |
        Select-Object -Unique
}

function Resolve-AndroidJdk {
    $supportedMajorVersions = @(17, 21)
    $validJdks = foreach ($candidate in Get-CandidateJavaHomes) {
        $major = Get-JavaMajorVersion -JavaHome $candidate
        if ($null -ne $major -and $supportedMajorVersions -contains $major) {
            [PSCustomObject]@{
                Path = $candidate
                MajorVersion = $major
                IsAndroidStudioJbr = $candidate -like "*Android Studio\jbr*"
            }
        }
    }

    return $validJdks |
        Sort-Object `
            @{ Expression = { if ($_.IsAndroidStudioJbr) { 0 } else { 1 } } }, `
            @{ Expression = { if ($_.MajorVersion -eq 17) { 0 } else { 1 } } }, `
            Path |
        Select-Object -First 1
}

function Test-AndroidSdk {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $platforms = Join-Path $Path "platforms"
    $buildTools = Join-Path $Path "build-tools"
    return ((Test-Path $platforms) -and (Test-Path $buildTools))
}

function Test-AndroidSdkPlatform {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [int]$ApiLevel
    )

    $platformsPath = Join-Path $Path "platforms"
    if (Test-Path (Join-Path $platformsPath "android-$ApiLevel")) {
        return $true
    }

    return $null -ne (
        Get-ChildItem -Path $platformsPath -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -like "android-$ApiLevel.*" } |
            Select-Object -First 1
    )
}

function Test-AndroidBuildToolsInstalled {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $buildTools = Join-Path $Path "build-tools"
    return (Test-Path $buildTools) -and
        ($null -ne (Get-ChildItem -Path $buildTools -Directory -ErrorAction SilentlyContinue | Select-Object -First 1))
}

function Test-AndroidPlatformToolsInstalled {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    return Test-Path (Join-Path $Path "platform-tools\adb.exe")
}

function Resolve-AndroidSdk {
    $candidates = @()

    if ($env:ANDROID_HOME) {
        $candidates += $env:ANDROID_HOME
    }

    if ($env:ANDROID_SDK_ROOT) {
        $candidates += $env:ANDROID_SDK_ROOT
    }

    if ($env:LOCALAPPDATA) {
        $candidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk")
    }

    if ($env:ProgramFiles) {
        $candidates += (Join-Path $env:ProgramFiles "Android\Sdk")
    }

    return $candidates |
        Where-Object { $_ -and (Test-Path $_) -and (Test-AndroidSdk $_) } |
        Select-Object -Unique -First 1
}

function Add-ToProcessPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path $Path)) {
        return
    }

    $pathParts = $env:Path -split ';'
    if ($pathParts -notcontains $Path) {
        $env:Path = "$Path;$env:Path"
    }
}

function Set-AndroidCliEnvironment {
    $jdk = Resolve-AndroidJdk
    if ($null -ne $jdk) {
        $env:JAVA_HOME = $jdk.Path
        Add-ToProcessPath (Join-Path $jdk.Path "bin")
    }

    $sdk = Resolve-AndroidSdk
    if ($null -ne $sdk) {
        $env:ANDROID_HOME = $sdk
        $env:ANDROID_SDK_ROOT = $sdk
        Add-ToProcessPath (Join-Path $sdk "platform-tools")
    }

    return [PSCustomObject]@{
        JavaHome = if ($null -ne $jdk) { $jdk.Path } else { $null }
        JavaMajorVersion = if ($null -ne $jdk) { $jdk.MajorVersion } else { $null }
        AndroidSdk = $sdk
        RequiredCompileSdk = 37
    }
}

function Get-AndroidSigningProperties {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $properties = @{}
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $properties[$parts[0].Trim()] = $parts[1].Trim()
        }
    }

    return $properties
}

function Get-RequiredAndroidSigningProperty {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$Properties,

        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    if (-not $Properties.ContainsKey($Name) -or [string]::IsNullOrWhiteSpace($Properties[$Name])) {
        throw "Configurazione release non valida: proprieta' '$Name' mancante in keystore.properties."
    }

    return $Properties[$Name]
}

function Assert-AndroidReleaseSigningConfigured {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RootPath
    )

    $keystorePropertiesPath = Join-Path $RootPath "keystore.properties"
    if (-not (Test-Path $keystorePropertiesPath)) {
        throw "Build release bloccata: keystore.properties non trovato nella root. Configurare signing locale non tracciato prima di generare APK/AAB release."
    }

    $signingProperties = Get-AndroidSigningProperties -Path $keystorePropertiesPath
    $storeFile = Get-RequiredAndroidSigningProperty -Properties $signingProperties -Name "storeFile"
    Get-RequiredAndroidSigningProperty -Properties $signingProperties -Name "storePassword" | Out-Null
    Get-RequiredAndroidSigningProperty -Properties $signingProperties -Name "keyAlias" | Out-Null
    Get-RequiredAndroidSigningProperty -Properties $signingProperties -Name "keyPassword" | Out-Null

    $resolvedStoreFile = if ([System.IO.Path]::IsPathRooted($storeFile)) {
        $storeFile
    } else {
        Join-Path $RootPath $storeFile
    }

    if (-not (Test-Path $resolvedStoreFile)) {
        throw "Build release bloccata: il keystore indicato in keystore.properties non esiste."
    }
}
