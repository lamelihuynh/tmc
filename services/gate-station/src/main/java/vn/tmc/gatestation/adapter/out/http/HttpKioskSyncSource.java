package vn.tmc.gatestation.adapter.out.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.client.RestClient;
import vn.tmc.gatestation.application.port.in.IKioskSupport.DeviceKey;
import vn.tmc.gatestation.application.port.out.IKioskSyncSource;
import vn.tmc.gatestation.domain.EmployeeAccess;

/**
 * Reads the delta from User/QR: GET /internal/v1/sync-bundle?since=...
 * (docs/api/user-qr.openapi.yaml). Unknown fields are ignored and a bundle without {@code
 * employees} (older User/QR) means none. If User/QR is down the error propagates and callers keep
 * what they already have.
 */
public class HttpKioskSyncSource implements IKioskSyncSource {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record BundleDto(Instant generatedAt, List<DeviceKey> deviceKeys, List<EmployeeDto> employees) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record EmployeeDto(UUID employeeUuid, String status, Set<String> factories) {
    EmployeeAccess toAccess() {
      return new EmployeeAccess(employeeUuid, "ACTIVE".equals(status), factories);
    }
  }

  private final RestClient http;

  public HttpKioskSyncSource(RestClient http) {
    this.http = http;
  }

  @Override
  public List<DeviceKey> deviceKeysChangedSince(Instant since) {
    return changedSince(since).deviceKeys();
  }

  @Override
  public Delta changedSince(Instant since) {
    var bundle =
        http.get()
            .uri(
                uri ->
                    uri.path("/internal/v1/sync-bundle")
                        .queryParam("since", since.toString())
                        .build())
            .retrieve()
            .body(BundleDto.class);
    if (bundle == null) {
      return new Delta(null, List.of(), List.of());
    }
    var employees =
        bundle.employees() == null
            ? List.<EmployeeAccess>of()
            : bundle.employees().stream()
                .filter(e -> e.employeeUuid() != null)
                .map(EmployeeDto::toAccess)
                .toList();
    return new Delta(bundle.generatedAt(), bundle.deviceKeys(), employees);
  }
}
