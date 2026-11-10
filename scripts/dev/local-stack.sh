#!/usr/bin/env bash
# Starts or stops the zero-Docker local edge stack: turnstile simulator, user-qr, attendance, gate-station.
# State lives in process memory ("local" profile), so `restart` gives you a clean slate.
#
#   scripts/dev/local-stack.sh start     build jars if missing, start the four processes, wait until ready
#   scripts/dev/local-stack.sh stop
#   scripts/dev/local-stack.sh restart
#   scripts/dev/local-stack.sh status
#   scripts/dev/local-stack.sh logs [service]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RUN="$ROOT/.local"
mkdir -p "$RUN"
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null || true)}"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"

# name:port:jar
SERVICES=(
  "turnstile:8090:simulators/turnstile-simulator/build/libs/turnstile-simulator-0.1.0-SNAPSHOT.jar"
  "user-qr:8083:services/user-qr/build/libs/user-qr-0.1.0-SNAPSHOT.jar"
  "attendance:8082:services/attendance/build/libs/attendance-0.1.0-SNAPSHOT.jar"
  "gate-station:8081:services/gate-station/build/libs/gate-station-0.1.0-SNAPSHOT.jar"
)

listening() { lsof -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1; }

build() {
  echo "building jars and simulator tools ..."
  (cd "$ROOT" && ./gradlew -q \
    :services:gate-station:bootJar :services:attendance:bootJar :services:user-qr:bootJar \
    :simulators:turnstile-simulator:bootJar \
    :simulators:device-simulator:installDist :simulators:kiosk-simulator:installDist)
}

start() {
  for entry in "${SERVICES[@]}"; do IFS=: read -r _ _ jar <<<"$entry"; [[ -f "$ROOT/$jar" ]] || { build; break; }; done
  for entry in "${SERVICES[@]}"; do
    IFS=: read -r name port jar <<<"$entry"
    if listening "$port"; then echo "$name already listening on $port"; continue; fi
    case "$name" in
      turnstile) env_vars=() ;;
      gate-station) env_vars=(SPRING_PROFILES_ACTIVE=local TURNSTILE_MODE=http TURNSTILE_URL=http://localhost:8090) ;;
      *) env_vars=(SPRING_PROFILES_ACTIVE=local) ;;
    esac
    nohup env "${env_vars[@]+"${env_vars[@]}"}" "$JAVA" -jar "$ROOT/$jar" >"$RUN/$name.log" 2>&1 &
    echo $! >"$RUN/$name.pid"
    echo "started $name (pid $!) on :$port"
  done
  echo "waiting for the stack ..."
  for _ in $(seq 1 60); do
    ready=0
    for entry in "${SERVICES[@]}"; do IFS=: read -r _ port _ <<<"$entry"; listening "$port" && ready=$((ready + 1)); done
    [[ $ready -eq ${#SERVICES[@]} ]] && { echo "stack is up"; return 0; }
    sleep 2
  done
  echo "stack did not come up in time; see $RUN/*.log" >&2
  return 1
}

stop() {
  for entry in "${SERVICES[@]}"; do
    IFS=: read -r name _ _ <<<"$entry"
    if [[ -f "$RUN/$name.pid" ]]; then
      kill "$(cat "$RUN/$name.pid")" 2>/dev/null && echo "stopped $name" || true
      rm -f "$RUN/$name.pid"
    fi
  done
}

status() {
  for entry in "${SERVICES[@]}"; do
    IFS=: read -r name port _ <<<"$entry"
    listening "$port" && echo "$name :$port up" || echo "$name :$port DOWN"
  done
}

case "${1:-}" in
  start) start ;;
  stop) stop ;;
  restart) stop; sleep 2; start ;;
  status) status ;;
  logs) tail -f "$RUN/${2:-gate-station}.log" ;;
  *) echo "usage: $0 start|stop|restart|status|logs [service]"; exit 2 ;;
esac
