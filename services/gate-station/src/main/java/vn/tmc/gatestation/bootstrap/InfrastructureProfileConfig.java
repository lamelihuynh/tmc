package vn.tmc.gatestation.bootstrap;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestClient;
import vn.tmc.gatestation.adapter.out.http.HttpAttendanceGateway;
import vn.tmc.gatestation.adapter.out.http.HttpKioskSyncSource;
import vn.tmc.gatestation.adapter.out.http.HttpQrValidationClient;
import vn.tmc.gatestation.adapter.out.http.HttpTurnstileAdapter;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryAntiPassbackStore;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryTokenReservation;
import vn.tmc.gatestation.adapter.out.redis.RedisAntiPassbackStore;
import vn.tmc.gatestation.adapter.out.redis.RedisTokenReservation;
import vn.tmc.gatestation.adapter.out.simulated.SimulatedTurnstileAdapter;
import vn.tmc.gatestation.application.port.out.IAntiPassbackStore;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway;
import vn.tmc.gatestation.application.port.out.IKioskSyncSource;
import vn.tmc.gatestation.application.port.out.IQRValidation;
import vn.tmc.gatestation.application.port.out.ITokenReservation;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort;
import vn.tmc.platform.kernel.IClock;

/**
 * Profiles other than "inmem" talk to the real peers over HTTP. Where the one-time-use state lives
 * is a separate choice: Redis by default, or process memory with the "local" profile (no Docker,
 * single node, for demos and development; state is lost on restart, so never use it on a real edge
 * node).
 */
@Configuration
@Profile("!inmem")
public class InfrastructureProfileConfig {

  @Bean
  IAttendanceGateway attendanceGateway(GateProperties props) {
    return new HttpAttendanceGateway(
        RestClient.builder().baseUrl(props.urls().attendance()).build());
  }

  @Bean
  IQRValidation qrValidation(GateProperties props) {
    return new HttpQrValidationClient(RestClient.builder().baseUrl(props.urls().userQr()).build());
  }

  @Bean
  IKioskSyncSource kioskSyncSource(GateProperties props) {
    return new HttpKioskSyncSource(RestClient.builder().baseUrl(props.urls().userQr()).build());
  }

  /**
   * Swap the turnstile adapter with one property, without touching any business code (OCP, DP-10).
   */
  @Bean
  ITurnstileControlPort turnstile(GateProperties props) {
    var t = props.turnstile();
    if (t == null || "simulated".equalsIgnoreCase(t.mode())) {
      return new SimulatedTurnstileAdapter();
    }
    return new HttpTurnstileAdapter(
        t.baseUrl(),
        t.token(),
        Duration.ofMillis(t.connectTimeoutMs()),
        Duration.ofMillis(t.readTimeoutMs()));
  }

  @Configuration
  @Profile("!inmem & !local")
  static class RedisState {
    @Bean
    ITokenReservation tokenReservation(StringRedisTemplate redis, IClock clock) {
      return new RedisTokenReservation(redis, clock);
    }

    @Bean
    IAntiPassbackStore antiPassbackStore(StringRedisTemplate redis) {
      return new RedisAntiPassbackStore(redis);
    }
  }

  @Configuration
  @Profile("local")
  static class LocalState {
    @Bean
    ITokenReservation tokenReservation(IClock clock) {
      return new InMemoryTokenReservation(clock);
    }

    @Bean
    IAntiPassbackStore antiPassbackStore(IClock clock) {
      return new InMemoryAntiPassbackStore(clock);
    }
  }
}
