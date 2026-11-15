package vn.tmc.userqr.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.tmc.userqr.bootstrap.UserQrApplication;

/**
 * The employees in GET /internal/v1/sync-bundle equal the single employee seed
 * docs/api/fixtures/employee-seed.json, in the EmployeeEligibility shape of user-qr.openapi.yaml.
 */
@SpringBootTest(classes = UserQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("inmem")
class EmployeeSeedContractTest {
  static final Path SEED = Path.of("../../docs/api/fixtures/employee-seed.json");

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  record Row(String employeeUuid, String status, Set<String> factories) {}

  private static Set<Row> rows(JsonNode array) {
    var out = new HashSet<Row>();
    for (JsonNode e : array) {
      var factories = new HashSet<String>();
      e.get("factories").forEach(f -> factories.add(f.asText()));
      out.add(new Row(e.get("employeeUuid").asText(), e.get("status").asText(), factories));
    }
    return out;
  }

  @Test
  void should_ship_exactly_the_shared_employee_seed_in_the_sync_bundle() throws Exception {
    var body =
        mvc.perform(get("/internal/v1/sync-bundle"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var bundle = json.readTree(body);

    assertThat(rows(bundle.get("employees")))
        .isEqualTo(rows(new ObjectMapper().readTree(SEED.toFile()).get("employees")));
    assertThat(bundle.get("employees").get(0).has("updatedAt")).isTrue();
  }
}
