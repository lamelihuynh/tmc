package vn.tmc.gatestation.adapter.out.http;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import vn.tmc.gatestation.application.port.out.IQRValidation;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Calls the QR Verifier of User/QR: POST /internal/v1/qr/verify {"token": "..."}
 * (docs/api/user-qr.openapi.yaml). A 4xx means the verifier refused the input itself (for example a
 * blank token), which is a denial. A transport error or 5xx propagates: the scan answers 503 and
 * the kiosk falls back to its offline verifier. It is never turned into an approval (DP-04).
 */
public class HttpQrValidationClient implements IQRValidation {
  private final RestClient http;

  public HttpQrValidationClient(RestClient http) {
    this.http = http;
  }

  @Override
  public Verdict verify(String compactToken) {
    try {
      var verdict =
          http.post()
              .uri("/internal/v1/qr/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .body(Map.of("token", compactToken == null ? "" : compactToken))
              .retrieve()
              .body(Verdict.class);
      if (verdict == null) {
        throw new IllegalStateException("QR verifier returned an empty answer");
      }
      return verdict;
    } catch (HttpClientErrorException e) {
      return Verdict.denied(ReasonCode.MALFORMED_TOKEN);
    }
  }
}
