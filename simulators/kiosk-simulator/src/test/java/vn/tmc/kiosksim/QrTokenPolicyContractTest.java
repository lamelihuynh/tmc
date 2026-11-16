package vn.tmc.kiosksim;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The kiosk's offline rules equal the shared QR token policy
 * docs/api/fixtures/qr-token-policy.json, which user-qr (QrTokenPolicyContractTest) and the
 * employee app (token.test.ts) also read.
 */
class QrTokenPolicyContractTest {
  @Test
  void should_use_the_shared_claim_list_skew_and_lifetime() throws Exception {
    JsonNode policy =
        new ObjectMapper()
            .readTree(Path.of("../../docs/api/fixtures/qr-token-policy.json").toFile());
    Set<String> allowed = new HashSet<>();
    policy.get("allowedClaims").forEach(c -> allowed.add(c.asText()));
    var verifier = KioskApp.defaultVerifier(null);

    assertThat(OfflineVerifier.ALLOWED).isEqualTo(allowed);
    assertThat(verifier.clockSkew())
        .isEqualTo(Duration.ofSeconds(policy.get("clockSkewSeconds").asLong()));
    assertThat(verifier.maxLifetime())
        .isEqualTo(Duration.ofSeconds(policy.get("maxLifetimeSeconds").asLong()));
    assertThat(policy.get("algorithm").asText()).isEqualTo("ES256");
  }
}
