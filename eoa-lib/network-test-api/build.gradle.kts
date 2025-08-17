

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)


    alias(libs.plugins.ksp)
}

group = "top.kagg886.sylu_eoa.api.test"
version = "1.0"

android("sylu_eoa.api.test")

kotlin {
    library(
        android = {
            publishLibraryVariants("release")
        }
    )

    sourceSets {
        commonMain.dependencies {
            api(project(":eoa-lib:network-core"))
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
