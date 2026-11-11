package vn.tmc.gatestation.adapter.out.http;

import java.time.Instant;
import java.util.List;
import org.springframework.web.client.RestClient;
import vn.tmc.gatestation.application.port.in.IKioskSupport.DeviceKey;
import vn.tmc.gatestation.application.port.out.IKioskSyncSource;

/**
 * Reads the device-key delta from User/QR: GET /internal/v1/sync-bundle?since=...
 * (docs/api/user-qr.openapi.yaml). If User/QR is down the error propagates and the kiosk keeps the
 * keys it already has.
 */
public class HttpKioskSyncSource implements IKioskSyncSource {
  /** The part of User/QR's bundle this service forwards to kiosks. */
  private record BundleDto(Instant generatedAt, List<DeviceKey> deviceKeys) {}

  private final RestClient http;

  public HttpKioskSyncSource(RestClient http) {
    this.http = http;
  }

  @Override
  public List<DeviceKey> deviceKeysChangedSince(Instant since) {
    var bundle =
        http.get()
            .uri(
                uri ->
                    uri.path("/internal/v1/sync-bundle")
                        .queryParam("since", since.toString())
                        .build())
            .retrieve()
            .body(BundleDto.class);
    return bundle == null || bundle.deviceKeys() == null ? List.of() : bundle.deviceKeys();
  }
}
