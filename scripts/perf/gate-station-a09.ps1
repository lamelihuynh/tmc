[CmdletBinding()]
param(
  [ValidateRange(1, 100000)]
  [int]$Count = 800,
  [ValidateRange(0, 100)]
  [double]$TargetRps = 0.45,
  [ValidateRange(0, 100)]
  [int]$WarmupPerEmployee = 2,
  [string]$EdgeUrl = 'http://localhost:8081',
  [string]$UserQrUrl = 'http://localhost:8083',
  [string]$GateId = 'BD-GATE-03',
  [string]$KioskId = 'A09-LOAD-TEST',
  [string]$OutputPath
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$deviceSim = Join-Path $root 'simulators\device-simulator\build\install\device-sim\bin\device-sim.bat'
if (-not (Test-Path $deviceSim)) {
  throw 'device-sim is missing. Run .\gradlew.bat :simulators:device-simulator:installDist first.'
}
if (-not $OutputPath) {
  $OutputPath = Join-Path $root 'docs\report\A-09-load-test.md'
}

$employees = @(
  [pscustomobject]@{ Name = 'a09-employee-01'; Uuid = '0192a1c0-0000-7000-8000-000000000001' },
  [pscustomobject]@{ Name = 'a09-employee-02'; Uuid = '0192a1c0-0000-7000-8000-000000000002' },
  [pscustomobject]@{ Name = 'a09-employee-04'; Uuid = '0192a1c0-0000-7000-8000-000000000004' },
  [pscustomobject]@{ Name = 'a09-employee-05'; Uuid = '0192a1c0-0000-7000-8000-000000000005' }
)
$runId = [guid]::NewGuid().ToString('N')
$profileHome = Join-Path ([System.IO.Path]::GetTempPath()) "tmc-a09-$runId"
New-Item -ItemType Directory -Force $profileHome | Out-Null

function Invoke-DeviceSim([string[]]$Arguments) {
  $output = & $deviceSim @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw "device-sim $($Arguments[0]) failed with exit code $LASTEXITCODE"
  }
  return $output
}

function New-Token($Employee) {
  $output = Invoke-DeviceSim @(
    'token', '--name', $Employee.Name, '--home', $profileHome, '--factories', 'BD'
  )
  $text = ($output -join [Environment]::NewLine)
  $match = [regex]::Match($text, '(?m)^eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\r?$')
  if (-not $match.Success) { throw "device-sim did not print a compact JWS token for $($Employee.Name)" }
  return $match.Value.Trim()
}

function Invoke-Scan([string]$Token) {
  $attemptId = [guid]::NewGuid().ToString()
  $payload = [ordered]@{
    gate_id = $GateId
    qr_token = $Token
    scanned_at = [DateTimeOffset]::UtcNow.ToString('o')
  } | ConvertTo-Json -Compress

  $request = [System.Net.Http.HttpRequestMessage]::new(
    [System.Net.Http.HttpMethod]::Post, "$EdgeUrl/kiosk/v1/scans"
  )
  $request.Headers.TryAddWithoutValidation('Idempotency-Key', $attemptId) | Out-Null
  $request.Headers.TryAddWithoutValidation('X-Kiosk-Id', $KioskId) | Out-Null
  $request.Content = [System.Net.Http.StringContent]::new(
    $payload, [System.Text.Encoding]::UTF8, 'application/json'
  )

  $timer = [System.Diagnostics.Stopwatch]::StartNew()
  try {
    $response = $script:http.SendAsync($request).GetAwaiter().GetResult()
    $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $timer.Stop()
    $parsed = $null
    try { $parsed = $body | ConvertFrom-Json -ErrorAction Stop } catch {}
    $outcome = if ($parsed) { [string]$parsed.outcome } else { 'INVALID_RESPONSE' }
    $reason = if ($parsed -and $parsed.reason_code) { [string]$parsed.reason_code } else { '' }
    return [pscustomobject]@{
      elapsedMs = [math]::Round($timer.Elapsed.TotalMilliseconds, 3)
      httpStatus = [int]$response.StatusCode
      outcome = $outcome
      reason = $reason
      success = ([int]$response.StatusCode -eq 200 -and $outcome -eq 'ALLOWED')
    }
  } catch {
    $timer.Stop()
    return [pscustomobject]@{
      elapsedMs = [math]::Round($timer.Elapsed.TotalMilliseconds, 3)
      httpStatus = 0
      outcome = 'REQUEST_ERROR'
      reason = $_.Exception.Message
      success = $false
    }
  } finally {
    $request.Dispose()
  }
}

function Get-Percentile([double[]]$Values, [double]$Percentile) {
  if (-not $Values -or $Values.Count -eq 0) { return [double]::NaN }
  $sorted = @($Values | Sort-Object)
  $index = [math]::Max(0, [math]::Ceiling($Percentile * $sorted.Count) - 1)
  return [double]$sorted[$index]
}

Add-Type -AssemblyName System.Net.Http
$script:http = [System.Net.Http.HttpClient]::new()
$script:http.Timeout = [TimeSpan]::FromSeconds(10)
$samples = [System.Collections.Generic.List[object]]::new()
$runWatch = [System.Diagnostics.Stopwatch]::new()

try {
  Invoke-RestMethod "$EdgeUrl/kiosk/v1/sync-bundle" -TimeoutSec 5 | Out-Null
  Invoke-RestMethod "$UserQrUrl/internal/v1/sync-bundle" -TimeoutSec 5 | Out-Null

  foreach ($employee in $employees) {
    Invoke-DeviceSim @(
      'keygen', '--name', $employee.Name, '--employee', $employee.Uuid, '--home', $profileHome
    ) | Out-Null
    Invoke-DeviceSim @(
      'register', '--name', $employee.Name, '--home', $profileHome, '--user-qr', $UserQrUrl
    ) | Out-Null
  }

  foreach ($employee in $employees) {
    for ($warmup = 0; $warmup -lt $WarmupPerEmployee; $warmup++) {
      $warmupResult = Invoke-Scan (New-Token $employee)
      if (-not $warmupResult.success) {
        throw "Warm-up failed for $($employee.Name): HTTP $($warmupResult.httpStatus), $($warmupResult.outcome), $($warmupResult.reason)"
      }
    }
  }

  Write-Host "Starting A-09: $Count measured scans, target $TargetRps requests/second, gate $GateId."
  $runWatch.Start()
  for ($index = 1; $index -le $Count; $index++) {
    if ($TargetRps -gt 0) {
      $scheduledSeconds = ($index - 1) / $TargetRps
      $remainingMs = ($scheduledSeconds - $runWatch.Elapsed.TotalSeconds) * 1000
      if ($remainingMs -gt 1) {
        Start-Sleep -Milliseconds ([int][math]::Floor($remainingMs))
      }
    }

    $employee = $employees[($index - 1) % $employees.Count]
    $token = New-Token $employee
    $sample = Invoke-Scan $token
    $sample | Add-Member -NotePropertyName index -NotePropertyValue $index
    $sample | Add-Member -NotePropertyName employee -NotePropertyValue $employee.Name
    $samples.Add($sample)

    if (($index % 100) -eq 0) {
      Write-Host ("Completed {0}/{1}; elapsed {2:N1} min" -f $index, $Count, $runWatch.Elapsed.TotalMinutes)
    }
  }
  $runWatch.Stop()

  $elapsedSeconds = [math]::Max(0.001, $runWatch.Elapsed.TotalSeconds)
  $latencies = [double[]]@($samples | ForEach-Object { $_.elapsedMs })
  $p50 = Get-Percentile $latencies 0.50
  $p95 = Get-Percentile $latencies 0.95
  $p99 = Get-Percentile $latencies 0.99
  $average = ($latencies | Measure-Object -Average).Average
  $successCount = @($samples | Where-Object success).Count
  $errorCount = $Count - $successCount
  $throughput = $Count / $elapsedSeconds
  $statusCounts = @($samples | Group-Object httpStatus | Sort-Object Name | ForEach-Object {
    '{0}: {1}' -f $_.Name, $_.Count
  }) -join '; '
  $outcomeCounts = @($samples | Group-Object outcome | Sort-Object Name | ForEach-Object {
    '{0}: {1}' -f $_.Name, $_.Count
  }) -join '; '

  $cpu = try { (Get-CimInstance Win32_Processor | Select-Object -First 1 -ExpandProperty Name) } catch { 'not available' }
  $memoryBytes = try { (Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory } catch { 0 }
  $memory = if ($memoryBytes) { '{0:N1} GiB' -f ($memoryBytes / 1GB) } else { 'not available' }
  $os = [System.Environment]::OSVersion.VersionString
  $commit = (& git -C $root rev-parse --short HEAD 2>$null)
  if (-not $commit) { $commit = 'unknown' }
  $gitChanges = @(& git -C $root status --porcelain)
  $workingTree = if ($gitChanges.Count -gt 0) { 'dirty' } else { 'clean' }

  $passed = (
    $Count -eq 800 -and
    $elapsedSeconds -le 1800 -and
    $p95 -le 1000 -and
    $p99 -le 2000 -and
    $errorCount -eq 0
  )
  $verdict = if ($passed) { 'PASS' } else { 'FAIL' }
  $date = [DateTimeOffset]::Now.ToString('yyyy-MM-dd HH:mm:ss zzz')
  $report = @"
# A-09 ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â KiÃƒÂ¡Ã‚Â»Ã†â€™m thÃƒÂ¡Ã‚Â»Ã‚Â­ tÃƒÂ¡Ã‚ÂºÃ‚Â£i Gate Station

- KÃƒÂ¡Ã‚ÂºÃ‚Â¿t quÃƒÂ¡Ã‚ÂºÃ‚Â£: **$verdict**
- ThÃƒÂ¡Ã‚Â»Ã‚Âi Ãƒâ€žÃ¢â‚¬ËœiÃƒÂ¡Ã‚Â»Ã†â€™m: $date
- Base commit: $commit; working tree: $workingTree.
- MÃƒÆ’Ã‚Â´i trÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Âng: Windows; Java 21; profile local; HTTP thÃƒÂ¡Ã‚ÂºÃ‚Â­t giÃƒÂ¡Ã‚Â»Ã‚Â¯a gate-station, user-qr, attendance vÃƒÆ’Ã‚Â  turnstile-simulator; khÃƒÆ’Ã‚Â´ng dÃƒÆ’Ã‚Â¹ng Docker.
- MÃƒÆ’Ã‚Â¡y: $cpu; RAM $memory; OS $os.
- KÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¹ch bÃƒÂ¡Ã‚ÂºÃ‚Â£n: $Count lÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Â£t POST /kiosk/v1/scans tÃƒÂ¡Ã‚Â»Ã¢â‚¬Âºi $GateId, luÃƒÆ’Ã‚Â¢n phiÃƒÆ’Ã‚Âªn 4 nhÃƒÆ’Ã‚Â¢n viÃƒÆ’Ã‚Âªn mÃƒÂ¡Ã‚ÂºÃ‚Â«u, token ES256 mÃƒÂ¡Ã‚Â»Ã¢â‚¬Âºi cho mÃƒÂ¡Ã‚Â»Ã¢â‚¬â€i lÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Â£t, cÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¢ng hai chiÃƒÂ¡Ã‚Â»Ã‚Âu, $($WarmupPerEmployee * $employees.Count) lÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Â£t warm-up khÃƒÆ’Ã‚Â´ng tÃƒÆ’Ã‚Â­nh vÃƒÆ’Ã‚Â o mÃƒÂ¡Ã‚ÂºÃ‚Â«u.
- TÃƒÂ¡Ã‚Â»Ã¢â‚¬Ëœc Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã¢â€žÂ¢ Ãƒâ€žÃ¢â‚¬ËœÃƒÆ’Ã‚Â­ch: $TargetRps lÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Â£t/giÃƒÆ’Ã‚Â¢y; thÃƒÂ¡Ã‚Â»Ã‚Âi gian Ãƒâ€žÃ¢â‚¬Ëœo: $([math]::Round($elapsedSeconds, 2)) giÃƒÆ’Ã‚Â¢y; thÃƒÆ’Ã‚Â´ng lÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Â£ng thÃƒÂ¡Ã‚Â»Ã‚Â±c tÃƒÂ¡Ã‚ÂºÃ‚Â¿: $([math]::Round($throughput, 3)) lÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Â£t/giÃƒÆ’Ã‚Â¢y.

| ChÃƒÂ¡Ã‚Â»Ã¢â‚¬Â° sÃƒÂ¡Ã‚Â»Ã¢â‚¬Ëœ | KÃƒÂ¡Ã‚ÂºÃ‚Â¿t quÃƒÂ¡Ã‚ÂºÃ‚Â£ | TiÃƒÆ’Ã‚Âªu chÃƒÆ’Ã‚Â­ |
| --- | ---: | ---: |
| LÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Â£t Ãƒâ€žÃ¢â‚¬Ëœo thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng | $successCount / $Count | 800 / 800 |
| LÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Â£t lÃƒÂ¡Ã‚Â»Ã¢â‚¬â€i | $errorCount | 0 |
| HTTP status | $statusCounts | mÃƒÂ¡Ã‚Â»Ã‚Âi lÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Â£t 200 |
| KÃƒÂ¡Ã‚ÂºÃ‚Â¿t quÃƒÂ¡Ã‚ÂºÃ‚Â£ ÃƒÂ¡Ã‚Â»Ã‚Â©ng dÃƒÂ¡Ã‚Â»Ã‚Â¥ng | $outcomeCounts | mÃƒÂ¡Ã‚Â»Ã‚Âi lÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Â£t ALLOWED |
| Trung bÃƒÆ’Ã‚Â¬nh | $([math]::Round($average, 2)) ms | ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â |
| p50 | $([math]::Round($p50, 2)) ms | ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â |
| p95 | $([math]::Round($p95, 2)) ms | ÃƒÂ¢Ã¢â‚¬Â°Ã‚Â¤ 1,000 ms |
| p99 | $([math]::Round($p99, 2)) ms | ÃƒÂ¢Ã¢â‚¬Â°Ã‚Â¤ 2,000 ms |

LÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡nh chÃƒÂ¡Ã‚ÂºÃ‚Â¡y lÃƒÂ¡Ã‚ÂºÃ‚Â¡i tÃƒÂ¡Ã‚Â»Ã‚Â« thÃƒâ€ Ã‚Â° mÃƒÂ¡Ã‚Â»Ã‚Â¥c gÃƒÂ¡Ã‚Â»Ã¢â‚¬Ëœc repo: pwsh scripts/perf/gate-station-a09.ps1.

Latency tÃƒÆ’Ã‚Â­nh tÃƒÂ¡Ã‚Â»Ã‚Â« lÃƒÆ’Ã‚Âºc gÃƒÂ¡Ã‚Â»Ã‚Â­i HTTP request Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚ÂºÃ‚Â¿n khi Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã‚Âc xong response; thÃƒÂ¡Ã‚Â»Ã‚Âi gian khÃƒÂ¡Ã‚Â»Ã…Â¸i tÃƒÂ¡Ã‚ÂºÃ‚Â¡o, Ãƒâ€žÃ¢â‚¬ËœÃƒâ€žÃ†â€™ng kÃƒÆ’Ã‚Â½ thiÃƒÂ¡Ã‚ÂºÃ‚Â¿t bÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¹, warm-up vÃƒÆ’Ã‚Â  sinh token khÃƒÆ’Ã‚Â´ng nÃƒÂ¡Ã‚ÂºÃ‚Â±m trong latency. TiÃƒÂ¡Ã‚ÂºÃ‚Â¿n trÃƒÆ’Ã‚Â¬nh phÃƒÆ’Ã‚Â¡t token chÃƒÂ¡Ã‚ÂºÃ‚Â¡y tuÃƒÂ¡Ã‚ÂºÃ‚Â§n tÃƒÂ¡Ã‚Â»Ã‚Â± vÃƒÆ’Ã‚Â  Ãƒâ€žÃ¢â‚¬ËœÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Â£c pacing ÃƒÂ¡Ã‚Â»Ã…Â¸ tÃƒÂ¡Ã‚Â»Ã¢â‚¬Ëœc Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã¢â€žÂ¢ Ãƒâ€žÃ¢â‚¬ËœÃƒÆ’Ã‚Â­ch; kÃƒÂ¡Ã‚ÂºÃ‚Â¿t quÃƒÂ¡Ã‚ÂºÃ‚Â£ nÃƒÆ’Ã‚Â y ÃƒÆ’Ã‚Â¡p dÃƒÂ¡Ã‚Â»Ã‚Â¥ng cho mÃƒÆ’Ã‚Â¡y phÃƒÆ’Ã‚Â¡t tÃƒÂ¡Ã‚ÂºÃ‚Â£i vÃƒÆ’Ã‚Â  stack local nÃƒÆ’Ã‚Âªu trÃƒÆ’Ã‚Âªn, khÃƒÆ’Ã‚Â´ng Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚ÂºÃ‚Â¡i diÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡n cho tÃƒÂ¡Ã‚ÂºÃ‚Â£i Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“ng thÃƒÂ¡Ã‚Â»Ã‚Âi hoÃƒÂ¡Ã‚ÂºÃ‚Â·c stack Docker Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚ÂºÃ‚Â§y Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã‚Â§.
"@

  $outputDirectory = Split-Path -Parent $OutputPath
  New-Item -ItemType Directory -Force $outputDirectory | Out-Null
  [System.IO.File]::WriteAllText(
    $OutputPath,
    $report,
    [System.Text.UTF8Encoding]::new($false)
  )
  Write-Host "Wrote A-09 report to $OutputPath"
  Write-Host ("p50 {0:N2} ms; p95 {1:N2} ms; p99 {2:N2} ms; {3} errors; verdict {4}" -f $p50, $p95, $p99, $errorCount, $verdict)

  if (-not $passed) {
    throw 'A-09 acceptance criteria were not met; see the generated report for measured results.'
  }
} finally {
  $runWatch.Stop()
  $script:http.Dispose()
}