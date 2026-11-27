package vn.tmc.gatestation.adapter.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import vn.tmc.gatestation.adapter.out.http.HttpTurnstileAdapter;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.CommandObservation;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.CommandResult;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.OpenGateRequest;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.Trigger;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.turnstilesim.TurnstileSimulatorApplication;

/** A-05: the edge adapter speaks to the actual turnstile simulator over HTTP. */
@Tag("integration")
@SpringBootTest(
    classes = TurnstileSimulatorApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TurnstileSimulatorContractIT {
  @LocalServerPort private int port;

  @Autowired private TestRestTemplate http;

  @AfterEach
  void clearFaults() {
    http.exchange("/admin/fault", HttpMethod.DELETE, null, String.class);
  }

  @Test
  void maps_all_five_fault_modes_and_resolves_both_ambiguous_timeouts() {
    // The first request initializes Spring's dispatcher. Exclude that startup cost from the
    // controller's 800 ms read budget, as the local stack does before serving scans.
    assertThat(
            http.getForEntity("/gates/TEST-WARM/status", String.class)
                .getStatusCode()
                .is2xxSuccessful())
        .isTrue();
    var adapter =
        new HttpTurnstileAdapter(
            "http://localhost:" + port, "secret", Duration.ofMillis(200), Duration.ofMillis(800));

    String normal = gate();
    assertThat(adapter.openGate(open(normal))).isEqualTo(CommandResult.OPENED);

    String jam = gate();
    fault(jam, "JAM", 0);
    assertThat(adapter.openGate(open(jam))).isEqualTo(CommandResult.ERROR);

    String offline = gate();
    fault(offline, "OFFLINE", 0);
    assertThat(adapter.openGate(open(offline))).isEqualTo(CommandResult.ERROR);

    String gatewayTimeout = gate();
    fault(gatewayTimeout, "GATEWAY_TIMEOUT", 0);
    assertThat(adapter.openGate(open(gatewayTimeout))).isEqualTo(CommandResult.TIMEOUT);
    assertEventuallyObserved(adapter, gatewayTimeout, CommandObservation.NOT_OPENED);

    String delayOpened = gate();
    fault(delayOpened, "DELAY_OPENED", 1200);
    assertThat(adapter.openGate(open(delayOpened))).isEqualTo(CommandResult.TIMEOUT);
    assertEventuallyObserved(adapter, delayOpened, CommandObservation.OPENED);

    String delayNotOpened = gate();
    fault(delayNotOpened, "DELAY_NOT_OPENED", 1200);
    assertThat(adapter.openGate(open(delayNotOpened))).isEqualTo(CommandResult.TIMEOUT);
    assertEventuallyObserved(adapter, delayNotOpened, CommandObservation.NOT_OPENED);
  }

  private static void assertEventuallyObserved(
      HttpTurnstileAdapter adapter, String gate, CommandObservation expected) {
    await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(() -> assertThat(adapter.queryCommand(gate, key(gate))).isEqualTo(expected));
  }

  private void fault(String gate, String mode, int delayMs) {
    assertThat(
            http.postForEntity(
                    "/admin/fault",
                    Map.of("gateId", gate, "mode", mode, "delayMs", delayMs),
                    String.class)
                .getStatusCode()
                .is2xxSuccessful())
        .isTrue();
  }

  private static String gate() {
    return "TEST-" + UUID.randomUUID();
  }

  private static OpenGateRequest open(String gate) {
    return new OpenGateRequest(gate, Direction.ENTRY, key(gate), Trigger.QR_SCAN, null, "a05");
  }

  private static String key(String gate) {
    return "key-" + gate;
  }
}
