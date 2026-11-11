<#
.SYNOPSIS
  Windows counterpart of scripts/dev/local-stack.sh: starts or stops the zero-Docker local stack.

.DESCRIPTION
  Starts five processes with the "local" profile (state in process memory, real HTTP between them):
    turnstile-simulator :8090, user-qr :8083, attendance :8082, gate-station :8081, identity :8087
  Logs and pid files go to .local\ . Because state is in memory, "restart" gives a clean slate.

    pwsh scripts/dev/local-stack.ps1 start     build jars if missing, start, wait until ready
    pwsh scripts/dev/local-stack.ps1 stop
    pwsh scripts/dev/local-stack.ps1 restart
    pwsh scripts/dev/local-stack.ps1 status

  Java 21 is taken from JAVA_HOME, else from "java" on PATH.
#>
param(
  [Parameter(Position = 0)]
  [ValidateSet('start', 'stop', 'restart', 'status')]
  [string]$Command = 'status'
)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$Run = Join-Path $Root '.local'
New-Item -ItemType Directory -Force $Run | Out-Null
$Version = '0.1.0-SNAPSHOT'

$Services = @(
  @{ Name = 'turnstile';    Port = 8090; Jar = "simulators\turnstile-simulator\build\libs\turnstile-simulator-$Version.jar"; Env = @{} },
  @{ Name = 'user-qr';      Port = 8083; Jar = "services\user-qr\build\libs\user-qr-$Version.jar";           Env = @{ SPRING_PROFILES_ACTIVE = 'local' } },
  @{ Name = 'attendance';   Port = 8082; Jar = "services\attendance\build\libs\attendance-$Version.jar";     Env = @{ SPRING_PROFILES_ACTIVE = 'local' } },
  @{ Name = 'gate-station'; Port = 8081; Jar = "services\gate-station\build\libs\gate-station-$Version.jar"; Env = @{ SPRING_PROFILES_ACTIVE = 'local'; TURNSTILE_MODE = 'http'; TURNSTILE_URL = 'http://localhost:8090' } },
  @{ Name = 'identity';     Port = 8087; Jar = "services\identity\build\libs\identity-$Version.jar";         Env = @{} }
)

function Get-Java {
  if ($env:JAVA_HOME) { return (Join-Path $env:JAVA_HOME 'bin\java.exe') }
  return 'java'
}

function Get-Listener([int]$Port) {
  Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
}

function Invoke-Build {
  Write-Host 'building jars and simulator tools ...'
  Push-Location $Root
  try {
    & .\gradlew.bat -q `
      :services:gate-station:bootJar :services:attendance:bootJar :services:user-qr:bootJar `
      :services:identity:bootJar :simulators:turnstile-simulator:bootJar `
      :simulators:device-simulator:installDist :simulators:kiosk-simulator:installDist
    if ($LASTEXITCODE -ne 0) { throw 'gradle build failed' }
  } finally { Pop-Location }
}

function Start-Stack {
  if ($Services | Where-Object { -not (Test-Path (Join-Path $Root $_.Jar)) }) { Invoke-Build }
  $java = Get-Java
  foreach ($s in $Services) {
    if (Get-Listener $s.Port) { Write-Host "$($s.Name) already listening on $($s.Port)"; continue }
    $saved = @{}
    foreach ($k in $s.Env.Keys) { $saved[$k] = [Environment]::GetEnvironmentVariable($k); [Environment]::SetEnvironmentVariable($k, $s.Env[$k]) }
    try {
      $p = Start-Process -FilePath $java -ArgumentList '-jar', "`"$(Join-Path $Root $s.Jar)`"" `
        -RedirectStandardOutput (Join-Path $Run "$($s.Name).log") `
        -RedirectStandardError (Join-Path $Run "$($s.Name).err.log") `
        -WindowStyle Hidden -PassThru
    } finally {
      foreach ($k in $saved.Keys) { [Environment]::SetEnvironmentVariable($k, $saved[$k]) }
    }
    Set-Content -Path (Join-Path $Run "$($s.Name).pid") -Value $p.Id
    Write-Host "started $($s.Name) (pid $($p.Id)) on :$($s.Port)"
  }
  Write-Host 'waiting for the stack ...'
  foreach ($i in 1..60) {
    $ready = @($Services | Where-Object { Get-Listener $_.Port }).Count
    if ($ready -eq $Services.Count) { Write-Host 'stack is up'; return }
    Start-Sleep -Seconds 2
  }
  throw "stack did not come up in time; see $Run\*.log"
}

function Stop-Stack {
  foreach ($s in $Services) {
    $listener = Get-Listener $s.Port
    if (-not $listener) { Write-Host "$($s.Name) not running"; continue }
    $proc = Get-Process -Id $listener.OwningProcess -ErrorAction SilentlyContinue
    # Only ever stop a Java process: never kill something unrelated that happens to hold the port.
    if ($proc -and $proc.ProcessName -like 'java*') {
      Stop-Process -Id $proc.Id -Force
      Write-Host "stopped $($s.Name) (pid $($proc.Id))"
    } else {
      Write-Warning "port $($s.Port) is held by '$($proc.ProcessName)', not Java; left alone"
    }
    Remove-Item (Join-Path $Run "$($s.Name).pid") -ErrorAction SilentlyContinue
  }
}

function Show-Status {
  foreach ($s in $Services) {
    $state = if (Get-Listener $s.Port) { 'UP' } else { 'down' }
    '{0,-13} :{1}  {2}' -f $s.Name, $s.Port, $state
  }
}

switch ($Command) {
  'start' { Start-Stack }
  'stop' { Stop-Stack }
  'restart' { Stop-Stack; Start-Sleep -Seconds 1; Start-Stack }
  'status' { Show-Status }
}
