package vn.tmc.hrintegration.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import vn.tmc.hrintegration.adapter.out.http.HttpHrPayrollAdapter;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort.LookupResult;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort.Submission;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort.SubmitResult;

class HttpHrPayrollAdapterTest {

  @Test
  void should_submit_one_batch_and_look_it_up_by_the_same_id() throws Exception {
    var eventId = UUID.randomUUID();
    var submission =
        new Submission(
            eventId, UUID.randomUUID(), "BD", "ENTRY", Instant.parse("2026-11-24T01:00:00Z"));

    verifySubmissionAndLookup(submission, eventId, eventId);
  }

  @Test
  void should_use_batch_id_for_submission_and_lookup_and_keep_original_event_id()
      throws Exception {
    var eventId = UUID.fromString("00000000-0000-4000-8000-000000000001");
    var batchId = UUID.fromString("00000000-0000-4000-8000-000000000002");
    var submission =
        new Submission(
            eventId, batchId, UUID.randomUUID(), "BD", "ENTRY", Instant.parse("2026-11-24T01:00:00Z"));

    verifySubmissionAndLookup(submission, eventId, batchId);
  }

  private void verifySubmissionAndLookup(
      Submission submission, UUID expectedEventId, UUID expectedBatchId) throws Exception {
    var json = new ObjectMapper();
    var postedBody = new AtomicReference<String>();
    var postedKey = new AtomicReference<String>();
    var postAuth = new AtomicReference<String>();
    var lookupAuth = new AtomicReference<String>();
    var batches = "/api/v1/attendance-batches";
    var statusPath = batches + "/" + expectedBatchId + "/status";
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

    server.createContext(
        batches,
        exchange -> {
          try (exchange) {
            var method = exchange.getRequestMethod();
            var path = exchange.getRequestURI().getPath();
            var headers = exchange.getRequestHeaders();

            if ("POST".equals(method) && batches.equals(path)) {
              postedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
              postedKey.set(headers.getFirst("Idempotency-Key"));
              postAuth.set(headers.getFirst("Authorization"));
            } else if ("GET".equals(method) && statusPath.equals(path)) {
              lookupAuth.set(headers.getFirst("Authorization"));
            } else {
              exchange.sendResponseHeaders(404, -1);
              return;
            }

            var response = "{\"status\":\"ACCEPTED\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
          }
        });

    server.start();
    try {
      var baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
      var adapter = new HttpHrPayrollAdapter(baseUri, "test-token", json);

      assertThat(adapter.submit(submission)).isEqualTo(SubmitResult.ACCEPTED);
      assertThat(postAuth.get()).isEqualTo("Bearer test-token");
      assertThat(postedKey.get()).isEqualTo(expectedBatchId.toString());

      var body = json.readTree(postedBody.get());
      assertThat(body.path("batch_id").asText()).isEqualTo(expectedBatchId.toString());
      assertThat(body.path("factory_code").asText()).isEqualTo("BD");
      assertThat(body.path("period").asText()).isEqualTo("2026-11");
      assertThat(body.path("records").size()).isEqualTo(1);
      assertThat(body.at("/records/0/event_id").asText()).isEqualTo(expectedEventId.toString());
      assertThat(body.at("/records/0/employee_uuid").asText())
          .isEqualTo(submission.employeeUuid().toString());
      assertThat(body.at("/records/0/direction").asText()).isEqualTo("ENTRY");
      assertThat(Instant.parse(body.at("/records/0/occurred_at").asText()))
          .isEqualTo(submission.scannedAt());

      assertThat(adapter.lookup(expectedBatchId)).isEqualTo(LookupResult.ACCEPTED);
      assertThat(lookupAuth.get()).isEqualTo("Bearer test-token");
    } finally {
      server.stop(0);
    }
  }
}