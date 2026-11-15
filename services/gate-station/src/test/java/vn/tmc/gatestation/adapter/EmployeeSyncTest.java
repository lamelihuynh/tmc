package vn.tmc.gatestation.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import vn.tmc.gatestation.adapter.out.http.HttpKioskSyncSource;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryEmployeeDirectory;
import vn.tmc.gatestation.application.usecase.SyncEmployeeDirectoryUseCase;
import vn.tmc.gatestation.domain.EmployeeAccess;

/**
 * Employee eligibility from User/QR's sync bundle (docs/api/user-qr.openapi.yaml) into the edge
 * directory: tolerant reader, newest wins, failures keep the last known list.
 */
class EmployeeSyncTest {
  private static final UUID E1 = UUID.fromString("0192a1c0-0000-7000-8000-000000000001");
  private static final UUID E9 = UUID.fromString("0192a1c0-0000-7000-8000-000000000009");
  private final RestClient.Builder builder = RestClient.builder().baseUrl("http://user-qr");
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
  private final HttpKioskSyncSource source = new HttpKioskSyncSource(builder.build());
  private final InMemoryEmployeeDirectory directory =
      new InMemoryEmployeeDirectory(List.of(new EmployeeAccess(E1, true, Set.of("BD"))));
  private final Instant now = Instant.parse("2026-11-20T08:00:00Z");
  private final SyncEmployeeDirectoryUseCase sync =
      new SyncEmployeeDirectoryUseCase(source, directory, () -> now);

  @Test
  void should_apply_employees_from_the_bundle_ignore_unknown_fields_and_move_the_cursor() {
    server
        .expect(requestTo("http://user-qr/internal/v1/sync-bundle?since=1970-01-01T00:00:00Z"))
        .andRespond(
            withSuccess(
                """
                {"generatedAt":"2026-11-20T07:59:59Z","deviceKeys":[],"futureField":{"x":1},
                 "employees":[
                   {"employeeUuid":"%s","status":"INACTIVE","factories":["BD"],"updatedAt":"2026-11-20T07:00:00Z","extra":true},
                   {"employeeUuid":"%s","status":"ACTIVE","factories":["LA","BD"]}]}
                """
                    .formatted(E1, E9),
                MediaType.APPLICATION_JSON));

    assertThat(sync.refresh()).isEqualTo(2);

    assertThat(directory.find(E1).orElseThrow().active()).isFalse();
    assertThat(directory.find(E9).orElseThrow().allowedFactories())
        .containsExactlyInAnyOrder("BD", "LA");
    assertThat(sync.cursor()).isEqualTo(Instant.parse("2026-11-20T07:59:59Z"));
  }

  @Test
  void should_treat_a_bundle_without_employees_as_none_for_an_older_user_qr() {
    server
        .expect(requestTo("http://user-qr/internal/v1/sync-bundle?since=1970-01-01T00:00:00Z"))
        .andRespond(
            withSuccess(
                "{\"generatedAt\":\"2026-11-20T07:59:59Z\",\"deviceKeys\":[]}",
                MediaType.APPLICATION_JSON));

    assertThat(sync.refresh()).isZero();
    assertThat(directory.find(E1).orElseThrow().active()).isTrue();
  }

  @Test
  void should_keep_the_last_known_list_and_the_cursor_when_user_qr_fails() {
    server
        .expect(requestTo("http://user-qr/internal/v1/sync-bundle?since=1970-01-01T00:00:00Z"))
        .andRespond(withServerError());

    assertThatThrownBy(sync::refresh).isInstanceOf(RuntimeException.class);
    assertThat(directory.find(E1)).isPresent();
    assertThat(sync.cursor()).isEqualTo(Instant.EPOCH);
  }
}
