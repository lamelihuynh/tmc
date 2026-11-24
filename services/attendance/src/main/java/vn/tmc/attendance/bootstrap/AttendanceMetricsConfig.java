package vn.tmc.attendance.bootstrap;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.domain.AttemptStatus;
import vn.tmc.platform.kernel.IClock;

/**
 * Operational metrics exposed via Micrometer / Actuator (B-08, AC-11). Gauges read live state from
 * repositories so values reflect additions, publishes, and reconciliations immediately.
 */
@Configuration
public class AttendanceMetricsConfig {
  public static final String OUTBOX_STALE_METRIC = "tmc.attendance.outbox.stale";
  public static final String GATE_UNKNOWN_METRIC = "tmc.attendance.attempts.gate_unknown";
  public static final Duration OUTBOX_STALE_THRESHOLD = Duration.ofMinutes(5);

  @Bean
  MeterBinder attendanceMetrics(
      IOutboxRepository outbox, IAttemptRepository attempts, IClock clock) {
    return registry -> bind(registry, outbox, attempts, clock);
  }

  static void bind(
      MeterRegistry registry, IOutboxRepository outbox, IAttemptRepository attempts, IClock clock) {
    Gauge.builder(
            OUTBOX_STALE_METRIC,
            () -> outbox.countPendingOlderThan(clock.now().minus(OUTBOX_STALE_THRESHOLD)))
        .description("Number of PENDING outbox events older than 5 minutes")
        .baseUnit("events")
        .register(registry);

    Gauge.builder(GATE_UNKNOWN_METRIC, () -> attempts.countByStatus(AttemptStatus.GATE_UNKNOWN))
        .description("Number of scan attempts currently in GATE_UNKNOWN status")
        .baseUnit("attempts")
        .register(registry);
  }
}
