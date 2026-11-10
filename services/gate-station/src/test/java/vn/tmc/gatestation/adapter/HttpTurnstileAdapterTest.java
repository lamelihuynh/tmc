package vn.tmc.gatestation.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.tmc.gatestation.adapter.out.http.HttpTurnstileAdapter;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.CommandObservation;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.CommandResult;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.OpenGateRequest;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort.Trigger;
import vn.tmc.gatestation.domain.Direction;

/**
 * Contract test of the anti-corruption layer against a real HTTP server (document.md table 16.3.2).
 */
class HttpTurnstileAdapterTest {
  private HttpServer server;
  private HttpTurnstileAdapter adapter;
  private final Map<String, String> seenHeaders = new ConcurrentHashMap<>();
  private final AtomicInteger openHits = new AtomicInteger();

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/gates/OK/open",
        ex -> {
          openHits.incrementAndGet();
          seenHeaders.put(
              "idem", String.valueOf(ex.getRequestHeaders().getFirst("Idempotency-Key")));
          seenHeaders.put("auth", String.valueOf(ex.getRequestHeaders().getFirst("Authorization")));
          seenHeaders.put(
              "body", new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          respond(ex, 200, "{\"result\":\"UNLOCKED\"}");
        });
    server.createContext(
        "/gates/JAM/open", ex -> respond(ex, 409, "{\"result\":\"MECHANICAL_ARM_JAMMED\"}"));
    server.createContext("/gates/OFFLINE/open", ex -> respond(ex, 503, "{}"));
    server.createContext("/gates/GW/open", ex -> respond(ex, 504, "{}"));
    server.createContext(
        "/gates/ODD/open", ex -> respond(ex, 200, "{\"result\":\"SOMETHING_ELSE\"}"));
    server.createContext(
        "/gates/SLOW/open",
        ex -> {
          openHits.incrementAndGet();
          try {
            Thread.sleep(1500);
          } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
          }
          respond(ex, 200, "{\"result\":\"UNLOCKED\"}");
        });
    server.createContext(
        "/gates/OK/status",
        ex ->
            respond(
                ex,
                200,
                "{\"state\":\"ONLINE\",\"last_command_key\":\"k1\",\"last_command_result\":\"UNLOCKED\"}"));
    server.createContext(
        "/gates/NOPE/status",
        ex ->
            respond(
                ex,
                200,
                "{\"state\":\"ONLINE\",\"last_command_key\":\"k1\",\"last_command_result\":\"JAMMED\"}"));
    server.start();
    adapter = adapterFor(server.getAddress().getPort());
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private static HttpTurnstileAdapter adapterFor(int port) {
    return new HttpTurnstileAdapter(
        "http://127.0.0.1:" + port, "secret", Duration.ofMillis(200), Duration.ofMillis(800));
  }

  private static void respond(HttpExchange ex, int status, String body) throws IOException {
    var bytes = body.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().add("Content-Type", "application/json");
    ex.sendResponseHeaders(status, bytes.length);
    try (var out = ex.getResponseBody()) {
      out.write(bytes);
    }
  }

  private OpenGateRequest open(String gate) {
    return new OpenGateRequest(gate, Direction.ENTRY, "key-123", Trigger.QR_SCAN, null, "corr");
  }

  @Test
  void should_map_200_unlocked_to_opened_and_send_idempotency_and_auth_headers() {
    assertThat(adapter.openGate(open("OK"))).isEqualTo(CommandResult.OPENED);

    assertThat(seenHeaders.get("idem")).isEqualTo("cmd-ts-key-123");
    assertThat(seenHeaders.get("auth")).isEqualTo("Bearer secret");
    assertThat(seenHeaders.get("body")).contains("\"direction\":\"ENTRY\"").contains("QR_SCAN");
  }

  @Test
  void should_map_409_and_503_to_a_definite_error() {
    assertThat(adapter.openGate(open("JAM"))).isEqualTo(CommandResult.ERROR);
    assertThat(adapter.openGate(open("OFFLINE"))).isEqualTo(CommandResult.ERROR);
  }

  @Test
  void should_not_trust_a_200_that_does_not_say_unlocked() {
    assertThat(adapter.openGate(open("ODD"))).isEqualTo(CommandResult.ERROR);
  }

  @Test
  void should_map_504_to_timeout_which_means_unknown() {
    assertThat(adapter.openGate(open("GW"))).isEqualTo(CommandResult.TIMEOUT);
  }

  @Test
  void should_map_a_read_timeout_to_timeout_and_send_the_command_only_once() {
    assertThat(adapter.openGate(open("SLOW"))).isEqualTo(CommandResult.TIMEOUT);
    // a slow answer is NOT a connect failure, so it must never be retried (the gate may have
    // opened)
    assertThat(openHits.get()).isEqualTo(1);
  }

  @Test
  void should_report_timeout_after_one_retry_when_the_controller_cannot_be_reached()
      throws IOException {
    int closedPort;
    try (var s = new ServerSocket(0)) {
      closedPort = s.getLocalPort();
    }

    assertThat(adapterFor(closedPort).openGate(open("OK"))).isEqualTo(CommandResult.TIMEOUT);
  }

  @Test
  void should_resolve_a_command_through_the_status_endpoint() {
    assertThat(adapter.queryCommand("OK", "k1")).isEqualTo(CommandObservation.OPENED);
    assertThat(adapter.queryCommand("NOPE", "k1")).isEqualTo(CommandObservation.NOT_OPENED);
    assertThat(adapter.queryCommand("OK", "another-key"))
        .isEqualTo(CommandObservation.UNDETERMINED);
  }

  @Test
  void should_report_undetermined_when_the_status_endpoint_is_unreachable() throws IOException {
    int closedPort;
    try (var s = new ServerSocket(0)) {
      closedPort = s.getLocalPort();
    }

    assertThat(adapterFor(closedPort).queryCommand("OK", "k1"))
        .isEqualTo(CommandObservation.UNDETERMINED);
  }
}
