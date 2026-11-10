package vn.tmc.devicesim;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Pretend to be an employee's phone.
 *
 * <pre>
 *   device-sim keygen   --name emp1 --employee 0192a1c0-0000-7000-8000-000000000001
 *   device-sim register --name emp1 --user-qr http://localhost:8083
 *   device-sim token    --name emp1
 *   device-sim token    --name emp1 --long-lived      (rejected: looks like a static QR)
 *   device-sim token    --name emp1 --pii             (rejected: carries a name)
 * </pre>
 */
@Command(
    name = "device-sim",
    mixinStandardHelpOptions = true,
    subcommands = {
      DeviceSimulatorCli.KeyGen.class,
      DeviceSimulatorCli.Register.class,
      DeviceSimulatorCli.Token.class
    })
public class DeviceSimulatorCli implements Runnable {

  public static void main(String[] args) {
    System.exit(new CommandLine(new DeviceSimulatorCli()).execute(args));
  }

  @Override
  public void run() {
    new CommandLine(this).usage(System.out);
  }

  abstract static class Base implements Callable<Integer> {
    @Option(names = "--name", required = true, description = "profile name, e.g. emp1")
    String name;

    @Option(names = "--home", description = "profile directory (default ~/.tmc-sim/devices)")
    Path home = Path.of(System.getProperty("user.home"), ".tmc-sim", "devices");

    Path file() {
      return home.resolve(name + ".properties");
    }
  }

  @Command(
      name = "keygen",
      description = "Create a key pair for an employee (private key stays local).")
  static class KeyGen extends Base {
    @Option(names = "--employee", required = true)
    UUID employee;

    @Override
    public Integer call() throws Exception {
      DeviceProfile.generate(employee).save(file());
      System.out.println("created " + file());
      return 0;
    }
  }

  @Command(
      name = "register",
      description = "Register the public key with User/QR (UC-07 enrollment).")
  static class Register extends Base {
    @Option(names = "--user-qr", defaultValue = "http://localhost:8083")
    String userQr;

    @Override
    public Integer call() throws Exception {
      var profile = DeviceProfile.load(file());
      var deviceId =
          new UserQrClient(userQr).register(profile.employeeUuid(), profile.publicKeyX509Base64());
      profile.withDeviceId(deviceId).save(file());
      System.out.println(
          "registered deviceId=" + deviceId + " (previous device of this employee is revoked)");
      return 0;
    }
  }

  @Command(name = "token", description = "Print a fresh signed QR token (default lifetime 30 s).")
  static class Token extends Base {
    @Option(names = "--ttl", defaultValue = "30", description = "lifetime in seconds")
    long ttl;

    @Option(names = "--jti", description = "force a token id (to demonstrate replay)")
    String jti;

    @Option(names = "--factories", split = ",", defaultValue = "BD,LA")
    List<String> factories;

    @Option(names = "--pii", description = "add a name claim: the server must reject it")
    boolean pii;

    @Option(names = "--long-lived", description = "24h lifetime: the server must reject it")
    boolean longLived;

    @Option(names = "--describe", description = "also print the decoded claims to stderr")
    boolean describe;

    @Override
    public Integer call() throws Exception {
      var profile = DeviceProfile.load(file());
      var token =
          TokenSigner.sign(
              profile, factories, new TokenSigner.Options(ttl, jti, pii, longLived, Instant.now()));
      if (describe) {
        System.err.println(TokenSigner.describe(token));
      }
      System.out.println(token);
      return 0;
    }
  }
}
