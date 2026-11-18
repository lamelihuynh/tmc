plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":platform:common-kernel"))
    implementation(project(":platform:security"))
    implementation(project(":platform:messaging"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.nimbus.jose.jwt)

    testImplementation(project(":platform:test-support"))
    testImplementation(testFixtures(project(":platform:security")))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
