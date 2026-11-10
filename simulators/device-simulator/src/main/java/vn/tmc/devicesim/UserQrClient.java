package vn.tmc.devicesim;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Registers the public key with User/QR as the signed-in employee (headers stand in for the gateway
 * JWT).
 */
public class UserQrClient {
  private static final Pattern DEVICE_ID = Pattern.compile("\"deviceId\"\\s*:\\s*\"([^\"]+)\"");
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
  private final String baseUrl;

  public UserQrClient(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String register(UUID employeeUuid, String publicKeyBase64) throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create(baseUrl + "/self-service/v1/devices"))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .header("X-Subject", employeeUuid.toString())
            .header("X-Roles", "EMPLOYEE")
            .POST(
                HttpRequest.BodyPublishers.ofString("{\"publicKey\":\"" + publicKeyBase64 + "\"}"))
            .build();
    var response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 201) {
      throw new IllegalStateException(
          "register failed: HTTP " + response.statusCode() + " " + response.body());
    }
    var m = DEVICE_ID.matcher(response.body());
    if (!m.find()) {
      throw new IllegalStateException("no deviceId in response: " + response.body());
    }
    return m.group(1);
  }
}
