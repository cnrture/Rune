rootProject.name = "Getcontact-Android-Studio-Plugin-Project"

pluginManagement {
    plugins {
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
    }
}
