package vn.tmc.reporting.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A pass flagged by a fraud rule, kept for HR review (UC-09). The pass itself stays in the read
 * model unchanged. One alert per pass and rule.
 *
 * @param factoryCode factory of the flagged pass
 * @param otherFactory factory of the earlier pass it conflicts with
 */
public record FraudAlert(
    UUID attemptId,
    UUID employeeUuid,
    String factoryCode,
    String otherFactory,
    String rule,
    String detail,
    Instant at) {}
