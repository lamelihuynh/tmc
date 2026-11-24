package vn.tmc.attendance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.tmc.attendance.application.port.out.IAttemptRepository;
import vn.tmc.attendance.application.port.out.IOutboxRepository;
import vn.tmc.attendance.bootstrap.AttendanceApplication;
import vn.tmc.attendance.bootstrap.AttendanceMetricsConfig;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.kernel.Ids;

/** B-08: verifies both metrics are registered in MeterRegistry and exposed at /actuator/metrics. */
@SpringBootTest(classes = AttendanceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"inmem", "test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ActuatorMetricsApiTest {
  @Autowired MockMvc mvc;
  @Autowired MeterRegistry registry;
  @Autowired IOutboxRepository outbox;
  @Autowired IAttemptRepository attempts;
  @Autowired IClock clock;

  @Test
  void should_expose_operational_metrics_via_actuator_and_reflect_current_data() throws Exception {
    var now = clock.now();

    outbox.add(
        new EventEnvelope(
            Ids.uuidV7(),
            "AccessAttemptRecorded",
            1,
            Ids.uuidV7(),
            1,
            now.minus(Duration.ofMinutes(2)),
            "corr-fresh",
            "attendance",
            "BD",
            "BD-GATE-01",
            null));
    outbox.add(
        new EventEnvelope(
            Ids.uuidV7(),
            "AccessAttemptRecorded",
            1,
            Ids.uuidV7(),
            1,
            now.minus(Duration.ofMinutes(7)),
            "corr-stale",
            "attendance",
            "BD",
            "BD-GATE-01",
            null));
    var publishedStale =
        new EventEnvelope(
            Ids.uuidV7(),
            "AccessAttemptRecorded",
            1,
            Ids.uuidV7(),
            1,
            now.minus(Duration.ofMinutes(10)),
            "corr-pub",
            "attendance",
            "BD",
            "BD-GATE-01",
            null);
    outbox.add(publishedStale);
    outbox.markPublished(publishedStale.messageId());

    var unknown =
        AccessAttempt.validated(
            Ids.uuidV7(),
            UUID.randomUUID(),
            "BD-GATE-01",
            "BD",
            Direction.ENTRY,
            "jti-act-1",
            AttemptSource.ONLINE,
            now,
            now);
    unknown.reserve(now);
    unknown.commandGate(now);
    unknown.gateUnknown(now);
    attempts.save(unknown);

    assertThat(registry.get(AttendanceMetricsConfig.OUTBOX_STALE_METRIC).gauge().value())
        .isEqualTo(1.0);
    assertThat(registry.get(AttendanceMetricsConfig.GATE_UNKNOWN_METRIC).gauge().value())
        .isEqualTo(1.0);

    mvc.perform(get("/actuator/metrics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.names", hasItem(AttendanceMetricsConfig.OUTBOX_STALE_METRIC)))
        .andExpect(jsonPath("$.names", hasItem(AttendanceMetricsConfig.GATE_UNKNOWN_METRIC)));

    mvc.perform(get("/actuator/metrics/" + AttendanceMetricsConfig.OUTBOX_STALE_METRIC))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.measurements[0].value").value(1.0));

    mvc.perform(get("/actuator/metrics/" + AttendanceMetricsConfig.GATE_UNKNOWN_METRIC))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.measurements[0].value").value(1.0));

    // Sensitive management endpoints stay unexposed
    mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
  }
}
