package vn.tmc.hrintegration.adapter.out.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort;

public class HttpHrPayrollAdapter implements IHrPayrollPort {
  private static final ZoneId PAYROLL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

  private final HttpClient client;
  private final ObjectMapper json;
  private final URI batchesUri;
  private final String bearerToken;

  public HttpHrPayrollAdapter(URI baseUri, String bearerToken, ObjectMapper json) {
    this.client = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
    this.json = json;
    this.batchesUri = baseUri.resolve("/api/v1/attendance-batches");
    this.bearerToken = bearerToken;
  }

  @Override
  public SubmitResult submit(Submission submission) {
    String batchId = submission.batchId().toString();
    String period = YearMonth.from(submission.scannedAt().atZone(PAYROLL_ZONE)).toString();

    var record =
        Map.of(
            "event_id", submission.eventId().toString(),
            "employee_uuid", submission.employeeUuid().toString(),
            "direction", submission.direction(),
            "occurred_at", submission.scannedAt().toString());

    var batch =
        Map.of(
            "batch_id", batchId,
            "factory_code", submission.factoryCode(),
            "period", period,
            "records", List.of(record));

    try {
      var request =
          requestTo(batchesUri)
              .header("Content-Type", "application/json")
              .header("Idempotency-Key", batchId)
              .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(batch)))
              .build();

      var response = client.send(request, HttpResponse.BodyHandlers.discarding());

      return switch (response.statusCode()) {
        case 200 -> SubmitResult.ACCEPTED;
        case 422 -> SubmitResult.REJECTED;
        default -> SubmitResult.TIMEOUT;
      };
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return SubmitResult.TIMEOUT;
    } catch (IOException e) {
      return SubmitResult.TIMEOUT;
    }
  }

  @Override
  public LookupResult lookup(UUID batchId) {
    URI statusUri = URI.create(batchesUri + "/" + batchId + "/status");
    var request = requestTo(statusUri).GET().build();

    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 404) {
        return LookupResult.NOT_FOUND;
      }
      if (response.statusCode() != 200) {
        return LookupResult.UNKNOWN;
      }

      JsonNode body = json.readTree(response.body());
      if (body == null) {
        return LookupResult.UNKNOWN;
      }

      return switch (body.path("status").asText()) {
        case "ACCEPTED" -> LookupResult.ACCEPTED;
        case "REJECTED" -> LookupResult.REJECTED;
        default -> LookupResult.UNKNOWN;
      };
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return LookupResult.UNKNOWN;
    } catch (IOException e) {
      return LookupResult.UNKNOWN;
    }
  }

  private HttpRequest.Builder requestTo(URI uri) {
    return HttpRequest.newBuilder(uri)
        .timeout(REQUEST_TIMEOUT)
        .header("Authorization", "Bearer " + bearerToken)
        .header("Accept", "application/json");
  }
}