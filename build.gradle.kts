// Plugins are managed by buildSrc
// All plugin versions and configurations are centralized in buildSrc/build.gradle.kts
plugins {
    alias(libs.plugins.benmanes.versions)
    alias(libs.plugins.version.catalog.update)
}

subprojects {
    tasks.matching { it.name == "jvmRun" && project.path != ":composeApp" }.configureEach {
        enabled = false
    }
}
