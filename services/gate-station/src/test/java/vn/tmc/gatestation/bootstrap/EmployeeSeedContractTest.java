package vn.tmc.gatestation.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The seed in application.yml equals the single employee seed docs/api/fixtures/employee-seed.json.
 */
@SpringBootTest(classes = GateStationApplication.class)
@ActiveProfiles("inmem")
class EmployeeSeedContractTest {
  static final Path SEED = Path.of("../../docs/api/fixtures/employee-seed.json");

  @Autowired GateProperties props;

  record Row(UUID employeeUuid, boolean active, Set<String> factories) {}

  @Test
  void should_match_the_shared_employee_seed() throws Exception {
    var expected = new HashSet<Row>();
    for (JsonNode e : new ObjectMapper().readTree(SEED.toFile()).get("employees")) {
      var factories = new HashSet<String>();
      e.get("factories").forEach(f -> factories.add(f.asText()));
      expected.add(
          new Row(
              UUID.fromString(e.get("employeeUuid").asText()),
              "ACTIVE".equals(e.get("status").asText()),
              factories));
    }

    var actual =
        props.employees().stream()
            .map(e -> new Row(e.employeeUuid(), e.active(), Set.copyOf(e.factories())))
            .collect(Collectors.toSet());

    assertThat(actual).isEqualTo(expected);
  }
}
