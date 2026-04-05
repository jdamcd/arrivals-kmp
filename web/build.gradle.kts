import java.util.Base64

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

val generateFontData by tasks.registering {
    val fontFile = file("src/jsMain/resources/font/LUR.ttf")
    val outputDir = layout.buildDirectory.dir("generated/font")
    inputs.file(fontFile)
    outputs.dir(outputDir)
    doLast {
        val base64 = Base64.getEncoder().encodeToString(fontFile.readBytes())
        outputDir.get().asFile.mkdirs()
        outputDir.get().file("FontData.kt").asFile.writeText(
            """
            package com.jdamcd.arrivals.web

            internal const val LUR_FONT_BASE64: String =
                "$base64"
            """.trimIndent()
        )
    }
}

val generateMtaData by tasks.registering {
    val dataDir = file("data/mta")
    val outputDir = layout.buildDirectory.dir("generated/mta")
    inputs.dir(dataDir)
    outputs.dir(outputDir)
    doLast {
        val stops = File(dataDir, "stops.txt").readText()
        val routes = File(dataDir, "routes.txt").readText()
        outputDir.get().asFile.mkdirs()
        outputDir.get().file("MtaData.kt").asFile.writeText(
            buildString {
                appendLine("package com.jdamcd.arrivals.web")
                appendLine()
                appendLine("internal val MTA_STOPS_CSV: String = \"\"\"")
                appendLine(stops.replace("$", "\${'\$'}"))
                appendLine("\"\"\".trimIndent()")
                appendLine()
                appendLine("internal val MTA_ROUTES_CSV: String = \"\"\"")
                appendLine(routes.replace("$", "\${'\$'}"))
                appendLine("\"\"\".trimIndent()")
            }
        )
    }
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "arrivals.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        jsMain {
            kotlin.srcDir(generateFontData.map { layout.buildDirectory.dir("generated/font") })
            kotlin.srcDir(generateMtaData.map { layout.buildDirectory.dir("generated/mta") })
            dependencies {
                implementation(project(":shared"))
                implementation(libs.kotlin.coroutines)
                implementation(libs.kotlin.datetime)
                implementation(libs.ktor.client.js)
            }
        }
    }
}
