plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
        mainRun {
            mainClass.set("com.jdamcd.arrivals.cli.CliKt")
        }
    }
    listOf(
        macosArm64(),
        macosX64(),
        linuxX64(),
        linuxArm64()
    ).forEach {
        it.binaries.executable {
            entryPoint = "com.jdamcd.arrivals.cli.main"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.koin)
            implementation(libs.kotlin.coroutines)
            implementation(libs.clikt)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
