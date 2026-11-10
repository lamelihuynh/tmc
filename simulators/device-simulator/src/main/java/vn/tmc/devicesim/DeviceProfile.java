package vn.tmc.devicesim;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

/**
 * A simulated phone: one P-256 key pair plus the identity it registered with (ADR-12 device
 * binding). Stored as a small properties file. The private key stays in this file the way it would
 * stay in the phone's keystore: it is never sent anywhere.
 */
public record DeviceProfile(
    UUID employeeUuid, String deviceId, String publicKeyX509Base64, String privateKeyPkcs8Base64) {

  public static DeviceProfile generate(UUID employeeUuid) {
    try {
      var gen = KeyPairGenerator.getInstance("EC");
      gen.initialize(new ECGenParameterSpec("secp256r1"));
      KeyPair pair = gen.generateKeyPair();
      return new DeviceProfile(
          employeeUuid,
          null,
          Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
          Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
    } catch (Exception e) {
      throw new IllegalStateException("cannot generate key pair", e);
    }
  }

  public DeviceProfile withDeviceId(String id) {
    return new DeviceProfile(employeeUuid, id, publicKeyX509Base64, privateKeyPkcs8Base64);
  }

  public ECPrivateKey privateKey() {
    try {
      return (ECPrivateKey)
          KeyFactory.getInstance("EC")
              .generatePrivate(
                  new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyPkcs8Base64)));
    } catch (Exception e) {
      throw new IllegalStateException("corrupt private key", e);
    }
  }

  public void save(Path file) throws IOException {
    Files.createDirectories(file.getParent());
    var p = new Properties();
    p.setProperty("employeeUuid", employeeUuid.toString());
    if (deviceId != null) {
      p.setProperty("deviceId", deviceId);
    }
    p.setProperty("publicKey", publicKeyX509Base64);
    p.setProperty("privateKey", privateKeyPkcs8Base64);
    try (OutputStream out = Files.newOutputStream(file)) {
      p.store(out, "TMC device simulator profile - dev only, never use real keys");
    }
  }

  public static DeviceProfile load(Path file) throws IOException {
    var p = new Properties();
    try (InputStream in = Files.newInputStream(file)) {
      p.load(in);
    }
    return new DeviceProfile(
        UUID.fromString(p.getProperty("employeeUuid")),
        p.getProperty("deviceId"),
        p.getProperty("publicKey"),
        p.getProperty("privateKey"));
  }
}
