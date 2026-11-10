plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.spotless) apply false
}

allprojects {
    group = "vn.tmc"
    version = "0.1.0-SNAPSHOT"
    repositories { mavenCentral() }
}

subprojects {
    // Only modules that contain Java sources get the java plugin (see each module's build.gradle.kts).
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
        }

        apply(plugin = "com.diffplug.spotless")
        extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            java {
                target("src/**/*.java")
                googleJavaFormat("1.24.0").reflowLongStrings(false)
                removeUnusedImports()
                endWithNewline()
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.compilerArgs.add("-parameters")
        }

        tasks.withType<Test>().configureEach {
            testLogging {
                events("failed", "skipped")
                showStandardStreams = false
            }
        }

        // Only the regular `test` task excludes the tag; `integrationTest` below includes it.
        // (Applying the exclude to every Test task would also hit integrationTest and run nothing.)
        tasks.named<Test>("test") {
            useJUnitPlatform {
                // Integration tests (Testcontainers) are tagged "integration" and run via `integrationTest`.
                excludeTags("integration")
            }
        }

        tasks.register<Test>("integrationTest") {
            description = "Runs tests tagged 'integration' (needs Docker for Testcontainers)."
            group = "verification"
            val sourceSets = project.extensions.getByType<SourceSetContainer>()
            testClassesDirs = sourceSets["test"].output.classesDirs
            classpath = sourceSets["test"].runtimeClasspath
            useJUnitPlatform { includeTags("integration") }
            shouldRunAfter("test")
        }
    }
}
