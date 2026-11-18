package vn.tmc.identity.application.port.out;

/**
 * Driven port: checks a password without ever handing the stored secret to the use case. An unknown
 * username answers false, the same as a wrong password.
 */
public interface IPasswordVerifier {
  boolean matches(String username, String password);
}
