package vn.tmc.userqr.application.usecase;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Rbac;
import vn.tmc.platform.security.Role;
import vn.tmc.userqr.application.port.in.IDeviceManagement;
import vn.tmc.userqr.application.port.out.ActiveDeviceConflictException;
import vn.tmc.userqr.application.port.out.IAuditLog;
import vn.tmc.userqr.application.port.out.IDeviceKeyRegistry;
import vn.tmc.userqr.application.port.out.ITransactionRunner;
import vn.tmc.userqr.domain.Device;

/**
 * UC-02 and UC-07: device enrollment and revocation with server-side authorization and audit
 * (ADR-08). Every device change and its audit row (outbox, C-08) run in one transaction: either
 * both are stored or neither is. A refusal changes nothing, so its DENIED audit is written outside
 * any transaction and survives the exception that follows.
 */
public class DeviceManagementService implements IDeviceManagement {
  private final IDeviceKeyRegistry registry;
  private final IAuditLog audit;
  private final ITransactionRunner tx;
  private final IClock clock;

  /** Without a transaction: for unit tests on the in-memory adapters. */
  public DeviceManagementService(IDeviceKeyRegistry registry, IAuditLog audit, IClock clock) {
    this(registry, audit, ITransactionRunner.none(), clock);
  }

  public DeviceManagementService(
      IDeviceKeyRegistry registry, IAuditLog audit, ITransactionRunner tx, IClock clock) {
    this.registry = registry;
    this.audit = audit;
    this.tx = tx;
    this.clock = clock;
  }

  @Override
  public RegisteredDevice register(
      Principal caller, UUID employeeUuid, String publicKeyX509Base64) {
    Rbac.requireRole(caller, Role.EMPLOYEE);
    if (!caller.subject().equals(employeeUuid)) {
      audit.record("DEVICE_REGISTER", caller.subject(), employeeUuid.toString(), "DENIED");
      throw new AccessDeniedException("an employee can only register their own device");
    }
    var key = decodeEcPublicKey(publicKeyX509Base64);
    try {
      return tx.inTransaction(
          () -> {
            var now = clock.now();
            registry
                .findActiveByEmployee(employeeUuid)
                .ifPresent(old -> registry.save(old.revoke(now)));
            var device = Device.register("dev-" + Ids.uuidV7(), employeeUuid, key, now);
            registry.save(device);
            audit.record("DEVICE_REGISTER", caller.subject(), device.deviceId(), "OK");
            return new RegisteredDevice(device.deviceId());
          });
    } catch (ActiveDeviceConflictException e) {
      // C-01: a parallel registration for the same employee committed first. This one was rolled
      // back entirely (no device, no revocation, no outbox row); only the refusal is audited, after
      // the rollback so it survives. The caller gets 409 and may simply register again.
      audit.record("DEVICE_REGISTER", caller.subject(), employeeUuid.toString(), "CONFLICT");
      throw e;
    }
  }

  @Override
  public void revokeOwn(Principal caller, String deviceId) {
    Rbac.requireRole(caller, Role.EMPLOYEE);
    var device = registry.findById(deviceId).orElse(null);
    if (device == null || !device.employeeUuid().equals(caller.subject())) {
      // same answer for "missing" and "not yours" so ids cannot be probed
      audit.record("DEVICE_REVOKE", caller.subject(), deviceId, "DENIED");
      throw new AccessDeniedException("device not found for caller");
    }
    tx.inTransaction(
        () -> {
          registry.save(device.revoke(clock.now()));
          audit.record("DEVICE_REVOKE", caller.subject(), deviceId, "OK");
          return null;
        });
  }

  @Override
  public void revokeForEmployee(Principal caller, UUID employeeUuid) {
    Rbac.requireRole(caller, Role.HR_STAFF, Role.SYSTEM_ADMIN);
    tx.inTransaction(
        () -> {
          registry
              .findActiveByEmployee(employeeUuid)
              .ifPresent(
                  d -> {
                    registry.save(d.revoke(clock.now()));
                    audit.record("DEVICE_REVOKE_BY_STAFF", caller.subject(), d.deviceId(), "OK");
                  });
          return null;
        });
  }

  private static PublicKey decodeEcPublicKey(String base64) {
    try {
      var bytes = Base64.getDecoder().decode(base64);
      var key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(bytes));
      return key;
    } catch (Exception e) {
      throw new IllegalArgumentException("publicKey must be a Base64 X.509 EC (P-256) key", e);
    }
  }
}
