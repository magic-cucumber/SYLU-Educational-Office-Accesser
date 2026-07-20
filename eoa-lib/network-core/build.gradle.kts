
plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)

    alias(libs.plugins.ksp)
}

group = "top.kagg886.eoa.network.core"
version = "1.0"

kotlin {
    library(module = "eoa.network.core", android = { androidResources.enable = true })

    sourceSets {

        commonMain {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.sweet.api.runtime)
                api(libs.kotlinx.datetime)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

dependencies {
    with(libs.sweet.compiler) {
        add("kspAndroid", this)
        add("kspJvm", this)
        add("kspIosArm64", this)
        add("kspIosSimulatorArm64", this)
    }
}
tasks.withType<org.gradle.api.tasks.Sync>().configureEach {
    if (name == "processAndroidMainJavaRes") {
        from(layout.buildDirectory.dir("generated/ksp/android/androidMain/resources"))
    }
}
