package vn.tmc.identity.application.port.out;

import java.util.UUID;

/**
 * Driven port: security-relevant facts about logins (NFR-02, C-08), delivered to audit-admin as
 * {@code identity.AuditRecorded}. Identifiers and short reason codes only: never a username,
 * password, MFA code or token (ADR-09).
 *
 * @param actor who did it, when known (the account subject after a successful login)
 * @param target what it is about (the account subject), null when no account matched
 * @param result OK or a short reason code without personal data
 */
@FunctionalInterface
public interface IAuditLog {
  void record(String action, UUID actor, String target, String result);
}
