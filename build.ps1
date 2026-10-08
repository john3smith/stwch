param([switch]$SkipTests)
$ErrorActionPreference='Stop'
if (-not $env:JAVA_HOME) { $env:JAVA_HOME='C:\ytdownforand\.tools\jdk-17.0.20.1+1' }
Push-Location $PSScriptRoot
try {
    if ($SkipTests) { & .\gradlew.bat assembleDebug --console=plain }
    else { & .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain }
    if ($LASTEXITCODE -ne 0) { throw 'Android build failed' }
    $version=[regex]::Match((Get-Content .\app\build.gradle -Raw),"versionName '([^']+)'").Groups[1].Value
    if (-not $version) { throw 'Version unavailable' }
    New-Item -ItemType Directory -Path releases -Force | Out-Null
    $source='.\app\build\outputs\apk\debug\app-debug.apk'
    $target=Join-Path 'releases' "stwch-v$version-debug.apk"
    if (Test-Path -LiteralPath $target) {
        if ((Get-FileHash -LiteralPath $source).Hash -ne (Get-FileHash -LiteralPath $target).Hash) {
            throw 'Existing versioned APK differs; increment version before publishing'
        }
    } else { Copy-Item -LiteralPath $source -Destination $target }
    Get-Item -LiteralPath $target | Select-Object Name,Length | Format-List
    Get-FileHash -LiteralPath $target -Algorithm SHA256 | Format-List Algorithm,Hash
} finally { Pop-Location }
