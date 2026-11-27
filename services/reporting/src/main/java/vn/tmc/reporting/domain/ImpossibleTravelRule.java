package vn.tmc.reporting.domain;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * ADR-12 R4: the same employee enters one factory less than the travel time after a pass at another
 * factory (default 60 minutes). Order of arrival does not matter: the gap is taken either way.
 */
public final class ImpossibleTravelRule implements FraudRule {
  public static final String NAME = "IMPOSSIBLE_TRAVEL";
  public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(60);

  private final Duration window;

  public ImpossibleTravelRule(Duration window) {
    this.window = Objects.requireNonNull(window, "window");
  }

  @Override
  public Optional<Finding> evaluate(Presence previous, Presence current) {
    if (previous == null
        || !previous.employeeUuid().equals(current.employeeUuid())
        || previous.factoryCode().equals(current.factoryCode())
        || !current.isInside()) {
      return Optional.empty();
    }
    var gap = Duration.between(previous.at(), current.at()).abs();
    if (gap.compareTo(window) >= 0) {
      return Optional.empty();
    }
    return Optional.of(
        new Finding(
            NAME,
            previous.factoryCode()
                + " "
                + previous.direction()
                + " at "
                + previous.at()
                + ", "
                + current.factoryCode()
                + " ENTRY at "
                + current.at()
                + ": "
                + gap.toMinutes()
                + " min apart, travel needs at least "
                + window.toMinutes()
                + " min"));
  }
}
