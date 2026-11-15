package vn.tmc.userqr.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;
import vn.tmc.userqr.adapter.out.inmem.InMemoryDeviceKeyRegistry;
import vn.tmc.userqr.adapter.out.inmem.InMemoryEmployeeDirectory;
import vn.tmc.userqr.adapter.out.inmem.LoggingAuditLog;
import vn.tmc.userqr.application.port.out.IEmployeeDirectory;
import vn.tmc.userqr.application.usecase.BuildSyncBundleUseCase;
import vn.tmc.userqr.application.usecase.DeviceManagementService;
import vn.tmc.userqr.support.MutableClock;
import vn.tmc.userqr.support.PhoneSimulator;

class SyncBundleTest {
  private final MutableClock clock = new MutableClock();
  private final InMemoryDeviceKeyRegistry registry = new InMemoryDeviceKeyRegistry();
  private final DeviceManagementService management =
      new DeviceManagementService(registry, new LoggingAuditLog(), clock);
  private final BuildSyncBundleUseCase sync = new BuildSyncBundleUseCase(registry, clock);

  @Test
  void
      should_ship_new_keys_and_revocations_changed_after_the_cursor_so_kiosks_can_verify_offline() {
    var e1 = UUID.randomUUID();
    var p1 = new PhoneSimulator(e1);
    var caller = new Principal(e1, Set.of(Role.EMPLOYEE), DataScope.all());
    var id = management.register(caller, e1, p1.publicKeyBase64()).deviceId();
    var cursor = clock.now();

    clock.advance(Duration.ofSeconds(5));
    management.revokeOwn(caller, id);

    var delta = sync.changedSince(cursor);

    assertThat(delta.deviceKeys()).hasSize(1);
    assertThat(delta.deviceKeys().get(0).deviceId()).isEqualTo(id);
    assertThat(delta.deviceKeys().get(0).revoked()).isTrue();
    assertThat(sync.changedSince(Instant.EPOCH).deviceKeys()).hasSize(1);
    assertThat(sync.changedSince(clock.now()).deviceKeys()).isEmpty();
  }

  @Test
  void should_ship_employee_eligibility_changed_after_the_cursor_inactive_ones_included() {
    var start = clock.now();
    var active = UUID.randomUUID();
    var leaver = UUID.randomUUID();
    var directory =
        new InMemoryEmployeeDirectory(
            List.of(
                new IEmployeeDirectory.Eligibility(active, true, Set.of("LA", "BD"), start),
                new IEmployeeDirectory.Eligibility(leaver, true, Set.of("BD"), start)));
    var withEmployees = new BuildSyncBundleUseCase(registry, directory, clock);

    clock.advance(Duration.ofSeconds(5));
    var cursor = clock.now();
    clock.advance(Duration.ofSeconds(5));
    directory.put(new IEmployeeDirectory.Eligibility(leaver, false, Set.of("BD"), clock.now()));

    assertThat(withEmployees.changedSince(Instant.EPOCH).employees()).hasSize(2);
    var delta = withEmployees.changedSince(cursor).employees();
    assertThat(delta).hasSize(1);
    assertThat(delta.get(0).employeeUuid()).isEqualTo(leaver);
    assertThat(delta.get(0).status()).isEqualTo("INACTIVE");
    assertThat(withEmployees.changedSince(Instant.EPOCH).employees())
        .filteredOn(e -> e.employeeUuid().equals(active))
        .first()
        .satisfies(e -> assertThat(e.factories()).containsExactly("BD", "LA"));
    // the old constructor still works and simply ships no employees
    assertThat(sync.changedSince(Instant.EPOCH).employees()).isEmpty();
  }
}
