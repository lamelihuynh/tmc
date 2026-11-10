#!/usr/bin/env bash
# ADR-11 P3: the gate opens but its answer is lost. The system must not open a second time, must say
# "unknown", and must resolve it later by asking the gate what it did.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
require_stack

say "1. Enroll, sync"
enroll_phone demo3 "$EMP_TIMEOUT"
kiosk sync

passes() { curl -sf "$TURNSTILE/gates/BD-HW-03/status" | sed -n 's/.*"pass_count":\([0-9]*\).*/\1/p'; }
BEFORE="$(passes)"
note "unlocks so far on this gate: $BEFORE"

say "2. Make the turnstile open but answer after 1.5 s (our read timeout is 0.8 s)"
curl -sf -X POST "$TURNSTILE/admin/fault" -H 'Content-Type: application/json' \
  -d '{"gateId":"BD-HW-03","mode":"DELAY_OPENED","delayMs":1500}' >/dev/null

say "3. Scan: the answer is unknown, the kiosk is told to wait (HTTP 202), nothing is opened twice"
OUT="$(kiosk scan --gate BD-GATE-03 "$(new_token demo3)" || true)"; note "$OUT"
expect "PENDING" "$OUT"
ATTEMPT="$(attempt_id "$OUT")"
note "attempt id: $ATTEMPT"

say "4. Remove the fault. The turnstile saw exactly ONE new unlock"
curl -sf -X DELETE "$TURNSTILE/admin/fault" >/dev/null
note "unlocks now: $(passes)"
expect "$((BEFORE + 1))" "$(passes)"

say "5. Reconcile asks the gate what happened and commits the attendance (waits up to 30 s)"
for i in $(seq 1 15); do
  STATE="$(curl -sf "$ATTENDANCE/internal/v1/attempts/$ATTEMPT" || true)"
  [[ "$STATE" == *COMMITTED* ]] && break
  sleep 2
done
note "$STATE"
expect "COMMITTED" "$STATE"

say "6. Reconcile asked the gate, it did NOT send another unlock"
expect "$((BEFORE + 1))" "$(passes)"
say "timeout demo passed"
