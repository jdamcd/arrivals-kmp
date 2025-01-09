plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    application
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.koin)
}

application {
    mainClass.set("com.jdamcd.arrivals.desktop.MainKt")
}
