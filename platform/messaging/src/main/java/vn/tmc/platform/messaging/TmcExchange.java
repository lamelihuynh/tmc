package vn.tmc.platform.messaging;

/**
 * The one topic exchange every service publishes to (ADR-15,
 * docs/api/attendance-events.asyncapi.yaml). Routing key = {@code <source>.<eventType>}, source =
 * the short service name.
 */
public final class TmcExchange {
  private TmcExchange() {}

  public static final String NAME = "tmc.events";

  public static final String ATTENDANCE = "attendance";
  public static final String USER_QR = "user-qr";
  public static final String IDENTITY = "identity";
  public static final String GATE_STATION = "gate-station";

  public static String routingKey(String source, String eventType) {
    return source + "." + eventType;
  }

  /** Binding pattern for one event type from every source, e.g. {@code *.AuditRecorded}. */
  public static String anySource(String eventType) {
    return "*." + eventType;
  }

  /** Binding pattern for every event of one source, e.g. {@code attendance.*}. */
  public static String everyEventOf(String source) {
    return source + ".*";
  }
}
