package vn.tmc.kiosksim;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import vn.tmc.platform.kernel.IClock;

/**
 * Pretend to be one gate kiosk.
 *
 * <pre>
 *   kiosk-sim sync                         pull gates + device keys + revocations
 *   kiosk-sim scan --gate BD-GATE-01 TOKEN scan a QR (online, falls back to offline by itself)
 *   kiosk-sim offline on|off               force offline mode to demonstrate ADR-11
 *   kiosk-sim flush                        hand undelivered journal rows to the edge
 *   kiosk-sim status                       print the journal
 * </pre>
 */
@Command(
    name = "kiosk-sim",
    mixinStandardHelpOptions = true,
    subcommands = {
      KioskSimulatorCli.Sync.class,
      KioskSimulatorCli.Scan.class,
      KioskSimulatorCli.Offline.class,
      KioskSimulatorCli.Flush.class,
      KioskSimulatorCli.Status.class
    })
public class KioskSimulatorCli implements Runnable {

  public static void main(String[] args) {
    System.exit(new CommandLine(new KioskSimulatorCli()).execute(args));
  }

  @Override
  public void run() {
    new CommandLine(this).usage(System.out);
  }

  abstract static class Base implements Callable<Integer> {
    @Option(
        names = "--edge",
        defaultValue = "http://localhost:8081",
        description = "Edge Gateway base URL")
    String edge;

    @Option(names = "--kiosk-id", defaultValue = "BD-KIOSK-01")
    String kioskId;

    @Option(names = "--db", description = "journal file (default ~/.tmc-sim/<kiosk-id>.db)")
    Path db;

    Journal openJournal() throws Exception {
      var path =
          db != null ? db : Path.of(System.getProperty("user.home"), ".tmc-sim", kioskId + ".db");
      Files.createDirectories(path.toAbsolutePath().getParent());
      return new Journal(path.toString());
    }

    KioskApp app(Journal journal) {
      LocalGateLink link =
          (gate, direction) ->
              System.out.println("[LOCAL GATE LINK] OPEN gate=" + gate + " direction=" + direction);
      return new KioskApp(
          journal,
          new HttpEdgeClient(edge, Duration.ofMillis(1500)),
          KioskApp.defaultVerifier(journal),
          link,
          IClock.system(),
          kioskId,
          2);
    }
  }

  interface LocalGateLink extends KioskApp.LocalGate {}

  @Command(
      name = "sync",
      description = "Pull device keys, revocations and gate catalog from the edge.")
  static class Sync extends Base {
    @Override
    public Integer call() throws Exception {
      try (var j = openJournal()) {
        app(j).sync();
        System.out.println("synced");
        return 0;
      } catch (EdgeClient.EdgeUnreachableException e) {
        System.err.println("edge not reachable: " + e.getMessage());
        return 1;
      }
    }
  }

  @Command(name = "scan", description = "Scan one QR token.")
  static class Scan extends Base {
    @Option(names = "--gate", required = true)
    String gate;

    @Parameters(index = "0", description = "compact QR token from device-sim")
    String token;

    @Override
    public Integer call() throws Exception {
      try (var j = openJournal()) {
        var r = app(j).scan(token, gate);
        System.out.printf(
            "%s attempt=%s reason=%s direction=%s mode=%s%n",
            r.outcome(),
            r.attemptId(),
            r.reason(),
            r.direction(),
            r.offline() ? "OFFLINE" : "ONLINE");
        return "ALLOWED".equals(r.outcome()) ? 0 : 2;
      }
    }
  }

  @Command(name = "offline", description = "Force offline mode on or off.")
  static class Offline extends Base {
    @Parameters(index = "0", description = "on | off")
    String mode;

    @Override
    public Integer call() throws Exception {
      try (var j = openJournal()) {
        app(j).setForcedOffline("on".equalsIgnoreCase(mode));
        System.out.println("forced offline = " + "on".equalsIgnoreCase(mode));
        return 0;
      }
    }
  }

  @Command(
      name = "flush",
      description = "Hand undelivered journal rows to the edge, in journal order.")
  static class Flush extends Base {
    @Override
    public Integer call() throws Exception {
      try (var j = openJournal()) {
        var r = app(j).flush();
        System.out.println(r);
        return r.stillPending() == 0 ? 0 : 1;
      }
    }
  }

  @Command(name = "status", description = "Print the journal.")
  static class Status extends Base {
    @Override
    public Integer call() throws Exception {
      try (var j = openJournal()) {
        System.out.println("seq | attempt | gate | status | direction | handed_over | result");
        j.all()
            .forEach(
                r ->
                    System.out.printf(
                        "%d | %s | %s | %s | %s | %s | %s%n",
                        r.seq(),
                        r.attemptId(),
                        r.gateId(),
                        r.status(),
                        r.direction(),
                        r.handedOver(),
                        r.result()));
        return 0;
      }
    }
  }
}
