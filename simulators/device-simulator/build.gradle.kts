plugins {
    java
    application
}

dependencies {
    implementation(libs.nimbus.jose.jwt)
    implementation(libs.picocli)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj)
}

application {
    mainClass.set("vn.tmc.devicesim.DeviceSimulatorCli")
    applicationName = "device-sim"
}
