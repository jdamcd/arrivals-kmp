package com.jdamcd.arrivals.gtfs

import com.google.transit.realtime.FeedMessage
import com.jdamcd.arrivals.Fixtures
import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.TestHelper
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class GtfsArrivalsTest {

    private val api = mockk<GtfsApi>()
    private val clock = mockk<Clock>()
    private val settings = Settings()
    private val arrivals = GtfsArrivals(api, clock, settings)

    private val fetchTime = 1734717694L
    private lateinit var feedMessage: FeedMessage

    @BeforeTest
    fun setup() {
        settings.gtfsRealtime = "realtime_url"
        settings.gtfsSchedule = "schedule_url"
        settings.gtfsStop = "G28S"
        settings.gtfsStopsUpdated = fetchTime - 1000

        feedMessage = TestHelper.resource("feed_message.pb").let {
            FeedMessage.ADAPTER.decode(it)
        }
    }

    @Test
    fun `fetches latest arrivals`() = runBlocking<Unit> {
        coEvery { api.fetchFeedMessage("realtime_url") } returns feedMessage
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        every { api.readStops() } returns Fixtures.STOPS_CSV_1

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 3
        val first = latest.arrivals[0]
        first.destination shouldBe "G - Church Av"
        first.time shouldBe "Due"
        first.secondsToStop shouldBe 30
        val second = latest.arrivals[1]
        second.destination shouldBe "G - Church Av"
        second.time shouldBe "8 min"
        second.secondsToStop shouldBe 506
        val third = latest.arrivals[2]
        third.destination shouldBe "G - Church Av"
        third.time shouldBe "16 min"
        third.secondsToStop shouldBe 956
    }

    @Test
    fun `fetches latest arrivals with station`() = runBlocking<Unit> {
        coEvery { api.fetchFeedMessage("realtime_url") } returns feedMessage
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        every { api.readStops() } returns Fixtures.STOPS_CSV_1

        val latest = arrivals.latest()

        latest.station shouldBe "Nassau Av"
    }

    @Test
    fun `throws NoDataException if no arrivals match stop`() = runBlocking<Unit> {
        settings.gtfsStop = "1234"
        coEvery { api.fetchFeedMessage("realtime_url") } returns feedMessage
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        every { api.readStops() } returns Fixtures.STOPS_CSV_1

        assertFailsWith<NoDataException> {
            arrivals.latest()
        }
    }

    @Test
    fun `updates stops when stale`() = runBlocking<Unit> {
        settings.gtfsStopsUpdated = fetchTime - 172801
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        coEvery { api.downloadStops("schedule_url") } returns Fixtures.STOPS_CSV_1
        coEvery { api.fetchFeedMessage("realtime_url") } returns feedMessage

        val latest = arrivals.latest()

        coVerify { api.downloadStops("schedule_url") }
        latest.arrivals shouldHaveSize 3
    }

    @Test
    fun `throws NoDataException if stops fail to load`() = runBlocking<Unit> {
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        coEvery { api.downloadStops("schedule_url") } throws Exception()

        assertFailsWith<NoDataException> {
            arrivals.latest()
        }
    }
}
