# Creates (or updates) the Windows scheduled task that runs run-hourly.ps1 every hour.
#
#   .\setup-task.ps1                  create/update, first run 2 minutes from now, then hourly
#   .\setup-task.ps1 -StartAt 09:00   first run at 9 AM, then hourly
#   .\setup-task.ps1 -Status          show when it last ran, the result, and the next run
#   .\setup-task.ps1 -RunNow          run it immediately
#   .\setup-task.ps1 -Remove          delete the task

param(
    [datetime] $StartAt = (Get-Date).AddMinutes(2),
    [int]      $IntervalHours = 1,
    [switch]   $Status,
    [switch]   $RunNow,
    [switch]   $Remove
)

$ErrorActionPreference = 'Stop'
$taskName = 'USBA Connect hourly test'
$scriptPath = Join-Path $PSScriptRoot 'run-hourly.ps1'

function Show-Status {
    $task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
    if (-not $task) {
        Write-Host "Task '$taskName' does not exist. Run .\setup-task.ps1 to create it." -ForegroundColor Yellow
        return
    }
    $info = $task | Get-ScheduledTaskInfo
    Write-Host ''
    Write-Host "Task       : $taskName"
    Write-Host "State      : $($task.State)"
    Write-Host "Last run   : $($info.LastRunTime)"
    Write-Host "Last result: $($info.LastTaskResult)  (0 = passed, 1 = test failed, 267011 = has not run yet)"
    Write-Host "Next run   : $($info.NextRunTime)"

    $summary = Join-Path $PSScriptRoot 'logs\summary.log'
    if (Test-Path $summary) {
        Write-Host ''
        Write-Host 'Last 5 runs:'
        Get-Content $summary -Tail 5 | ForEach-Object { Write-Host "  $_" }
    } else {
        Write-Host ''
        Write-Host 'No logs\summary.log yet, so the script has not completed a run.' -ForegroundColor Yellow
    }
}

if ($Status) { Show-Status; return }

if ($Remove) {
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
    Write-Host "Removed task '$taskName'." -ForegroundColor Green
    return
}

if ($RunNow) {
    Start-ScheduledTask -TaskName $taskName
    Write-Host "Started '$taskName'. It takes about a minute; then check .\setup-task.ps1 -Status" -ForegroundColor Green
    return
}

if (-not (Test-Path $scriptPath)) {
    throw "run-hourly.ps1 was not found next to this script ($PSScriptRoot). Run setup-task.ps1 from the project folder."
}

$action = New-ScheduledTaskAction -Execute 'powershell.exe' `
    -Argument ('-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "{0}"' -f $scriptPath) `
    -WorkingDirectory $PSScriptRoot

$trigger = New-ScheduledTaskTrigger -Once -At $StartAt -RepetitionInterval (New-TimeSpan -Hours $IntervalHours)

$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries -ExecutionTimeLimit (New-TimeSpan -Minutes 30) -MultipleInstances IgnoreNew

Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings `
    -Description 'Runs the USBA Connect send-message automation test every hour.' -Force | Out-Null

Write-Host "Task '$taskName' created. First run: $($StartAt.ToString('yyyy-MM-dd HH:mm')), then every $IntervalHours hour(s)." -ForegroundColor Green
Show-Status
