// Plugins are managed by buildSrc
// All plugin versions and configurations are centralized in buildSrc/build.gradle.kts
plugins {
    alias(libs.plugins.benmanes.versions)
    alias(libs.plugins.version.catalog.update)
}

// Work around Gradle retaining a stale problems report between invocations.
gradle.taskGraph.whenReady {
    rootProject.layout.buildDirectory.file("reports/problems/problems-report.html").get().asFile.delete()
}

subprojects {
    tasks.matching { it.name == "jvmRun" && project.path != ":composeApp" }.configureEach {
        enabled = false
    }
}
