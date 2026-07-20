

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)


    alias(libs.plugins.ksp)
}

group = "top.kagg886.sylu_eoa.api.test"
version = "1.0"

kotlin {
    library(module = "sylu_eoa.api.test", android = { androidResources.enable = true })

    sourceSets {

        commonMain.dependencies {
            api(project.dependencies.project(":eoa-lib:network-core"))
            implementation(libs.sweet.api.runtime)
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
