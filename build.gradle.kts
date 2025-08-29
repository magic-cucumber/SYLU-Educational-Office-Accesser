// Plugins are managed by buildSrc
// All plugin versions and configurations are centralized in buildSrc/build.gradle.kts
plugins {
    alias(libs.plugins.benmanes.versions)
    alias(libs.plugins.version.catalog.update)
}
