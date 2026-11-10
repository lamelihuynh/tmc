package vn.tmc.auditadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import vn.tmc.auditadmin.adapter.out.inmem.InMemoryAuditStore;
import vn.tmc.auditadmin.application.port.out.IAuditStore;
import vn.tmc.auditadmin.application.usecase.AuditQueryService;
import vn.tmc.auditadmin.application.usecase.RecordAuditUseCase;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.InMemoryProcessedMessageStore;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;

class AuditTest {
  private final InMemoryAuditStore store = new InMemoryAuditStore();
  private final RecordAuditUseCase record = new RecordAuditUseCase(store);
  private final AuditQueryService query = new AuditQueryService(store, record, IClock.system());
  private final Principal admin =
      new Principal(UUID.randomUUID(), Set.of(Role.SYSTEM_ADMIN), DataScope.all());

  private EventEnvelope audit(String action) {
    return new EventEnvelope(
        Ids.uuidV7(),
        "AuditRecorded",
        1,
        Ids.uuidV7(),
        1,
        Instant.now(),
        "c",
        "attendance-edge",
        "BD",
        "G",
        Map.of("action", action, "employeeUuid", UUID.randomUUID().toString()));
  }

  @Test
  void should_have_no_way_to_change_or_remove_history_through_the_store_port() {
    var names =
        Arrays.stream(IAuditStore.class.getDeclaredMethods())
            .filter(m -> Modifier.isPublic(m.getModifiers()))
            .map(m -> m.getName())
            .toList();

    assertThat(names)
        .noneMatch(
            n ->
                n.startsWith("update")
                    || n.startsWith("delete")
                    || n.startsWith("remove")
                    || n.startsWith("clear"));
  }

  @Test
  void should_store_an_audit_event_and_find_it_by_action() {
    record.handle(audit("MANUAL_OPEN_PENDING_VERIFICATION"));

    assertThat(query.search(admin, null, null, "MANUAL_OPEN_PENDING_VERIFICATION", null))
        .hasSize(1);
  }

  @Test
  void should_keep_one_entry_when_the_same_message_is_delivered_twice() {
    var msg = audit("ACCESS_DENIED");
    var consumer =
        new IdempotentConsumer("audit-admin", new InMemoryProcessedMessageStore(), record::handle);

    consumer.accept(msg);
    consumer.accept(msg);
    record.handle(msg); // even bypassing the consumer table the message id keeps it unique

    assertThat(store.count()).isEqualTo(1);
  }

  @Test
  void should_ignore_events_that_are_not_audit_events() {
    record.handle(
        new EventEnvelope(
            Ids.uuidV7(),
            "AttendanceConfirmed",
            1,
            Ids.uuidV7(),
            1,
            Instant.now(),
            "c",
            "s",
            "BD",
            "G",
            Map.of()));

    assertThat(store.count()).isZero();
  }

  @Test
  void should_audit_the_search_itself() {
    query.search(admin, null, null, null, null);

    assertThat(store.search(null, null, "AUDIT_QUERIED", null, 10)).hasSize(1);
  }

  @Test
  void should_forbid_everyone_but_the_system_admin_from_reading_audit() {
    var hr = new Principal(UUID.randomUUID(), Set.of(Role.HR_STAFF), DataScope.all());
    var guard = new Principal(UUID.randomUUID(), Set.of(Role.SECURITY_GUARD), DataScope.all());

    assertThatThrownBy(() -> query.search(hr, null, null, null, null))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> query.search(guard, null, null, null, null))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Disabled("TODO Team D (AC-05): hash chain over entries so tampering is detectable")
  @Test
  void should_detect_a_modified_entry_through_the_hash_chain() {}

  @Disabled(
      "TODO Team D (NFR-02): search returns within 5 seconds and retention is at least 12 months")
  @Test
  void should_meet_the_query_time_and_retention_requirements() {}
}
