package vn.tmc.identity.application.port.out;

/** Driven port: second factor (NFR-03). Implementations must fail closed. */
@FunctionalInterface
public interface IMfaVerifier {
  boolean verify(String username, String code);
}
