#!/usr/bin/env bash
# Happy path plus the refusals that protect it (ADR-12): enroll, scan, replay, long-lived, PII.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
require_stack

say "1. Enroll a phone (device binding): the private key never leaves the simulated phone"
enroll_phone demo1 "$EMP_ONLINE"

say "2. Kiosk pulls gates, device keys and revocations"
kiosk sync

say "3. Scan a fresh 30 s token at the entry gate"
T1="$(new_token demo1)"
OUT="$(kiosk scan --gate BD-GATE-01 "$T1" || true)"; note "$OUT"
expect "ALLOWED" "$OUT"
expect "mode=ONLINE" "$OUT"

say "4. The same QR used again (a screenshot) is refused, the gate stays closed"
OUT="$(kiosk scan --gate BD-GATE-02 "$T1" || true)"; note "$OUT"
expect "QR_ALREADY_USED" "$OUT"

say "5. A token that looks like a static QR (24 h) is refused"
OUT="$(kiosk scan --gate BD-GATE-02 "$(new_token demo1 --long-lived)" || true)"; note "$OUT"
expect "MALFORMED_TOKEN" "$OUT"

say "6. A token carrying a name (personal data) is refused"
OUT="$(kiosk scan --gate BD-GATE-02 "$(new_token demo1 --pii)" || true)"; note "$OUT"
expect "MALFORMED_TOKEN" "$OUT"

say "7. Two entries in a row are refused by anti-passback"
OUT="$(kiosk scan --gate BD-GATE-01 "$(new_token demo1)" || true)"; note "$OUT"
expect "ANTI_PASSBACK_VIOLATION" "$OUT"

say "Journal"
kiosk status
say "online demo passed"
