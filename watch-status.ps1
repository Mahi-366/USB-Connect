# Small always-on-top window showing the result of the hourly test.
# Open it once (for example after your morning run) and leave it on screen:
#
#   .\watch-status.ps1
#
# It refreshes by itself every minute, so you never need to type a status command.

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$taskName = 'USBA Connect hourly test'
$summaryFile = Join-Path $PSScriptRoot 'logs\summary.log'

$form = New-Object System.Windows.Forms.Form
$form.Text = 'USBA Connect - hourly test'
$form.Size = New-Object System.Drawing.Size(620, 420)
$form.TopMost = $true
$form.Icon = [System.Drawing.SystemIcons]::Information

$header = New-Object System.Windows.Forms.Label
$header.Dock = 'Top'
$header.Height = 90
$header.TextAlign = 'MiddleCenter'
$header.Font = New-Object System.Drawing.Font('Segoe UI', 13, [System.Drawing.FontStyle]::Bold)
$header.ForeColor = [System.Drawing.Color]::White

$body = New-Object System.Windows.Forms.TextBox
$body.Multiline = $true
$body.ReadOnly = $true
$body.Dock = 'Fill'
$body.ScrollBars = 'Vertical'
$body.Font = New-Object System.Drawing.Font('Consolas', 9)
$body.BackColor = [System.Drawing.Color]::White

# Add the filling control first so the header keeps its place at the top
$form.Controls.Add($body)
$form.Controls.Add($header)

function Update-View {
    $lines = @()
    if (Test-Path $summaryFile) {
        $lines = Get-Content $summaryFile -Tail 15
    }
    $last = if ($lines.Count -gt 0) { $lines[-1] } else { '' }

    if ($last -match '\sPASS\s') {
        $header.BackColor = [System.Drawing.Color]::FromArgb(22, 133, 62)
        $state = 'WORKING'
    } elseif ($last -match '\sFAIL\s') {
        $header.BackColor = [System.Drawing.Color]::FromArgb(176, 35, 35)
        $state = 'FAILING'
    } else {
        $header.BackColor = [System.Drawing.Color]::FromArgb(90, 90, 90)
        $state = 'NO RUNS YET'
    }

    $nextRun = 'unknown'
    $taskState = 'not created'
    $task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
    if ($task) {
        $taskState = $task.State
        $info = $task | Get-ScheduledTaskInfo
        if ($info.NextRunTime) { $nextRun = $info.NextRunTime.ToString('HH:mm') }
    }

    $header.Text = "$state`n$last"
    $body.Text = ("Task: $taskState     Next run: $nextRun     Updated: " +
                  (Get-Date -Format 'HH:mm:ss') + "`r`n`r`nRecent runs (newest last):`r`n`r`n" +
                  ($lines -join "`r`n"))
}

$timer = New-Object System.Windows.Forms.Timer
$timer.Interval = 60000
$timer.Add_Tick({ Update-View })

$form.Add_Shown({ Update-View; $timer.Start() })
[void]$form.ShowDialog()
