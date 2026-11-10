package vn.tmc.kiosksim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP client with the 2 s kiosk budget. 4xx answers are business results, I/O problems are
 * "unreachable".
 */
public class HttpEdgeClient implements EdgeClient {
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build();
  private final ObjectMapper json =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final String baseUrl;
  private final Duration timeout;

  public HttpEdgeClient(String baseUrl, Duration timeout) {
    this.baseUrl = baseUrl;
    this.timeout = timeout;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ScanDto(
      @JsonProperty("attempt_id") UUID attemptId,
      String outcome,
      @JsonProperty("reason_code") String reasonCode,
      String direction,
      boolean duplicate) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record GateDto(String gateId, String mode) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record KeyDto(
      String deviceId, String employeeUuid, String publicKeyX509Base64, boolean revoked) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record BundleDto(List<GateDto> gates, List<KeyDto> deviceKeys) {}

  @Override
  public ScanResponse scan(
      UUID attemptId,
      String gateId,
      String tokenOrHash,
      Instant scannedAt,
      long journalSeq,
      String kioskId)
      throws EdgeUnreachableException {
    var body = new LinkedHashMap<String, Object>();
    body.put("gate_id", gateId);
    body.put("qr_token", tokenOrHash);
    body.put("scanned_at", scannedAt.toString());
    body.put("journal_seq", journalSeq);
    var response =
        send(
            HttpRequest.newBuilder(URI.create(baseUrl + "/kiosk/v1/scans"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", attemptId.toString())
                .header("X-Kiosk-Id", kioskId)
                .header("X-Correlation-Id", "corr-" + attemptId)
                .POST(HttpRequest.BodyPublishers.ofString(write(body))));
    if (response.statusCode() >= 500) {
      throw new EdgeUnreachableException("edge answered HTTP " + response.statusCode(), null);
    }
    try {
      var dto = json.readValue(response.body(), ScanDto.class);
      return new ScanResponse(
          dto.attemptId(), dto.outcome(), dto.reasonCode(), dto.direction(), dto.duplicate());
    } catch (IOException e) {
      throw new EdgeUnreachableException("unreadable edge answer", e);
    }
  }

  @Override
  public SyncBundle sync(Instant since) throws EdgeUnreachableException {
    var response =
        send(
            HttpRequest.newBuilder(URI.create(baseUrl + "/kiosk/v1/sync-bundle?since=" + since))
                .GET());
    if (response.statusCode() != 200) {
      throw new EdgeUnreachableException("sync answered HTTP " + response.statusCode(), null);
    }
    try {
      var dto = json.readValue(response.body(), BundleDto.class);
      return new SyncBundle(
          dto.gates() == null
              ? List.of()
              : dto.gates().stream().map(g -> new Journal.Gate(g.gateId(), g.mode())).toList(),
          dto.deviceKeys() == null
              ? List.of()
              : dto.deviceKeys().stream()
                  .map(
                      k ->
                          new Journal.DeviceKey(
                              k.deviceId(), k.employeeUuid(), k.publicKeyX509Base64(), k.revoked()))
                  .toList());
    } catch (IOException e) {
      throw new EdgeUnreachableException("unreadable sync bundle", e);
    }
  }

  @Override
  public List<OfflineResult> offlineBatch(String kioskId, List<OfflineLine> lines)
      throws EdgeUnreachableException {
    var records =
        lines.stream()
            .map(
                l -> {
                  Map<String, Object> m = new LinkedHashMap<>();
                  m.put("attemptId", l.attemptId());
                  m.put("journalSeq", l.journalSeq());
                  m.put("employeeUuid", l.employeeUuid());
                  m.put("gateId", l.gateId());
                  m.put("direction", l.direction());
                  m.put("jti", l.jti());
                  m.put("scannedAt", l.scannedAt().toString());
                  m.put("localStatus", l.localStatus());
                  m.put("deniedReason", l.deniedReason());
                  return m;
                })
            .toList();
    var response =
        send(
            HttpRequest.newBuilder(URI.create(baseUrl + "/kiosk/v1/scans/offline-batch"))
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        write(Map.of("kioskId", kioskId, "records", records)))));
    if (response.statusCode() != 200) {
      throw new EdgeUnreachableException(
          "offline batch answered HTTP " + response.statusCode(), null);
    }
    try {
      var arr = json.readValue(response.body(), OfflineResult[].class);
      return List.of(arr);
    } catch (IOException e) {
      throw new EdgeUnreachableException("unreadable batch answer", e);
    }
  }

  private HttpResponse<String> send(HttpRequest.Builder builder) throws EdgeUnreachableException {
    try {
      return http.send(builder.timeout(timeout).build(), HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new EdgeUnreachableException("edge not reachable: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EdgeUnreachableException("interrupted", e);
    }
  }

  private String write(Object o) {
    try {
      return json.writeValueAsString(o);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
