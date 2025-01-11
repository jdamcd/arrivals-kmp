import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.wire)
}

kotlin {
    jvm()
    listOf(
        macosArm64(),
        macosX64()
    ).forEach {
        it.binaries.framework {
            baseName = "ArrivalsLib"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.ktor.common)
            implementation(libs.kotlin.datetime)
            implementation(libs.kotlin.coroutines)
            implementation(libs.wire.runtime)
            implementation(libs.okio)
            implementation(libs.koin)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.jvm)
            implementation(libs.logging.nop)
        }

        jvmTest.dependencies {
            implementation(libs.coroutines.test)
            implementation(libs.mockk)
        }

        macosMain.dependencies {
            implementation(libs.ktor.client.macos)
        }
    }
}

wire {
    kotlin {}
    sourcePath {
        srcDir("src/commonMain/proto")
    }
}

@Suppress("TooGenericExceptionCaught")
configure<BuildKonfigExtension> {
    packageName = "com.jdamcd.arrivals"

    val props = Properties()
    try {
        props.load(file("secret.properties").inputStream())
    } catch (_: Exception) {}

    defaultConfigs {
        buildConfigField(
            FieldSpec.Type.STRING,
            "TFL_APP_KEY",
            props["tfl_app_key"]?.toString() ?: ""
        )
    }
}
