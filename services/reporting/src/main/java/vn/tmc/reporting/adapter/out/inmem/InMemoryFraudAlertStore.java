package vn.tmc.reporting.adapter.out.inmem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import vn.tmc.reporting.application.port.out.IFraudAlertStore;
import vn.tmc.reporting.domain.FraudAlert;

/** Process-memory fraud alerts for the inmem and local profiles. */
public class InMemoryFraudAlertStore implements IFraudAlertStore {
  private final List<FraudAlert> alerts = new ArrayList<>();

  @Override
  public synchronized void record(FraudAlert alert) {
    boolean known =
        alerts.stream()
            .anyMatch(
                a -> a.attemptId().equals(alert.attemptId()) && a.rule().equals(alert.rule()));
    if (!known) {
      alerts.add(alert);
    }
  }

  @Override
  public synchronized List<FraudAlert> involving(String factoryCode, int limit) {
    return alerts.stream()
        .filter(a -> a.factoryCode().equals(factoryCode) || a.otherFactory().equals(factoryCode))
        .sorted(Comparator.comparing(FraudAlert::at).reversed())
        .limit(limit)
        .toList();
  }
}
