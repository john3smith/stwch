param([string]$BatPath = (Join-Path $PSScriptRoot 'stwch-nowbar-adb-enable.bat'))
$ErrorActionPreference = 'Stop'
$resolved = (Resolve-Path -LiteralPath $BatPath).Path
$content = [IO.File]::ReadAllText($resolved)
$code = ($content -split '(?m)^# STWCH_POWERSHELL_BEGIN\r?$', 2)[1]
$parseErrors = $null
$tokens = $null
[void][Management.Automation.Language.Parser]::ParseInput($code, [ref]$tokens, [ref]$parseErrors)
if ($parseErrors.Count) { throw 'Embedded PowerShell parse failed' }
. ([scriptblock]::Create(($code -split '# STWCH_ENTRY_BEGIN', 2)[0]))
$script:passed = 1

function Assert-Equal($Actual, $Expected, [string]$Label) {
    if ($Actual -ne $Expected) { throw ('FAIL: ' + $Label) }
    $script:passed++
}
function Assert-Throws([scriptblock]$Action, [string]$Label) {
    $failed = $false
    try { & $Action | Out-Null } catch { $failed = $true }
    Assert-Equal $failed $true $Label
}

Assert-Equal (Select-Phone "List of devices attached`nphone123 device model:Galaxy`nemulator-5554 device`nlocalhost:46217 device") 'phone123' 'USB phone selection'
Assert-Equal (Select-Phone "List of devices attached`n192.168.0.20:37125 device model:Galaxy") '192.168.0.20:37125' 'Wireless phone selection'
Assert-Throws { Select-Phone "List of devices attached`nemulator-5554 device" } 'Reject emulator-only'
Assert-Throws { Select-Phone "List of devices attached`na device`nb device" } 'Reject two phones'
Assert-Throws { Select-Phone "List of devices attached`na unauthorized" } 'Reject unauthorized'
Assert-Throws { Select-Phone "List of devices attached`na device`nb unauthorized" } 'Reject ambiguous unauthorized phone'
Assert-Throws { Select-Phone "List of devices attached`na offline" } 'Reject offline phone'
Assert-Throws { Select-Phone "List of devices attached`na device`nb recovery" } 'Reject recovery-mode second phone'

$env:STWCH_HELPER_SELF = $resolved
$env:STWCH_ADB = $resolved
Assert-Equal (Find-Adb) $resolved 'Explicit literal ADB path'

function Read-Host([string]$Prompt) { return $script:answer }
$script:answer = 'n'
Assert-Throws { Download-Adb } 'Download cancelled before filesystem or network changes'

$script:calls = New-Object 'System.Collections.Generic.List[string]'
$script:original = 'null'
$script:applied = '1'
$script:putFails = $false
function Invoke-Adb([string]$AdbPath, [string[]]$Arguments) {
    $record = $Arguments -join ' '
    $script:calls.Add($record)
    if ($record -match 'settings put') {
        if ($script:putFails) { throw 'Mock ADB failure' }
        return ''
    }
    if ($script:calls.Count -eq 1) { return $script:original }
    return $script:applied
}

$script:answer = 'y'
Enable-NowBar 'mock-adb' 'phone123'
Assert-Equal $script:calls.Count 3 'Get, put, verify sequence'
Assert-Equal $script:calls[1] '-s phone123 shell settings put secure enable_notification_nowbar_test 1' 'Write scoped to selected serial'
Assert-Equal $script:calls[2] '-s phone123 shell settings get secure enable_notification_nowbar_test' 'Readback verifies same serial'

$script:calls.Clear()
$script:answer = 'n'
Enable-NowBar 'mock-adb' 'phone123'
Assert-Equal $script:calls.Count 1 'Activation cancelled without write'

$script:calls.Clear()
$script:answer = 'y'
$script:original = 'unexpected output'
Assert-Throws { Enable-NowBar 'mock-adb' 'phone123' } 'Invalid original value prevents writes'
Assert-Equal $script:calls.Count 1 'No write after invalid original value'

$script:calls.Clear()
$script:original = 'null'
$script:putFails = $true
Assert-Throws { Enable-NowBar 'mock-adb' 'phone123' } 'ADB write failure is reported'
Assert-Equal $script:calls.Count 2 'No success readback after failed write'

$script:calls.Clear()
$script:putFails = $false
$script:applied = '0'
Assert-Throws { Enable-NowBar 'mock-adb' 'phone123' } 'Readback mismatch is reported'

Write-Host ('PASSED: ' + $script:passed + ' checks. Phone responses mocked; no phone settings or network changed.')
