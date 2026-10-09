@echo off
setlocal EnableExtensions DisableDelayedExpansion
set "STWCH_HELPER_SELF=%~f0"
set "STWCH_HELPER_MODE=%~1"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$body=[IO.File]::ReadAllText($env:STWCH_HELPER_SELF); $code=($body -split '(?m)^# STWCH_POWERSHELL_BEGIN\r?$',2)[1]; if(-not $code){exit 1}; & ([scriptblock]::Create($code))"
set "STWCH_HELPER_EXIT=%ERRORLEVEL%"
if /i not "%~1"=="--check" pause
exit /b %STWCH_HELPER_EXIT%
# STWCH_POWERSHELL_BEGIN
$ErrorActionPreference = 'Stop'

function Find-Adb {
    $folder = Split-Path -Parent $env:STWCH_HELPER_SELF
    $candidates = @(
        $env:STWCH_ADB,
        (Join-Path $folder 'platform-tools\adb.exe'),
        (Join-Path $folder 'adb.exe')
    )
    foreach ($sdk in @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME)) {
        if ($sdk) { $candidates += Join-Path $sdk 'platform-tools\adb.exe' }
    }
    if ($env:LOCALAPPDATA) {
        $candidates += Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
        $cache = Join-Path $env:LOCALAPPDATA 'stwch\adb-cache'
        if (Test-Path -LiteralPath $cache -PathType Container) {
            foreach ($dir in (Get-ChildItem -LiteralPath $cache -Directory | Sort-Object LastWriteTime -Descending)) {
                $candidates += Join-Path $dir.FullName 'platform-tools\adb.exe'
            }
        }
    }
    $command = Get-Command adb.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) { $candidates += $command.Source }
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate -PathType Leaf)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    return $null
}

function Invoke-Adb([string]$AdbPath, [string[]]$Arguments) {
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $AdbPath
    $info.Arguments = ($Arguments | ForEach-Object { '"' + $_.Replace('"', '\"') + '"' }) -join ' '
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardOutputEncoding = [Text.Encoding]::UTF8
    $info.StandardErrorEncoding = [Text.Encoding]::UTF8
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $info
    try {
        [void]$process.Start()
        $output = $process.StandardOutput.ReadToEndAsync()
        $errors = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit(15000)) {
            $process.Kill()
            throw 'ADB request timed out. Check USB debugging and the phone connection.'
        }
        $text = $output.GetAwaiter().GetResult().Trim()
        $errorText = $errors.GetAwaiter().GetResult().Trim()
        if ($process.ExitCode -ne 0) { throw ('ADB failed: ' + $errorText) }
        return $text
    } finally { $process.Dispose() }
}

function Download-Adb {
    Write-Host 'ADB was not found. No Android Studio or administrator account is required.'
    Write-Host 'Official download and license: https://developer.android.com/tools/releases/platform-tools'
    if ((Read-Host 'Accept the SDK license and download official Google Platform-Tools? [y/N]') -notmatch '^(?i)y(es)?$') {
        throw 'Cancelled. Install Platform-Tools or place its folder next to this BAT, then try again.'
    }
    if (-not $env:LOCALAPPDATA) { throw 'LOCALAPPDATA is unavailable.' }
    $destination = Join-Path $env:LOCALAPPDATA ('stwch\adb-cache\' + [Guid]::NewGuid().ToString('N'))
    [void](New-Item -ItemType Directory -Path $destination)
    $archive = Join-Path $destination 'platform-tools.zip'
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -UseBasicParsing -Uri 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip' -OutFile $archive -TimeoutSec 120
    Expand-Archive -LiteralPath $archive -DestinationPath $destination
    $adb = Join-Path $destination 'platform-tools\adb.exe'
    if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) { throw 'The download did not contain adb.exe.' }
    # Only remove the ZIP created by this invocation, never existing SDK files.
    Remove-Item -LiteralPath $archive
    return $adb
}

function Select-Phone([string]$Listing) {
    $phones = @()
    $notReady = @()
    foreach ($line in ($Listing -split '\r?\n')) {
        if ($line -match '^([^\s]+)\s+(device|offline|unauthorized|recovery|sideload|bootloader|connecting|no permissions)(?:\s|$)') {
            $serial = $Matches[1]
            $state = $Matches[2]
            if ($serial -notmatch '^(emulator-|localhost:|127\.0\.0\.1:)') {
                if ($state -eq 'device') { $phones += $serial }
                else { $notReady += $serial }
            }
        }
    }
    if ($phones.Count -ne 1 -or $notReady.Count -ne 0) {
        throw 'Connect exactly ONE ready physical phone. Authorize USB debugging on the phone. Emulators and localhost test devices are excluded.'
    }
    return $phones[0]
}

function Enable-NowBar([string]$AdbPath, [string]$Serial) {
    $original = Invoke-Adb $AdbPath @('-s', $Serial, 'shell', 'settings', 'get', 'secure', 'enable_notification_nowbar_test')
    if ($original -notmatch '^(null|-?\d+)$') { throw 'Could not read the original setting. No setting was changed.' }
    Write-Host ('Original value (record this for restoration): ' + $original)
    Write-Host 'Unofficial Samsung DEVICE-WIDE test setting, not a stwch-only permission.'
    if ((Read-Host 'Enable this setting on the displayed phone? [y/N]') -notmatch '^(?i)y(es)?$') {
        Write-Host 'Cancelled. No phone setting was changed.'
        return
    }
    [void](Invoke-Adb $AdbPath @('-s', $Serial, 'shell', 'settings', 'put', 'secure', 'enable_notification_nowbar_test', '1'))
    $applied = Invoke-Adb $AdbPath @('-s', $Serial, 'shell', 'settings', 'get', 'secure', 'enable_notification_nowbar_test')
    if ($applied -ne '1') { throw 'The setting was not verified as 1. Check the phone before retrying.' }
    Write-Host 'SUCCESS: setting verified as 1. Allow stwch notifications; pause/resume and check the lock screen.'
    Write-Host 'Now Bar display is firmware-dependent and is not guaranteed by this setting.'
    if ($original -eq 'null') {
        Write-Host ('Restore original setting: adb -s ' + $Serial + ' shell settings delete secure enable_notification_nowbar_test')
    } else {
        Write-Host ('Restore original setting: adb -s ' + $Serial + ' shell settings put secure enable_notification_nowbar_test ' + $original)
    }
}

# STWCH_ENTRY_BEGIN
try {
    Write-Host 'stwch Now Bar ADB helper v2 - Windows 10/11'
    $adb = Find-Adb
    if (-not $adb) {
        if ($env:STWCH_HELPER_MODE -eq '--check') { throw 'ADB not found. Check mode never downloads tools.' }
        $adb = Download-Adb
    }
    Write-Host ('ADB: ' + $adb)
    Write-Host (Invoke-Adb $adb @('version'))
    if ($env:STWCH_HELPER_MODE -eq '--check') {
        Write-Host 'CHECK PASSED. No download or phone setting change was performed.'
        exit 0
    }
    $listing = Invoke-Adb $adb @('devices', '-l')
    Write-Host $listing
    $serial = Select-Phone $listing
    Write-Host ('Selected phone: ' + $serial)
    Enable-NowBar $adb $serial
    exit 0
} catch {
    Write-Host ('ERROR: ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
}
