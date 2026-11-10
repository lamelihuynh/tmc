plugins {
    java
    application
}

dependencies {
    implementation(project(":platform:common-kernel"))
    implementation(libs.nimbus.jose.jwt)
    implementation(libs.picocli)
    implementation(libs.sqlite.jdbc)
    implementation(libs.jackson.databind)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj)
}

application {
    mainClass.set("vn.tmc.kiosksim.KioskSimulatorCli")
    applicationName = "kiosk-sim"
}
