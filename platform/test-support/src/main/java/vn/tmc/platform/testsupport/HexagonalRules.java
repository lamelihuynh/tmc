package vn.tmc.platform.testsupport;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Dependency rules every service must satisfy (document.md section 11.3, AC-09 fitness function,
 * ADR-13 compliance check 1). Each service has one ArchitectureTest that calls {@link #verifyAll}.
 *
 * <ul>
 *   <li>domain must not depend on application, adapter, bootstrap or Spring
 *   <li>application must not depend on adapter or bootstrap
 *   <li>no service may import another service's code (only platform libraries or HTTP/events)
 * </ul>
 */
public final class HexagonalRules {
  private HexagonalRules() {}

  public static JavaClasses importServiceClasses(String rootPackage) {
    return new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(rootPackage);
  }

  public static void verifyAll(String servicePackage, String... otherServicePackages) {
    JavaClasses classes = importServiceClasses(servicePackage);
    domainIsIndependent(servicePackage).check(classes);
    applicationDoesNotDependOnAdapters(servicePackage).check(classes);
    for (String other : otherServicePackages) {
      noCrossServiceImports(servicePackage, other).check(classes);
    }
  }

  public static ArchRule domainIsIndependent(String servicePackage) {
    return noClasses()
        .that()
        .resideInAPackage(servicePackage + ".domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            servicePackage + ".application..",
            servicePackage + ".adapter..",
            servicePackage + ".bootstrap..",
            "org.springframework..",
            "jakarta.persistence..")
        .because("domain holds pure business rules (DP-01 Dependency Rule)");
  }

  public static ArchRule applicationDoesNotDependOnAdapters(String servicePackage) {
    return noClasses()
        .that()
        .resideInAPackage(servicePackage + ".application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(servicePackage + ".adapter..", servicePackage + ".bootstrap..")
        .because("application depends on ports it owns, adapters implement them (DIP)");
  }

  public static ArchRule noCrossServiceImports(String servicePackage, String otherServicePackage) {
    return noClasses()
        .that()
        .resideInAPackage(servicePackage + "..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(otherServicePackage + "..")
        .because("services talk through contracts only (ADR-01, ADR-10)");
  }
}
