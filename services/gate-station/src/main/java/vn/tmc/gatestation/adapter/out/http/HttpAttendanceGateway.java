package vn.tmc.gatestation.adapter.out.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * HTTP client of the Attendance internal API (docs/api/internal-attendance.openapi.yaml, base path
 * /internal/v1). Request bodies are built field by field so this adapter, not a shared class, is
 * the place that knows the wire format. Any transport error or 5xx surfaces as a
 * RestClientException, which the use case treats as "Attendance is down".
 */
public class HttpAttendanceGateway implements IAttendanceGateway {
  private static final ParameterizedTypeReference<List<OfflineResultDto>> OFFLINE_RESULTS =
      new ParameterizedTypeReference<>() {};

  /** The part of Attendance's AttemptView this service reads. */
  private record AttemptDto(UUID attemptId, String status, ReasonCode reasonCode) {}

  private record OfflineResultDto(UUID attemptId, String outcome, String detail) {}

  private final RestClient http;

  public HttpAttendanceGateway(RestClient http) {
    this.http = http;
  }

  @Override
  public Optional<AttemptState> find(UUID attemptId) {
    try {
      var dto =
          http.get().uri("/internal/v1/attempts/{id}", attemptId).retrieve().body(AttemptDto.class);
      return Optional.ofNullable(dto).map(HttpAttendanceGateway::toState);
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    }
  }

  @Override
  public AttemptState recordAttempt(AttemptRecord r) {
    var body = new HashMap<String, Object>();
    body.put("attemptId", r.attemptId().toString());
    body.put("employeeUuid", r.employeeUuid().toString());
    body.put("gateId", r.gateId());
    body.put("factoryCode", r.factoryCode());
    body.put("direction", r.direction().name());
    body.put("jti", r.jti());
    body.put("source", r.source());
    body.put("scannedAt", r.scannedAt().toString());
    body.put("correlationId", r.correlationId());
    return post("/internal/v1/attempts", body);
  }

  @Override
  public AttemptState recordDenied(DenialRecord r) {
    var body = new HashMap<String, Object>();
    body.put("attemptId", r.attemptId().toString());
    body.put("employeeUuid", r.employeeUuid() == null ? null : r.employeeUuid().toString());
    body.put("gateId", r.gateId());
    body.put("factoryCode", r.factoryCode());
    body.put("direction", r.direction() == null ? null : r.direction().name());
    body.put("jti", r.jti());
    body.put("scannedAt", r.scannedAt().toString());
    body.put("reasonCode", r.reason().name());
    body.put("correlationId", r.correlationId());
    return post("/internal/v1/attempts/denied", body);
  }

  @Override
  public AttemptState markGateCommanded(UUID attemptId) {
    return post("/internal/v1/attempts/" + attemptId + "/gate-commanded", Map.of());
  }

  @Override
  public AttemptState recordGateResult(UUID attemptId, GateResult result, String correlationId) {
    var body = new HashMap<String, Object>();
    body.put("result", result.name());
    body.put("correlationId", correlationId);
    return post("/internal/v1/attempts/" + attemptId + "/gate-result", body);
  }

  @Override
  public List<OfflineItemResult> ingestOffline(String kioskId, List<OfflineItem> items) {
    var records =
        items.stream()
            .map(
                i -> {
                  var m = new HashMap<String, Object>();
                  m.put("attemptId", i.attemptId().toString());
                  m.put("journalSeq", i.journalSeq());
                  m.put(
                      "employeeUuid",
                      i.employeeUuid() == null ? null : i.employeeUuid().toString());
                  m.put("gateId", i.gateId());
                  m.put("factoryCode", i.factoryCode());
                  m.put("direction", i.direction() == null ? null : i.direction().name());
                  m.put("jti", i.jti());
                  m.put("scannedAt", i.scannedAt().toString());
                  m.put("localStatus", i.localStatus());
                  m.put("deniedReason", i.deniedReason() == null ? null : i.deniedReason().name());
                  return m;
                })
            .toList();
    var body = new HashMap<String, Object>();
    body.put("kioskId", kioskId);
    body.put("records", records);
    var results =
        http.post()
            .uri("/internal/v1/offline-batch")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(OFFLINE_RESULTS);
    if (results == null) {
      throw new IllegalStateException("attendance returned no result for the offline batch");
    }
    return results.stream()
        .map(
            r ->
                new OfflineItemResult(
                    r.attemptId(), OfflineOutcome.valueOf(r.outcome()), r.detail()))
        .toList();
  }

  private AttemptState post(String path, Object body) {
    var dto =
        http.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(AttemptDto.class);
    if (dto == null) {
      throw new IllegalStateException("attendance returned an empty answer for " + path);
    }
    return toState(dto);
  }

  private static AttemptState toState(AttemptDto dto) {
    State state;
    try {
      state = State.valueOf(dto.status());
    } catch (IllegalArgumentException | NullPointerException e) {
      state = State.OTHER; // VALIDATED, GATE_OPENED or a status added later
    }
    return new AttemptState(dto.attemptId(), state, dto.reasonCode());
  }
}
