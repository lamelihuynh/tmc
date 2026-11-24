package vn.tmc.userqr.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.tmc.userqr.application.port.out.IEmployeeDirectory;
import vn.tmc.userqr.application.port.out.IEmployeeDirectory.Eligibility;

/**
 * Loads the shared dev employee seed from {@code docs/api/fixtures/employee-seed.json} (C-02). Only
 * eligibility fields (UUID, status, factories) are read; {@code username} is ignored (ADR-09).
 */
final class EmployeeSeedLoader {
  private static final Logger LOG = LoggerFactory.getLogger(EmployeeSeedLoader.class);
  private static final Path DEFAULT_RELATIVE = Path.of("docs/api/fixtures/employee-seed.json");
  private static final ObjectMapper JSON = new ObjectMapper();

  private EmployeeSeedLoader() {}

  static void seedIfAbsent(
      IEmployeeDirectory directory, String configuredPath, boolean enabled, Instant now) {
    if (!enabled) {
      return;
    }
    var file = resolvePath(configuredPath);
    if (file == null) {
      LOG.warn(
          "Employee seed file {} not found; starting with existing directory rows only",
          DEFAULT_RELATIVE);
      return;
    }
    for (var row : read(file, now)) {
      directory.saveIfAbsent(row);
    }
  }

  static List<Eligibility> read(Path file, Instant updatedAt) {
    try {
      JsonNode root = JSON.readTree(file.toFile());
      JsonNode employees = root.get("employees");
      if (employees == null || !employees.isArray()) {
        return List.of();
      }
      List<Eligibility> out = new ArrayList<>();
      for (JsonNode node : employees) {
        UUID uuid = UUID.fromString(node.get("employeeUuid").asText());
        boolean active = "ACTIVE".equals(node.get("status").asText());
        Set<String> factories = new LinkedHashSet<>();
        JsonNode facNode = node.get("factories");
        if (facNode != null && facNode.isArray()) {
          facNode.forEach(f -> factories.add(f.asText()));
        }
        out.add(new Eligibility(uuid, active, factories, updatedAt));
      }
      return List.copyOf(out);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read employee seed file: " + file, e);
    }
  }

  private static Path resolvePath(String configuredPath) {
    if (configuredPath != null && !configuredPath.isBlank()) {
      Path explicit = Path.of(configuredPath);
      if (!Files.isRegularFile(explicit)) {
        throw new IllegalStateException("Configured employee seed file not found: " + explicit);
      }
      return explicit;
    }
    Path dir = Path.of("").toAbsolutePath().normalize();
    for (int depth = 0; depth < 5 && dir != null; depth++) {
      Path candidate = dir.resolve(DEFAULT_RELATIVE);
      if (Files.isRegularFile(candidate)) {
        return candidate;
      }
      dir = dir.getParent();
    }
    return null;
  }
}
