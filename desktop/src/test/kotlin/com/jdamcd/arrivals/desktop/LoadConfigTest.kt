package com.jdamcd.arrivals.desktop

import com.jdamcd.arrivals.InMemorySettings
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.SettingsConfig
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test

class LoadConfigTest {

    private val settings: Settings = InMemorySettings()
    private val configFile: File = File.createTempFile("arrivals", ".yml")

    @AfterTest
    fun cleanup() {
        configFile.delete()
    }

    private fun writeConfig(yaml: String) = configFile.writeText(yaml)

    @Test
    fun `full config applies all fields`() {
        writeConfig(
            """
            mode: darwin
            stop: CLJ
            platform: "5"
            line: U8
            direction: inbound
            gtfs_realtime: https://example.com/rt
            gtfs_schedule: https://example.com/schedule.zip
            gtfs_api_key: secret
            gtfs_api_key_param: header:Authorization
            """.trimIndent()
        )

        loadConfig(settings, configFile)

        settings.mode shouldBe "darwin"
        settings.stopId shouldBe "CLJ"
        settings.platform shouldBe "5"
        settings.line shouldBe "U8"
        settings.direction shouldBe "inbound"
        settings.gtfsRealtime shouldBe "https://example.com/rt"
        settings.gtfsSchedule shouldBe "https://example.com/schedule.zip"
        settings.gtfsApiKey shouldBe "secret"
        settings.gtfsApiKeyParam shouldBe "header:Authorization"
    }

    @Test
    fun `partial config clears absent stop fields`() {
        settings.platform = "99"
        settings.line = "STALE"
        writeConfig(
            """
            mode: bvg
            stop: "900013102"
            """.trimIndent()
        )

        loadConfig(settings, configFile)

        settings.mode shouldBe "bvg"
        settings.stopId shouldBe "900013102"
        settings.platform shouldBe ""
        settings.line shouldBe ""
    }

    @Test
    fun `non-map config leaves settings untouched`() {
        settings.mode = "tfl"
        settings.stopId = "910GSHRDHST"
        settings.platform = "2"
        writeConfig("- just\n- a\n- list")

        loadConfig(settings, configFile)

        settings.mode shouldBe "tfl"
        settings.stopId shouldBe "910GSHRDHST"
        settings.platform shouldBe "2"
    }

    @Test
    fun `empty config leaves settings untouched`() {
        settings.stopId = "910GSHRDHST"
        writeConfig("")

        loadConfig(settings, configFile)

        settings.stopId shouldBe "910GSHRDHST"
    }

    @Test
    fun `missing file applies cold start`() {
        val missing = File(configFile.parentFile, "does-not-exist.yml")

        loadConfig(settings, missing)

        settings.mode shouldBe SettingsConfig.MODE_GTFS
        settings.stopId shouldBe "A42N"
    }
}
