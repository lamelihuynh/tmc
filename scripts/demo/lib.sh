#!/usr/bin/env bash
# Shared helpers for the demo scripts. Requires the local stack from docs/runbook/local-setup.md.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EDGE="${EDGE:-http://localhost:8081}"
ATTENDANCE="${ATTENDANCE:-http://localhost:8082}"
USERQR="${USERQR:-http://localhost:8083}"
TURNSTILE="${TURNSTILE:-http://localhost:8090}"
# Each demo uses its own seeded employee so anti-passback state of one demo cannot disturb another.
# Restart the local stack to reset the in-memory state before running a demo a second time.
EMP_ONLINE="0192a1c0-0000-7000-8000-000000000001"
EMP_OFFLINE="0192a1c0-0000-7000-8000-000000000004"
EMP_TIMEOUT="0192a1c0-0000-7000-8000-000000000005"

DEVICE_SIM="$ROOT/simulators/device-simulator/build/install/device-sim/bin/device-sim"
KIOSK_SIM="$ROOT/simulators/kiosk-simulator/build/install/kiosk-sim/bin/kiosk-sim"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

say() { printf '\n\033[1m== %s\033[0m\n' "$*"; }
note() { printf '   %s\n' "$*"; }

require_stack() {
  for url in "$EDGE/kiosk/v1/sync-bundle" "$USERQR/internal/v1/sync-bundle" "$TURNSTILE/gates/BD-HW-01/status"; do
    if ! curl -sf -o /dev/null "$url"; then
      echo "stack is not running: $url did not answer. See docs/runbook/local-setup.md" >&2
      exit 1
    fi
  done
  if [[ ! -x "$DEVICE_SIM" || ! -x "$KIOSK_SIM" ]]; then
    echo "simulators are not built. Run: make sims" >&2
    exit 1
  fi
}

# usage: expect "ALLOWED" "$output"
expect() {
  if [[ "$2" == *"$1"* ]]; then
    printf '   \033[32mOK\033[0m   got "%s"\n' "$1"
  else
    printf '   \033[31mFAIL\033[0m expected "%s" in: %s\n' "$1" "$2" >&2
    exit 1
  fi
}

enroll_phone() {  # usage: enroll_phone <name> <employee-uuid>
  "$DEVICE_SIM" keygen --name "$1" --employee "$2" --home "$WORK/phones" >/dev/null
  "$DEVICE_SIM" register --name "$1" --home "$WORK/phones" --user-qr "$USERQR"
}

new_token() {  # usage: new_token <name> [extra device-sim token args]
  local name="$1"; shift
  "$DEVICE_SIM" token --name "$name" --home "$WORK/phones" "$@"
}

kiosk() {  # usage: kiosk <args...>   (a private journal per run)
  "$KIOSK_SIM" "$@" --edge "$EDGE" --db "$WORK/kiosk.db" --kiosk-id BD-KIOSK-01
}

attempt_id() {  # extracts attempt=<uuid> from a kiosk scan line
  sed -n 's/.*attempt=\([0-9a-f-]*\).*/\1/p' <<<"$1"
}
