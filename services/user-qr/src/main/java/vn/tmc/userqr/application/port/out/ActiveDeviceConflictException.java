package vn.tmc.userqr.application.port.out;

/**
 * Part of the {@link IDeviceKeyRegistry} contract (C-01): saving an ACTIVE device while another
 * ACTIVE device of the same employee exists. On PostgreSQL this is the partial unique index {@code
 * uq_one_active_device_per_employee} refusing the loser of two parallel registrations. Plain Java
 * so the use case can react without knowing the storage technology.
 */
public class ActiveDeviceConflictException extends RuntimeException {
  public ActiveDeviceConflictException(String message, Throwable cause) {
    super(message, cause);
  }

  public ActiveDeviceConflictException(String message) {
    super(message);
  }
}
