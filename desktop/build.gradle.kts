import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.viewmodel)
    implementation(libs.coroutines.swing)
    implementation(libs.koin)
    implementation(libs.snakeyaml)
}

compose.desktop {
    application {
        mainClass = "com.jdamcd.arrivals.desktop.MainKt"

        nativeDistributions {
            includeAllModules = true
            packageName = "ArrivalsDesktop"
            packageVersion = "1.0.0"
            targetFormats(TargetFormat.Dmg, TargetFormat.Deb, TargetFormat.Msi)
            macOS {
                iconFile.set(project.file("icon.icns"))
            }
            linux {
                iconFile.set(project.file("icon.png"))
            }
            windows {
                iconFile.set(project.file("icon.ico"))
            }
        }
    }
}
