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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort;

/**
 * HR/Payroll over HTTP (docs/api/hr-payroll.openapi.yaml). Status mapping of a submit: 2xx except
 * 207 is ACCEPTED; 422 is REJECTED (HR checked the data); any other 4xx is REFUSED (HR did not take
 * the request, the cause is on our side, logged as an error); 207, 5xx and network errors are
 * TIMEOUT, because HR may have stored some or all of the batch and only a lookup can tell.
 */
public class HttpHrPayrollAdapter implements IHrPayrollPort {
  private static final Logger LOG = LoggerFactory.getLogger(HttpHrPayrollAdapter.class);
  private static final ZoneId PAYROLL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
  private static final int MAX_LOGGED_BODY = 500;

  private final HttpClient client;
  private final ObjectMapper json;
  private final URI batchesUri;
  private final String bearerToken;

  /**
   * @param baseUri HR base address; a path in it is kept ({@code https://gw/hr} sends to {@code
   *     https://gw/hr/api/v1/...})
   */
  public HttpHrPayrollAdapter(URI baseUri, String bearerToken, ObjectMapper json) {
    this.client = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
    this.json = json;
    var base = baseUri.toString().endsWith("/") ? baseUri : URI.create(baseUri + "/");
    this.batchesUri = base.resolve("api/v1/attendance-batches");
    this.bearerToken = bearerToken;
  }

  @Override
  public SubmitResult submit(Submission submission) {
    String batchId = submission.batchId().toString();
    String period = YearMonth.from(submission.scannedAt().atZone(PAYROLL_ZONE)).toString();

    var record = new LinkedHashMap<String, Object>();
    record.put("event_id", submission.eventId().toString());
    if (submission.correctsEventId() != null) {
      record.put("corrects_event_id", submission.correctsEventId().toString());
    }
    record.put("employee_uuid", submission.employeeUuid().toString());
    record.put("direction", submission.direction());
    record.put("occurred_at", submission.scannedAt().toString());

    var batch = new LinkedHashMap<String, Object>();
    batch.put("batch_id", batchId);
    batch.put("factory_code", submission.factoryCode());
    batch.put("period", period);
    batch.put("records", List.of(record));

    try {
      var request =
          requestTo(batchesUri)
              .header("Content-Type", "application/json")
              .header("Idempotency-Key", batchId)
              .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(batch)))
              .build();

      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      if (status >= 200 && status < 300 && status != 207) {
        return SubmitResult.ACCEPTED;
      }
      if (status == 422) {
        LOG.warn("HR/Payroll rejected batch {}: {}", batchId, shorten(response.body()));
        return SubmitResult.REJECTED;
      }
      if (status >= 400 && status < 500) {
        LOG.error(
            "HR/Payroll refused batch {} with HTTP {}: check HR_PAYROLL_API_TOKEN and tmc.hr.url."
                + " Body: {}",
            batchId,
            status,
            shorten(response.body()));
        return SubmitResult.REFUSED;
      }
      LOG.warn("HR/Payroll answered HTTP {} for batch {}; outcome unknown", status, batchId);
      return SubmitResult.TIMEOUT;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return SubmitResult.TIMEOUT;
    } catch (IOException e) {
      LOG.warn("No answer from HR/Payroll for batch {}: {}", batchId, e.toString());
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

  private static String shorten(String body) {
    if (body == null) {
      return "";
    }
    return body.length() <= MAX_LOGGED_BODY ? body : body.substring(0, MAX_LOGGED_BODY) + "...";
  }
}
