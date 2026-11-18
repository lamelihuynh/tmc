plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":platform:common-kernel"))
    implementation(project(":platform:security"))
    // outbox for gate audit events (C-08) on the Gate Store
    implementation(project(":platform:messaging"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // Gate Store (profile "gate-store", ADR-14). DataSource auto-configuration stays off: GateStoreConfig
    // builds it only when that profile is active, so the inmem and local profiles need no database.
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(project(":platform:test-support"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.testcontainers.postgres)
}
