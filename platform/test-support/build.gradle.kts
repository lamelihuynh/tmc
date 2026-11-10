plugins { `java-library` }

dependencies {
    api(libs.archunit.junit5)
    api(platform(libs.junit.bom))
    api("org.junit.jupiter:junit-jupiter")
    api(libs.assertj)
    api(libs.testcontainers.junit)
}
