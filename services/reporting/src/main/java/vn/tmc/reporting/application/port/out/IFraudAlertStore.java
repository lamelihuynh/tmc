package vn.tmc.reporting.application.port.out;

import java.util.List;
import vn.tmc.reporting.domain.FraudAlert;

/**
 * Driven port: fraud alerts for HR review. Append-only; a second alert for the same pass and rule
 * is ignored.
 */
public interface IFraudAlertStore {
  void record(FraudAlert alert);

  /** Alerts that involve the factory (either side), newest first. */
  List<FraudAlert> involving(String factoryCode, int limit);
}
