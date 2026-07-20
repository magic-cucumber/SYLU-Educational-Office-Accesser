rootProject.name = "SYLU-EOA"

pluginManagement {
    repositories {
        google {
            content {
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        mavenCentral()
        mavenLocal()
    }
}
plugins {
    //https://github.com/JetBrains/compose-hot-reload?tab=readme-ov-file#set-up-automatic-provisioning-of-the-jetbrains-runtime-jbr-via-gradle
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

include(":composeApp")
include(":androidApp")
include(":composeApp-backend")
include(":widgetApp")
include(":eoa-lib:network-core")
include(":eoa-lib:network-html-api")
include(":eoa-lib:network-test-api")
include(":second-class")
include(":util")

include(":lib:ics-generator")
include(":lib:calender-exporter-v2")
include(":lib:rsa")
include(":lib:ktor-platform-engine")
