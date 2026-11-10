#!/usr/bin/env bash
# ADR-11 P6 and ADR-12 R2: the kiosk cannot reach the edge. It verifies alone, refuses a replay, and
# hands its journal over when the network is back.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
require_stack

say "1. Enroll and sync while the network is fine (the kiosk needs the keys cached)"
enroll_phone demo2 "$EMP_OFFLINE"
kiosk sync

say "2. Cut the network (forced offline)"
kiosk offline on

say "3. Scan offline: verified locally, opened through the local gate link, journaled first"
T="$(new_token demo2)"
OUT="$(kiosk scan --gate BD-GATE-03 "$T" || true)"; note "$OUT"
expect "ALLOWED" "$OUT"
expect "mode=OFFLINE" "$OUT"
expect "LOCAL GATE LINK" "$OUT"

say "4. Replaying the same QR offline is refused by the local replay cache"
OUT="$(kiosk scan --gate BD-GATE-03 "$T" || true)"; note "$OUT"
expect "QR_ALREADY_USED" "$OUT"

say "5. Journal before hand over (handed_over=false)"
kiosk status

say "6. Network is back: hand the journal over in order"
kiosk offline off
OUT="$(kiosk flush || true)"; note "$OUT"
expect "handedOver=2" "$OUT"

say "7. Flush again: nothing is sent twice"
OUT="$(kiosk flush || true)"; note "$OUT"
expect "handedOver=0" "$OUT"

say "Journal after hand over"
kiosk status
say "offline demo passed"
