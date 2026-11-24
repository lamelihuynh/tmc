package vn.tmc.userqr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;
import vn.tmc.userqr.adapter.out.inmem.InMemoryDeviceKeyRegistry;
import vn.tmc.userqr.adapter.out.inmem.LoggingAuditLog;
import vn.tmc.userqr.application.port.out.ActiveDeviceConflictException;
import vn.tmc.userqr.application.port.out.IDeviceKeyRegistry;
import vn.tmc.userqr.application.usecase.DeviceManagementService;
import vn.tmc.userqr.domain.Device;
import vn.tmc.userqr.support.MutableClock;
import vn.tmc.userqr.support.PhoneSimulator;

/**
 * C-01 without a database: the in-memory registry keeps the "one ACTIVE device per employee" rule
 * of the PostgreSQL index, and the use case audits a lost race before passing it on.
 */
class DeviceRegistrationConflictTest {
  private final MutableClock clock = new MutableClock();
  private final UUID employee = UUID.randomUUID();

  private Principal asEmployee() {
    return new Principal(employee, Set.of(Role.EMPLOYEE), DataScope.all());
  }

  @Test
  void should_refuse_a_second_active_device_in_memory_like_the_unique_index() {
    var registry = new InMemoryDeviceKeyRegistry();
    var key = new PhoneSimulator(employee).keys.getPublic();
    var first = Device.register("dev-1", employee, key, clock.now());
    registry.save(first);

    assertThatThrownBy(() -> registry.save(Device.register("dev-2", employee, key, clock.now())))
        .isInstanceOf(ActiveDeviceConflictException.class);
    // saving the same device again, or a revoked one, is not a conflict
    registry.save(first);
    registry.save(Device.register("dev-3", employee, key, clock.now()).revoke(clock.now()));
    assertThat(registry.findActiveByEmployee(employee).orElseThrow().deviceId()).isEqualTo("dev-1");
  }

  @Test
  void should_audit_the_conflict_and_pass_it_on_when_a_parallel_registration_won() {
    var audit = new LoggingAuditLog();
    var inner = new InMemoryDeviceKeyRegistry();
    // simulates the race: the other registration commits between our read and our insert
    IDeviceKeyRegistry racing =
        new IDeviceKeyRegistry() {
          @Override
          public Optional<Device> findById(String deviceId) {
            return inner.findById(deviceId);
          }

          @Override
          public Optional<Device> findActiveByEmployee(UUID employeeUuid) {
            var seen = inner.findActiveByEmployee(employeeUuid);
            inner.save(
                Device.register(
                    "dev-winner",
                    employeeUuid,
                    new PhoneSimulator(employeeUuid).keys.getPublic(),
                    clock.now()));
            return seen;
          }

          @Override
          public void save(Device device) {
            inner.save(device);
          }

          @Override
          public List<Device> changedSince(Instant since) {
            return inner.changedSince(since);
          }
        };
    var service = new DeviceManagementService(racing, audit, clock);

    assertThatThrownBy(
            () ->
                service.register(
                    asEmployee(), employee, new PhoneSimulator(employee).publicKeyBase64()))
        .isInstanceOf(ActiveDeviceConflictException.class);

    assertThat(audit.entries())
        .containsExactly(
            new LoggingAuditLog.Entry(
                "DEVICE_REGISTER", employee, employee.toString(), "CONFLICT"));
    assertThat(inner.findActiveByEmployee(employee).orElseThrow().deviceId())
        .isEqualTo("dev-winner");
  }
}
