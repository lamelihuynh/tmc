package vn.tmc.attendance.adapter.out.http;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import vn.tmc.attendance.application.port.out.IGateStatusQuery;
import vn.tmc.attendance.domain.AccessAttempt;

/**
 * Asks Gate Station: GET /internal/v1/attempts/{attemptId}/gate-observation?gateId=... (contract in
 * docs/api/kiosk-edge.openapi.yaml, internal section). Any failure is UNDETERMINED, never a guess:
 * the attempt stays GATE_UNKNOWN and is asked about again on the next reconcile run.
 */
public class HttpGateStatusQuery implements IGateStatusQuery {
  private static final Logger LOG = LoggerFactory.getLogger(HttpGateStatusQuery.class);
  private static final ParameterizedTypeReference<Map<String, String>> BODY =
      new ParameterizedTypeReference<>() {};

  private final RestClient http;

  public HttpGateStatusQuery(RestClient http) {
    this.http = http;
  }

  @Override
  public GateObservation observe(AccessAttempt attempt) {
    try {
      var body =
          http.get()
              .uri(
                  uri ->
                      uri.path("/internal/v1/attempts/{attemptId}/gate-observation")
                          .queryParam("gateId", attempt.gateId())
                          .build(attempt.attemptId()))
              .retrieve()
              .body(BODY);
      if (body == null || body.get("observation") == null) {
        return GateObservation.UNDETERMINED;
      }
      return GateObservation.valueOf(body.get("observation"));
    } catch (RuntimeException e) {
      LOG.warn("gate observation failed for attempt {}: {}", attempt.attemptId(), e.toString());
      return GateObservation.UNDETERMINED;
    }
  }
}
