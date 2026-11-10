package vn.tmc.platform.security;

/** Maps to HTTP 403 and an audit entry (NFR-01). */
public class AccessDeniedException extends RuntimeException {
  public AccessDeniedException(String message) {
    super(message);
  }
}
