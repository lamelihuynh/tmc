plugins {
    `java-library`
    // TestJwtKeys / TestTokens: testImplementation(testFixtures(project(":platform:security")))
    `java-test-fixtures`
}

dependencies {
    api(project(":platform:common-kernel"))
    // Nimbus is part of this module's API: JwtPrincipalVerifier and AccessTokenSigner wrap it.
    api(libs.nimbus.jose.jwt)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj)
}
