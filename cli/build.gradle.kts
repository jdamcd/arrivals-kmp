plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.jdamcd.arrivals.cli.CliKt")
    applicationName = "arrivals"
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.koin)
    implementation(libs.kotlin.coroutines)
    implementation(libs.clikt)
    implementation(libs.kotlinx.serialization.json)
}
