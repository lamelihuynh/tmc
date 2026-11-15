plugins { `java-library` }

// Spring JDBC, Spring AMQP and Jackson are compileOnly: a service that uses JdbcProcessedMessageStore,
// JdbcOutboxStore or AmqpEnvelopePublisher already has the matching Spring Boot starter, and a service
// that does not (the Docker-free local profile) does not pull them in through this library.
val springBootBom = "org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"

dependencies {
    api(project(":platform:common-kernel"))

    compileOnly(platform(springBootBom))
    compileOnly("org.springframework:spring-jdbc")
    compileOnly("org.springframework:spring-tx")
    compileOnly("org.springframework.amqp:spring-rabbit")
    compileOnly("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj)

    testImplementation(platform(springBootBom))
    testImplementation("org.springframework:spring-jdbc")
    testImplementation("org.springframework:spring-tx")
    testImplementation("org.springframework.amqp:spring-rabbit")
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.testcontainers.rabbit)
    testRuntimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("ch.qos.logback:logback-classic")
}
