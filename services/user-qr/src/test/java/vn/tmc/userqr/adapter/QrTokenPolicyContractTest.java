package vn.tmc.userqr.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import vn.tmc.userqr.bootstrap.UserQrApplication;
import vn.tmc.userqr.domain.QrClaims;
import vn.tmc.userqr.domain.TokenVerificationPolicy;

/**
 * The server's token rules (as configured in application.yml, and the code defaults) equal the
 * shared QR token policy docs/api/fixtures/qr-token-policy.json. The kiosk simulator and the
 * employee app check the same file, so changing one side without the others fails a build.
 */
@SpringBootTest(classes = UserQrApplication.class)
@ActiveProfiles("inmem")
class QrTokenPolicyContractTest {
  @Autowired TokenVerificationPolicy configured;

  @Test
  void should_match_the_shared_policy_file() throws Exception {
    JsonNode policy =
        new ObjectMapper()
            .readTree(Path.of("../../docs/api/fixtures/qr-token-policy.json").toFile());
    Set<String> allowed = new HashSet<>();
    policy.get("allowedClaims").forEach(c -> allowed.add(c.asText()));
    var skew = Duration.ofSeconds(policy.get("clockSkewSeconds").asLong());
    var lifetime = Duration.ofSeconds(policy.get("maxLifetimeSeconds").asLong());

    assertThat(QrClaims.ALLOWED_CLAIM_NAMES).isEqualTo(allowed);
    assertThat(configured.clockSkew()).isEqualTo(skew);
    assertThat(configured.maxLifetime()).isEqualTo(lifetime);
    assertThat(TokenVerificationPolicy.defaults()).isEqualTo(configured);
    assertThat(policy.get("defaultTtlSeconds").asLong())
        .isLessThanOrEqualTo(policy.get("maxLifetimeSeconds").asLong());
  }
}
