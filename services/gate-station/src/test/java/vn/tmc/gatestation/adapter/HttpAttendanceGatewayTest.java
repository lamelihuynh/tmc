package vn.tmc.gatestation.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import vn.tmc.gatestation.adapter.out.http.HttpAttendanceGateway;
import vn.tmc.gatestation.adapter.out.inmem.FakeAttendanceGateway;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.AttemptRecord;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.GateResult;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.State;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.gatestation.domain.LastPass;

/**
 * Wire format of the Attendance internal API as Gate Station reads it
 * (docs/api/internal-attendance.openapi.yaml), and the fake that stands in for it.
 */
class HttpAttendanceGatewayTest {
  private static final UUID EMPLOYEE = UUID.fromString("0192a1c0-0000-7000-8000-000000000001");
  private final RestClient.Builder builder = RestClient.builder().baseUrl("http://attendance");
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
  private final HttpAttendanceGateway gateway = new HttpAttendanceGateway(builder.build());

  @Test
  void should_read_direction_and_employee_from_the_attempt_view_and_ignore_unknown_fields() {
    var id = UUID.randomUUID();
    server
        .expect(requestTo("http://attendance/internal/v1/attempts/" + id))
        .andRespond(
            withSuccess(
                """
                {"attemptId":"%s","status":"COMMITTED","reasonCode":null,"anomaly":null,
                 "duplicate":false,"employeeUuid":"%s","direction":"EXIT","addedLater":42}
                """
                    .formatted(id, EMPLOYEE),
                MediaType.APPLICATION_JSON));

    var state = gateway.find(id).orElseThrow();

    assertThat(state.state()).isEqualTo(State.COMMITTED);
    assertThat(state.employeeUuid()).isEqualTo(EMPLOYEE);
    assertThat(state.direction()).isEqualTo(Direction.EXIT);
  }

  @Test
  void should_read_the_last_counted_pass_and_map_404_to_empty() {
    var before = Instant.parse("2026-11-20T08:00:00Z");
    server
        .expect(
            requestTo(
                "http://attendance/internal/v1/employees/"
                    + EMPLOYEE
                    + "/last-direction?before=2026-11-20T08:00:00Z"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                {"employeeUuid":"%s","direction":"ENTRY","occurredAt":"2026-11-20T06:55:00Z",
                 "factoryCode":"BD","gateId":"BD-GATE-01"}
                """
                    .formatted(EMPLOYEE),
                MediaType.APPLICATION_JSON));
    var other = UUID.randomUUID();
    server
        .expect(
            requestTo(
                "http://attendance/internal/v1/employees/"
                    + other
                    + "/last-direction?before=2026-11-20T08:00:00Z"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThat(gateway.lastDirection(EMPLOYEE, before))
        .contains(new LastPass(Direction.ENTRY, Instant.parse("2026-11-20T06:55:00Z")));
    assertThat(gateway.lastDirection(other, before)).isEmpty();
    server.verify();
  }

  @Test
  void fake_should_answer_the_last_committed_pass_like_attendance() {
    var fake = new FakeAttendanceGateway();
    var t0 = Instant.parse("2026-11-20T06:55:00Z");
    var id = UUID.randomUUID();
    fake.recordAttempt(
        new AttemptRecord(
            id, EMPLOYEE, "BD-GATE-01", "BD", Direction.ENTRY, "j", "ONLINE", t0, "c"));
    assertThat(fake.lastDirection(EMPLOYEE, t0.plusSeconds(60))).isEmpty(); // not committed yet

    fake.markGateCommanded(id);
    fake.recordGateResult(id, GateResult.OPENED, "c");

    assertThat(fake.lastDirection(EMPLOYEE, t0.plusSeconds(60)))
        .contains(new LastPass(Direction.ENTRY, t0));
    assertThat(fake.lastDirection(EMPLOYEE, t0)).isEmpty(); // strictly before
    assertThat(fake.find(id).orElseThrow().direction()).isEqualTo(Direction.ENTRY);

    fake.seedLastPass(
        UUID.fromString("0192a1c0-0000-7000-8000-000000000002"), new LastPass(Direction.EXIT, t0));
    assertThat(
            fake.lastDirection(
                UUID.fromString("0192a1c0-0000-7000-8000-000000000002"), t0.plusSeconds(1)))
        .contains(new LastPass(Direction.EXIT, t0));
  }
}
