package vn.tmc.identity.adapter.out.inmem;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import vn.tmc.identity.application.port.out.IPasswordVerifier;

/**
 * Keeps only a salted PBKDF2 hash per username, never the password (NFR-04). Comparison is constant
 * time. An unknown username still pays for one hash so timing does not reveal which accounts exist.
 */
public class Pbkdf2PasswordVerifier implements IPasswordVerifier {
  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final int ITERATIONS = 120_000;
  private static final int KEY_BITS = 256;
  private static final int SALT_BYTES = 16;

  private record Entry(byte[] salt, byte[] hash) {}

  private final Map<String, Entry> entries = new ConcurrentHashMap<>();
  private final SecureRandom random = new SecureRandom();
  private final byte[] decoySalt = new byte[SALT_BYTES];

  public void set(String username, String password) {
    var salt = new byte[SALT_BYTES];
    random.nextBytes(salt);
    entries.put(username, new Entry(salt, hash(password, salt)));
  }

  @Override
  public boolean matches(String username, String password) {
    if (password == null) {
      return false;
    }
    var entry = username == null ? null : entries.get(username);
    if (entry == null) {
      hash(password, decoySalt);
      return false;
    }
    return MessageDigest.isEqual(entry.hash(), hash(password, entry.salt()));
  }

  private static byte[] hash(String password, byte[] salt) {
    var spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
    try {
      return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
    } catch (Exception e) {
      throw new IllegalStateException("PBKDF2 is not available", e);
    } finally {
      spec.clearPassword();
    }
  }

  @Override
  public String toString() {
    return "Pbkdf2PasswordVerifier[" + entries.size() + " accounts]";
  }
}
