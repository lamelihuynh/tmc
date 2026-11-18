package vn.tmc.gatestation.adapter.out.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort;

/**
 * Anti-corruption layer in front of the Turnstile Control System (document.md table 16.3.2,
 * docs/api/turnstile.openapi.yaml). Mapping:
 *
 * <ul>
 *   <li>200 with result UNLOCKED: OPENED. A 200 that says anything else is not trusted: ERROR.
 *   <li>409, 503 and other definite refusals: ERROR (nothing opened, the token can be released).
 *   <li>504, a read timeout, or any failure after the request may have left: TIMEOUT (unknown).
 *   <li>Could not connect at all: nothing was sent, so ONE retry is safe; then TIMEOUT.
 * </ul>
 *
 * A request that may have reached the controller is never sent twice. The Idempotency-Key {@code
 * cmd-ts-<commandKey>} additionally lets the controller ignore a duplicate.
 */
public class HttpTurnstileAdapter implements ITurnstileControlPort {
  private static final Logger LOG = LoggerFactory.getLogger(HttpTurnstileAdapter.class);
  private static final String IDEMPOTENCY_PREFIX = "cmd-ts-";

  private final String baseUrl;
  private final String token;
  private final Duration readTimeout;
  private final HttpClient http;
  private final ObjectMapper mapper = new ObjectMapper();

  public HttpTurnstileAdapter(
      String baseUrl, String token, Duration connectTimeout, Duration readTimeout) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.token = token;
    this.readTimeout = readTimeout;
    this.http =
        HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .version(HttpClient.Version.HTTP_1_1)
            .build();
  }

  @Override
  public CommandResult openGate(OpenGateRequest r) {
    HttpRequest request;
    try {
      var body = new LinkedHashMap<String, Object>();
      body.put("direction", r.direction().name());
      body.put("trigger_source", r.trigger().name());
      body.put("operator_id", r.operatorId() == null ? null : r.operatorId().toString());
      body.put("command_key", r.commandKey());
      var builder =
          HttpRequest.newBuilder(URI.create(baseUrl + "/gates/" + r.hardwareGateId() + "/open"))
              .timeout(readTimeout)
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + token)
              .header("Idempotency-Key", IDEMPOTENCY_PREFIX + r.commandKey())
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      mapper.writeValueAsString(body), StandardCharsets.UTF_8));
      if (r.correlationId() != null) {
        builder.header("X-Correlation-Id", r.correlationId());
      }
      request = builder.build();
    } catch (Exception e) {
      LOG.error("cannot build the open command for gate {}", r.hardwareGateId(), e);
      return CommandResult.ERROR; // nothing was sent
    }

    for (int attempt = 1; ; attempt++) {
      try {
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return mapOpenResponse(response);
      } catch (HttpConnectTimeoutException | ConnectException e) {
        // The connection was never established, so the command did not leave: retry once.
        if (attempt >= 2) {
          LOG.warn("turnstile unreachable for gate {}: {}", r.hardwareGateId(), e.toString());
          return CommandResult.TIMEOUT;
        }
      } catch (IOException e) {
        // Includes the read timeout. The command may have arrived: unknown, never resend.
        LOG.warn("no answer from turnstile for gate {}: {}", r.hardwareGateId(), e.toString());
        return CommandResult.TIMEOUT;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return CommandResult.TIMEOUT;
      }
    }
  }

  private CommandResult mapOpenResponse(HttpResponse<String> response) {
    int status = response.statusCode();
    if (status == 200) {
      return "UNLOCKED".equals(field(response.body(), "result"))
          ? CommandResult.OPENED
          : CommandResult.ERROR;
    }
    if (status == 504 || status == 408) {
      return CommandResult.TIMEOUT;
    }
    return CommandResult.ERROR;
  }

  @Override
  public CommandObservation queryCommand(String hardwareGateId, String commandKey) {
    try {
      var request =
          HttpRequest.newBuilder(URI.create(baseUrl + "/gates/" + hardwareGateId + "/status"))
              .timeout(readTimeout)
              .header("Authorization", "Bearer " + token)
              .GET()
              .build();
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return CommandObservation.UNDETERMINED;
      }
      // The controller only reports its LAST command. If that is not ours we cannot tell.
      if (!commandKey.equals(field(response.body(), "last_command_key"))) {
        return CommandObservation.UNDETERMINED;
      }
      return "UNLOCKED".equals(field(response.body(), "last_command_result"))
          ? CommandObservation.OPENED
          : CommandObservation.NOT_OPENED;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return CommandObservation.UNDETERMINED;
    } catch (Exception e) {
      LOG.warn("turnstile status failed for gate {}: {}", hardwareGateId, e.toString());
      return CommandObservation.UNDETERMINED;
    }
  }

  private String field(String json, String name) {
    try {
      JsonNode node = mapper.readTree(json).get(name);
      return node == null || node.isNull() ? null : node.asText();
    } catch (Exception e) {
      return null;
    }
  }
}
