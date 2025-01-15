import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.viewmodel)
    implementation(libs.coroutines.swing)
    implementation(libs.koin)
}

compose.desktop {
    application {
        mainClass = "com.jdamcd.arrivals.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            includeAllModules = true
            packageName = "ArrivalsDesktop"
            packageVersion = "1.0.0"
        }
    }
}
