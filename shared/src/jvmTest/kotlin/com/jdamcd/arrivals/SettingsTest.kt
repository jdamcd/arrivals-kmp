package com.jdamcd.arrivals

import com.jdamcd.arrivals.gtfs.system.Mta
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

class SettingsTest {

    private val settings = Settings()

    @BeforeTest
    fun setup() {
        settings.mode = SettingsConfig.MODE_TFL
        settings.stopId = "910GSHRDHST"
        settings.platform = "2"
        settings.line = "U8"
        settings.direction = "inbound"
        settings.gtfsStopsUpdated = 123456L
        settings.gtfsRealtime = "https://example.com/feeds/gtfs-ace"
        settings.gtfsSchedule = "https://example.com/data/google_transit.zip"
        settings.gtfsApiKey = "key123"
        settings.gtfsApiKeyParam = "token"
    }

    @Test
    fun `clearStopConfig resets stop fields`() {
        settings.clearStopConfig()

        settings.stopId shouldBe ""
        settings.platform shouldBe ""
        settings.line shouldBe ""
        settings.direction shouldBe ""
        settings.gtfsStopsUpdated shouldBe 0
    }

    @Test
    fun `clearStopConfig preserves mode and GTFS URLs`() {
        settings.clearStopConfig()

        settings.mode shouldBe SettingsConfig.MODE_TFL
        settings.gtfsRealtime shouldBe "https://example.com/feeds/gtfs-ace"
        settings.gtfsSchedule shouldBe "https://example.com/data/google_transit.zip"
    }

    @Test
    fun `saveGtfsConfig clears stop config and sets GTFS fields`() {
        settings.saveGtfsConfig(
            stopId = "B20N",
            realtimeUrl = "https://example.com/feeds/gtfs-bdfm",
            scheduleUrl = "https://example.com/data/google_transit.zip",
            apiKey = "newkey",
            apiKeyParam = "header:X-Api-Key"
        )

        settings.mode shouldBe SettingsConfig.MODE_GTFS
        settings.stopId shouldBe "B20N"
        settings.gtfsRealtime shouldBe "https://example.com/feeds/gtfs-bdfm"
        settings.gtfsSchedule shouldBe "https://example.com/data/google_transit.zip"
        settings.gtfsApiKey shouldBe "newkey"
        settings.gtfsApiKeyParam shouldBe "header:X-Api-Key"
        settings.platform shouldBe ""
        settings.line shouldBe ""
        settings.direction shouldBe ""
        settings.gtfsStopsUpdated shouldBe 0
    }

    @Test
    fun `saveGtfsConfig defaults optional params to empty`() {
        settings.saveGtfsConfig(
            stopId = "DALY",
            realtimeUrl = "https://example.com/feeds/bart-rt",
            scheduleUrl = "https://example.com/data/bart_transit.zip"
        )

        settings.gtfsApiKey shouldBe ""
        settings.gtfsApiKeyParam shouldBe ""
    }

    @Test
    fun `applyColdStart sets MTA ACE defaults`() {
        settings.applyColdStart()

        settings.mode shouldBe SettingsConfig.MODE_GTFS
        settings.stopId shouldBe "A42N"
        settings.gtfsRealtime shouldBe Mta.realtime["ACE"]
        settings.gtfsSchedule shouldBe Mta.SCHEDULE
    }
}
