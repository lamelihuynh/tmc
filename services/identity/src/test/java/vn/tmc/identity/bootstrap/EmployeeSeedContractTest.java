package vn.tmc.identity.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * The identity dev accounts equal the single employee seed docs/api/fixtures/employee-seed.json.
 */
class EmployeeSeedContractTest {
  static final Path SEED = Path.of("../../docs/api/fixtures/employee-seed.json");

  record Row(String username, UUID employeeUuid, Set<String> factories) {}

  @Test
  void should_match_the_shared_employee_seed() throws Exception {
    JsonNode root = new ObjectMapper().readTree(SEED.toFile());
    Set<Row> expected = new HashSet<>();
    for (JsonNode e : root.get("employees")) {
      Set<String> factories = new HashSet<>();
      e.get("factories").forEach(f -> factories.add(f.asText()));
      expected.add(
          new Row(
              e.get("username").asText(),
              UUID.fromString(e.get("employeeUuid").asText()),
              factories));
    }

    var actual =
        DevSeed.EMPLOYEES.stream()
            .map(e -> new Row(e.username(), e.employeeUuid(), e.factories()))
            .collect(Collectors.toSet());

    assertThat(actual).isEqualTo(expected);
  }
}
