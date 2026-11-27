<#
.SYNOPSIS
  Hands-on trial of Team D's work on real PostgreSQL and RabbitMQ (D-04, D-05, D-06, D-08, D-13).

.DESCRIPTION
  Starts the central PostgreSQL and RabbitMQ from docker/docker-compose.yml, identity (profile
  "local", so admin1 can pass TOTP with the dev secret) and reporting, hr-integration, audit-admin on
  their DEFAULT profile (JDBC, RabbitMQ, JWT only). Then it checks, printing PASS or FAIL per step:

    D-13  no token -> 401; dev headers -> 401; supervisor token -> 200; wrong role -> 403
    D-04  an entry is projected into PostgreSQL and is still there after reporting restarts
    D-05  BD then LA ten minutes apart -> one IMPOSSIBLE_TRAVEL alert
    D-06  the simulated HR took both passes (sync status SYNCED, kept in sync_log)
    D-08  audit event stored; chain verification valid; the service login cannot UPDATE history

  Events are published straight to the tmc.events exchange through the RabbitMQ management API, as
  attendance would publish them. Logs: .local\trial-*.log.

    pwsh scripts/demo/team-d-trial.ps1                         # 5433 free
    pwsh scripts/demo/team-d-trial.ps1 -CentralDbPort 55433    # 5433 used by another project
    pwsh scripts/demo/team-d-trial.ps1 -Stop                   # stop the services started here

  A Docker volume created before 27/11 lacks the auditadmin_app login: run `make nuke` first, or see
  docs/runbook/troubleshooting.md.
#>
param(
  [int]$CentralDbPort = 5433,
  [switch]$Stop
)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$Run = Join-Path $Root '.local'
New-Item -ItemType Directory -Force $Run | Out-Null
$Version = '0.1.0-SNAPSHOT'
$Db = "jdbc:postgresql://localhost:$CentralDbPort"

$Services = @(
  @{ Name = 'identity'; Port = 8087; Jar = "services\identity\build\libs\identity-$Version.jar"
     Env = @{ SPRING_PROFILES_ACTIVE = 'local' } },
  @{ Name = 'reporting'; Port = 8084; Jar = "services\reporting\build\libs\reporting-$Version.jar"
     Env = @{ REPORTING_DB_URL = "$Db/reporting" } },
  @{ Name = 'hr-integration'; Port = 8085; Jar = "services\hr-integration\build\libs\hr-integration-$Version.jar"
     Env = @{ HR_DB_URL = "$Db/hr_integration" } },
  @{ Name = 'audit-admin'; Port = 8086; Jar = "services\audit-admin\build\libs\audit-admin-$Version.jar"
     Env = @{ AUDIT_DB_URL = "$Db/audit_admin" } }
)

function Get-Java {
  if ($env:JAVA_HOME) { return (Join-Path $env:JAVA_HOME 'bin\java.exe') }
  return 'java'
}

function Test-Port([int]$Port) {
  [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Stop-TrialService([hashtable]$s) {
  $pidFile = Join-Path $Run "trial-$($s.Name).pid"
  if (Test-Path $pidFile) {
    $id = [int](Get-Content $pidFile)
    $p = Get-Process -Id $id -ErrorAction SilentlyContinue
    if ($p -and $p.ProcessName -like 'java*') { Stop-Process -Id $id -Force }
    Remove-Item $pidFile -Force
  }
  # whatever Java process still listens on the service port was started by an earlier trial run
  $owner = Get-NetTCPConnection -LocalPort $s.Port -State Listen -ErrorAction SilentlyContinue |
    Select-Object -First 1 -ExpandProperty OwningProcess
  if ($owner) {
    $p = Get-Process -Id $owner -ErrorAction SilentlyContinue
    if ($p -and $p.ProcessName -like 'java*') { Stop-Process -Id $owner -Force }
  }
  $deadline = (Get-Date).AddSeconds(20)
  while ((Test-Port $s.Port) -and (Get-Date) -lt $deadline) { Start-Sleep -Milliseconds 300 }
}

function Start-TrialService([hashtable]$s) {
  if (Test-Port $s.Port) { throw "port $($s.Port) is already in use; stop whatever runs there first" }
  $log = Join-Path $Run "trial-$($s.Name).log"
  # The child inherits the environment, so set the service's variables only around the start.
  # cmd writes the output to the log and runs detached: this script never holds the child's pipes.
  $saved = @{}
  foreach ($k in @('SPRING_PROFILES_ACTIVE') + @($s.Env.Keys)) {
    $saved[$k] = [Environment]::GetEnvironmentVariable($k)
    [Environment]::SetEnvironmentVariable($k, $s.Env[$k])
  }
  try {
    $cmd = "/c `"`"$(Get-Java)`" -jar `"$(Join-Path $Root $s.Jar)`" > `"$log`" 2>&1`""
    $p = Start-Process cmd.exe -ArgumentList $cmd -WorkingDirectory $Root -WindowStyle Hidden -PassThru
  } finally {
    foreach ($k in $saved.Keys) { [Environment]::SetEnvironmentVariable($k, $saved[$k]) }
  }
  Start-Sleep -Seconds 2
  $java = Get-CimInstance Win32_Process -Filter "ParentProcessId=$($p.Id)" | Select-Object -First 1
  Set-Content (Join-Path $Run "trial-$($s.Name).pid") ($(if ($java) { $java.ProcessId } else { $p.Id }))
  $deadline = (Get-Date).AddSeconds(90)
  while (-not (Test-Port $s.Port)) {
    if ((Get-Date) -gt $deadline) { throw "$($s.Name) did not start; see $log" }
    Start-Sleep -Milliseconds 500
  }
  Write-Host "  $($s.Name) up on :$($s.Port)"
}

if ($Stop) {
  foreach ($s in $Services) { Stop-TrialService $s }
  Write-Host 'trial services stopped (PostgreSQL and RabbitMQ keep running: make down)'
  return
}

# ---------- infrastructure ----------
Write-Host "starting PostgreSQL central on :$CentralDbPort and RabbitMQ ..."
$env:TMC_CENTRAL_DB_PORT = "$CentralDbPort"
& docker compose -f (Join-Path $Root 'docker\docker-compose.yml') up -d --wait postgres-central rabbitmq
if ($LASTEXITCODE -ne 0) { throw 'docker compose failed' }

# Queues declared by an old build without a DLQ make the consumers fail to bind
# (PRECONDITION_FAILED x-dead-letter-exchange, docs/runbook/troubleshooting.md). Delete only those.
$mgmt = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('tmc:tmc')) }
foreach ($q in @('tmc.reporting.attendance', 'tmc.hr.attendance', 'tmc.audit.events')) {
  $info = Invoke-RestMethod -Uri "http://localhost:15672/api/queues/%2F/$q" -Headers $mgmt -SkipHttpErrorCheck
  if ($info.name -and -not $info.arguments.'x-dead-letter-exchange' -and $null -eq $info.arguments.'x-dead-letter-exchange') {
    Write-Host "  deleting stale queue $q (declared without a DLQ, $($info.messages) old message(s))"
    Invoke-RestMethod -Method Delete -Uri "http://localhost:15672/api/queues/%2F/$q" -Headers $mgmt | Out-Null
  }
}

# A volume created before 27/11 has no auditadmin_app login (init scripts run only on an empty
# volume). Create it and grant what Flyway V2 would have granted; harmless when already there.
$ensureRole = @'
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'auditadmin_app') THEN
    CREATE ROLE auditadmin_app LOGIN PASSWORD 'auditadmin_app';
  END IF;
END $$;
'@
$ensureRole | & docker exec -i tmc-postgres-central psql -q -U postgres -d postgres | Out-Null
$grants = @'
DO $$ BEGIN
  IF to_regclass('public.audit_entries') IS NOT NULL THEN
    GRANT SELECT, INSERT ON audit_entries, processed_messages TO auditadmin_app;
    GRANT USAGE ON SEQUENCE audit_entries_seq_seq TO auditadmin_app;
  END IF;
END $$;
'@
$grants | & docker exec -i tmc-postgres-central psql -q -U postgres -d audit_admin | Out-Null

$missing = $Services | Where-Object { -not (Test-Path (Join-Path $Root $_.Jar)) }
if ($missing) {
  Push-Location $Root
  try {
    & .\gradlew.bat -q :services:identity:bootJar :services:reporting:bootJar `
      :services:hr-integration:bootJar :services:audit-admin:bootJar
    if ($LASTEXITCODE -ne 0) { throw 'gradle build failed' }
  } finally { Pop-Location }
}

Write-Host 'starting services ...'
foreach ($s in $Services) { Stop-TrialService $s; Start-TrialService $s }

# ---------- helpers ----------
$script:Failures = 0
function Check([string]$Name, [bool]$Ok, [string]$Detail = '') {
  if ($Ok) { Write-Host "PASS  $Name" -ForegroundColor Green }
  else { $script:Failures++; Write-Host "FAIL  $Name  $Detail" -ForegroundColor Red }
}

function Invoke-Status([string]$Url, [hashtable]$Headers = @{}) {
  try {
    $r = Invoke-WebRequest -Uri $Url -Headers $Headers -UseBasicParsing -SkipHttpErrorCheck
    return @{ Status = [int]$r.StatusCode; Body = $r.Content }
  } catch { return @{ Status = 0; Body = $_.Exception.Message } }
}

function Get-Token([string]$User, [string]$Mfa = $null) {
  $body = @{ username = $User; password = 'dev-only-change-me' }
  if ($Mfa) { $body.mfaCode = $Mfa }
  $r = Invoke-RestMethod -Method Post -Uri 'http://localhost:8087/auth/v1/login' `
    -ContentType 'application/json' -Body ($body | ConvertTo-Json) -SkipHttpErrorCheck
  return $r.accessToken
}

# RFC 6238 TOTP (SHA-1, 6 digits, 30 s) for the dev secret of admin1 (DevSeed)
function Get-Totp([string]$Base32) {
  $alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
  $bits = ($Base32.ToUpper().ToCharArray() | ForEach-Object {
      [Convert]::ToString($alphabet.IndexOf($_), 2).PadLeft(5, '0') }) -join ''
  $key = for ($i = 0; $i + 8 -le $bits.Length; $i += 8) { [Convert]::ToByte($bits.Substring($i, 8), 2) }
  $counter = [BitConverter]::GetBytes([long][Math]::Floor([DateTimeOffset]::UtcNow.ToUnixTimeSeconds() / 30))
  [Array]::Reverse($counter)
  $hash = (New-Object System.Security.Cryptography.HMACSHA1 (, [byte[]]$key)).ComputeHash($counter)
  $o = $hash[19] -band 0xf
  # [int] first: shifting a [byte] in PowerShell stays a byte and loses the bits
  $code = (([int]$hash[$o] -band 0x7f) -shl 24) -bor ([int]$hash[$o + 1] -shl 16) -bor ([int]$hash[$o + 2] -shl 8) -bor [int]$hash[$o + 3]
  return ($code % 1000000).ToString('D6')
}

$Rabbit = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('tmc:tmc')) }
function Publish([string]$RoutingKey, [hashtable]$Envelope) {
  $msg = @{
    properties = @{ content_type = 'application/json' }
    routing_key = $RoutingKey
    payload = ($Envelope | ConvertTo-Json -Depth 5 -Compress)
    payload_encoding = 'string'
  } | ConvertTo-Json -Depth 5
  $r = Invoke-RestMethod -Method Post -Headers $Rabbit -ContentType 'application/json' `
    -Uri 'http://localhost:15672/api/exchanges/%2F/tmc.events/publish' -Body $msg
  if (-not $r.routed) { throw "message $RoutingKey was not routed to any queue" }
}

function Envelope([string]$Type, [string]$AttemptId, [string]$Factory, [hashtable]$Payload) {
  @{
    messageId = [guid]::NewGuid().ToString(); eventType = $Type; schemaVersion = 1
    eventId = $AttemptId; aggregateVersion = 5; occurredAt = (Get-Date).ToUniversalTime().ToString('o')
    correlationId = 'trial'; source = 'attendance-edge'; factoryCode = $Factory
    gateId = "$Factory-GATE-01"; payload = $Payload
  }
}

function Wait-Until([scriptblock]$Condition, [int]$Seconds = 20) {
  $deadline = (Get-Date).AddSeconds($Seconds)
  while ((Get-Date) -lt $deadline) { if (& $Condition) { return $true }; Start-Sleep -Milliseconds 500 }
  return $false
}

# ---------- D-13 ----------
Write-Host "`n--- D-13: identity JWT on the central services"
$presence = 'http://localhost:8084/supervision/v1/presence?factory=BD'
Check 'no token -> 401' ((Invoke-Status $presence).Status -eq 401)
Check 'dev headers on the default profile -> 401' ((Invoke-Status $presence @{
      'X-Subject' = [guid]::NewGuid().ToString(); 'X-Roles' = 'PRODUCTION_SUPERVISOR'; 'X-Scope-Factories' = 'BD' }).Status -eq 401)
$supervisor = Get-Token 'supervisor1'
Check 'supervisor1 logs in and gets a token' ([bool]$supervisor)
$auth = @{ Authorization = "Bearer $supervisor" }
Check 'supervisor token -> presence 200' ((Invoke-Status $presence $auth).Status -eq 200)
Check 'supervisor token on audit -> 403' ((Invoke-Status 'http://localhost:8086/admin/v1/audit' $auth).Status -eq 403)

# ---------- D-04, D-05, D-06 ----------
Write-Host "`n--- D-04 / D-05 / D-06: events from attendance"
$employee = [guid]::NewGuid().ToString()
$bdAttempt = [guid]::NewGuid().ToString()
$laAttempt = [guid]::NewGuid().ToString()
$t0 = (Get-Date).ToUniversalTime().AddMinutes(-20)
Publish 'attendance.AttendanceConfirmed' (Envelope 'AttendanceConfirmed' $bdAttempt 'BD' @{
    employeeUuid = $employee; direction = 'ENTRY'; scannedAt = $t0.ToString('o') })
Publish 'attendance.AttendanceConfirmed' (Envelope 'AttendanceConfirmed' $laAttempt 'LA' @{
    employeeUuid = $employee; direction = 'ENTRY'; scannedAt = $t0.AddMinutes(10).ToString('o') })

# (...) around Invoke-RestMethod: PowerShell 7 otherwise passes a JSON array down the pipe as one object
$inBd = { (Invoke-RestMethod -Uri $presence -Headers $auth) | Where-Object { $_.employeeUuid -eq $employee } }
Check 'D-04 entry at BD is projected' (Wait-Until { [bool](& $inBd) })
$alerts = { (Invoke-RestMethod -Uri 'http://localhost:8084/supervision/v1/alerts?factory=BD' -Headers $auth) |
    Where-Object { $_.employeeUuid -eq $employee } }
Check 'D-05 BD then LA 10 minutes apart -> IMPOSSIBLE_TRAVEL alert' (Wait-Until { @(& $alerts).Count -eq 1 })
$alert = @(& $alerts)[0]
if ($alert) { Write-Host "      $($alert.rule): $($alert.detail)" }

Write-Host '  restarting reporting ...'
$reporting = $Services | Where-Object Name -eq 'reporting'
Stop-TrialService $reporting; Start-TrialService $reporting
Check 'D-04 presence survives the restart (PostgreSQL)' (Wait-Until { [bool](& $inBd) })
Check 'D-05 alert survives the restart' (@(& $alerts).Count -eq 1)

$admin = Get-Token 'admin1' (Get-Totp 'JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP')
Check 'admin1 logs in with TOTP' ([bool]$admin)
$adminAuth = @{ Authorization = "Bearer $admin" }
$synced = { (Invoke-RestMethod -Uri 'http://localhost:8085/admin/v1/hr-sync?status=SYNCED' -Headers $adminAuth) |
    Where-Object { $_.eventId -in @($bdAttempt, $laAttempt) } }
Check 'D-06 both passes SYNCED to the (simulated) HR' (Wait-Until { @(& $synced).Count -eq 2 })

# ---------- D-08 ----------
Write-Host "`n--- D-08: append-only audit with a hash chain"
$auditId = [guid]::NewGuid().ToString()
Publish 'identity.AuditRecorded' (Envelope 'AuditRecorded' $auditId 'BD' @{
    action = 'TRIAL_CHECK'; actor = $employee; reasonCode = 'TRIAL' })
$found = { (Invoke-RestMethod -Uri "http://localhost:8086/admin/v1/audit?action=TRIAL_CHECK&actor=$employee" -Headers $adminAuth) }
Check 'audit event stored and found by action and actor' (Wait-Until { @(& $found).Count -ge 1 })
$verification = Invoke-RestMethod -Uri 'http://localhost:8086/admin/v1/audit/verification' -Headers $adminAuth
Check "hash chain valid ($($verification.checked) entries)" ($verification.valid -eq $true)
$update = & docker exec -e PGPASSWORD=auditadmin_app tmc-postgres-central psql -h localhost -U auditadmin_app -d audit_admin `
  -c "UPDATE audit_entries SET action = 'FORGED'" 2>&1 | Out-String
Check 'service login cannot UPDATE audit_entries' ($update -match 'permission denied') $update.Trim()

Write-Host ''
if ($script:Failures -eq 0) { Write-Host 'ALL CHECKS PASSED' -ForegroundColor Green }
else { Write-Host "$script:Failures CHECK(S) FAILED - logs in .local\trial-*.log" -ForegroundColor Red }
Write-Host 'Services keep running for manual tries (portal: cd apps/web-portal; npm run dev). Stop: pwsh scripts/demo/team-d-trial.ps1 -Stop'
exit $script:Failures
