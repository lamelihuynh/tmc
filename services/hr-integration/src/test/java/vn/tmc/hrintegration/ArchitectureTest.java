package vn.tmc.hrintegration;

import org.junit.jupiter.api.Test;
import vn.tmc.platform.testsupport.HexagonalRules;

class ArchitectureTest {
  @Test
  void should_respect_hexagonal_dependency_rules_and_not_import_other_services() {
    HexagonalRules.verifyAll(
        "vn.tmc.hrintegration",
        "vn.tmc.gatestation",
        "vn.tmc.attendance",
        "vn.tmc.userqr",
        "vn.tmc.identity",
        "vn.tmc.reporting",
        "vn.tmc.auditadmin");
  }
}
