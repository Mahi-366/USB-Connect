# Runs the automation test once, saves a log, and shows a desktop notification.
# Windows Task Scheduler calls this file every hour. See README.md ("Run the test automatically every hour").

Set-Location -Path $PSScriptRoot
New-Item -ItemType Directory -Force -Path 'logs' | Out-Null

$startedAt = Get-Date
$log = Join-Path 'logs' ('run-' + $startedAt.ToString('yyyyMMdd-HHmm') + '.log')

# --- Find Java -------------------------------------------------------------
# Task Scheduler does not always see the same PATH as IntelliJ, so look for a JDK ourselves.
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $candidate = Get-ChildItem -Path (Join-Path $env:USERPROFILE '.jdks') -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($candidate) {
        $env:JAVA_HOME = $candidate.FullName
    }
}
if (-not $env:JAVA_HOME -and -not (Get-Command java -ErrorAction SilentlyContinue)) {
    'No Java found. Set JAVA_HOME to your JDK folder, then try again.' | Tee-Object -FilePath $log
    exit 1
}

# --- Run the test ----------------------------------------------------------
& .\mvnw.cmd -B test '-Dheadless=true' 2>&1 | Tee-Object -FilePath $log | Out-Null
$passed = ($LASTEXITCODE -eq 0)

# --- Build a one line summary ---------------------------------------------
$patterns = 'PASS - status:', 'Message status is Failed', 'did not appear in History',
            'No matching option', 'Missing setting', 'Send failed with HTTP', 'BUILD FAILURE'
$detail = (Select-String -Path $log -SimpleMatch -Pattern $patterns | Select-Object -First 1).Line
if ($detail) {
    $detail = $detail.Trim() -replace '^\[ERROR\]\s*', '' -replace '\s+', ' '
    if ($detail.Length -gt 180) { $detail = $detail.Substring(0, 180) + '...' }
} else {
    $detail = 'See ' + $log
}

$result = if ($passed) { 'PASS' } else { 'FAIL' }
$line = '{0}  {1}  {2}' -f $startedAt.ToString('yyyy-MM-dd HH:mm'), $result, $detail

Add-Content -Path 'logs\summary.log' -Value $line
Set-Content -Path 'logs\latest-status.txt' -Value $line
Write-Output $line

# Keep the last 14 days of detailed logs only
Get-ChildItem 'logs\run-*.log' | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-14) } |
    Remove-Item -ErrorAction SilentlyContinue

# --- Desktop notification --------------------------------------------------
try {
    Add-Type -AssemblyName System.Windows.Forms
    Add-Type -AssemblyName System.Drawing
    $notify = New-Object System.Windows.Forms.NotifyIcon
    $notify.Icon = [System.Drawing.SystemIcons]::Information
    $notify.Visible = $true
    $notify.BalloonTipTitle = if ($passed) { 'USBA Connect: message sent OK' } else { 'USBA Connect: TEST FAILED' }
    $notify.BalloonTipText = $detail
    $notify.BalloonTipIcon = if ($passed) {
        [System.Windows.Forms.ToolTipIcon]::Info
    } else {
        [System.Windows.Forms.ToolTipIcon]::Error
    }
    $notify.ShowBalloonTip(20000)
    Start-Sleep -Seconds 12
    $notify.Dispose()
} catch {
    # No desktop session (for example the task runs while logged off) - the log still has the result.
}

if ($passed) { exit 0 } else { exit 1 }
