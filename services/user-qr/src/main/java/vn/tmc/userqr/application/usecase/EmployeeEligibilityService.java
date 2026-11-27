package vn.tmc.userqr.application.usecase;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Rbac;
import vn.tmc.platform.security.Role;
import vn.tmc.userqr.application.port.in.IEmployeeEligibility;
import vn.tmc.userqr.application.port.out.IAuditLog;
import vn.tmc.userqr.application.port.out.IDeviceKeyRegistry;
import vn.tmc.userqr.application.port.out.IEmployeeDirectory;
import vn.tmc.userqr.application.port.out.ITransactionRunner;

/**
 * C-02: HR feed for employee eligibility. Updates {@code employees}, revokes any ACTIVE device when
 * the employee becomes INACTIVE, and records the audit row(s) all within one transaction (C-08,
 * ADR-04). Only identifiers and status reach the audit log, never personal data (ADR-09).
 */
public class EmployeeEligibilityService implements IEmployeeEligibility {
  public static final String EMPLOYEE_ELIGIBILITY_UPDATE = "EMPLOYEE_ELIGIBILITY_UPDATE";
  public static final String DEVICE_REVOKE_BY_STAFF = "DEVICE_REVOKE_BY_STAFF";

  private final IEmployeeDirectory employees;
  private final IDeviceKeyRegistry devices;
  private final IAuditLog audit;
  private final ITransactionRunner tx;
  private final IClock clock;

  /** Without a transaction: for unit tests on the in-memory adapters. */
  public EmployeeEligibilityService(
      IEmployeeDirectory employees, IDeviceKeyRegistry devices, IAuditLog audit, IClock clock) {
    this(employees, devices, audit, ITransactionRunner.none(), clock);
  }

  public EmployeeEligibilityService(
      IEmployeeDirectory employees,
      IDeviceKeyRegistry devices,
      IAuditLog audit,
      ITransactionRunner tx,
      IClock clock) {
    this.employees = employees;
    this.devices = devices;
    this.audit = audit;
    this.tx = tx;
    this.clock = clock;
  }

  @Override
  public EmployeeEligibilityView update(
      Principal caller, UUID employeeUuid, Status status, Set<String> factories) {
    if (caller != null && !caller.hasAnyRole(Role.HR_STAFF) && employeeUuid != null) {
      audit.record(
          EMPLOYEE_ELIGIBILITY_UPDATE, caller.subject(), employeeUuid.toString(), "DENIED");
    }
    Rbac.requireRole(caller, Role.HR_STAFF);
    if (employeeUuid == null) {
      throw new IllegalArgumentException("employeeUuid is required");
    }
    if (status == null) {
      throw new IllegalArgumentException("status is required");
    }
    var normalizedFactories = normalizeFactories(factories);
    return tx.inTransaction(
        () -> {
          var now = clock.now();
          var eligibility =
              new IEmployeeDirectory.Eligibility(
                  employeeUuid, status == Status.ACTIVE, normalizedFactories, now);
          employees.save(eligibility);
          if (status == Status.INACTIVE) {
            devices
                .findActiveByEmployee(employeeUuid)
                .ifPresent(
                    d -> {
                      devices.save(d.revoke(now));
                      audit.record(DEVICE_REVOKE_BY_STAFF, caller.subject(), d.deviceId(), "OK");
                    });
          }
          audit.record(
              EMPLOYEE_ELIGIBILITY_UPDATE,
              caller.subject(),
              employeeUuid.toString(),
              status.name());
          return new EmployeeEligibilityView(
              employeeUuid, status, normalizedFactories.stream().sorted().toList(), now);
        });
  }

  private static Set<String> normalizeFactories(Set<String> raw) {
    if (raw == null) {
      throw new IllegalArgumentException("factories is required");
    }
    var out = new LinkedHashSet<String>();
    for (String f : raw) {
      if (f == null || f.isBlank()) {
        throw new IllegalArgumentException("factory code must not be blank");
      }
      out.add(f.trim());
    }
    return Set.copyOf(out);
  }
}
