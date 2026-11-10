package vn.tmc.platform.kernel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class IdsTest {

  @Test
  void should_generate_version_7_uuid() {
    assertThat(Ids.uuidV7().version()).isEqualTo(7);
  }

  @Test
  void should_generate_unique_ids() {
    var seen = new HashSet<>();
    for (int i = 0; i < 10_000; i++) {
      assertThat(seen.add(Ids.uuidV7())).isTrue();
    }
  }
}
