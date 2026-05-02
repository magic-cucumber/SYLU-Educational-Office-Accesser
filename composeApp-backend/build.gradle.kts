val databaseVersion = (project.findProperty("database.version") as String).toInt()

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildConfig)
}

group = "top.kagg886.composeapp.backend"
version = "1.0"

android("composeapp.backend")

kotlin {
    library(
        android = {
            publishLibraryVariants("release")
        }
    )

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)

            api(libs.compose.foundation)
            api(libs.kermit)
            api(libs.androidx.paging)
            api(libs.room.runtime)
            api(libs.room.paging)

            implementation(libs.mkmb.core)

            implementation(project(":util"))
            api(project(":eoa-lib:network-html-api"))
            api(project(":eoa-lib:network-test-api"))
            api(project(":second-class"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}

buildConfig {
    packageName("top.kagg886.backend.config")
    buildConfigField("DATABASE_VERSION", databaseVersion)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    with(libs.room.compiler) {
        add("kspAndroid", this)
        add("kspJvm", this)
        add("kspIosArm64", this)
        add("kspIosSimulatorArm64", this)
    }
}
