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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
class GtfsArrivalsTest {

    private val api = mockk<GtfsApi>(relaxUnitFun = true)
    private val clock = mockk<Clock>()
    private val settings = Settings()
    private val arrivals = GtfsArrivals(api, clock, settings)

    private val realtimeUrl = "https://api-endpoint.mta.info/Dataservice/mtagtfsfeeds/nyct%2Fgtfs-g"
    private val fetchTime = 1734717694L
    private lateinit var feedMessage: FeedMessage

    @BeforeTest
    fun setup() {
        settings.gtfsRealtime = realtimeUrl
        settings.gtfsSchedule = "schedule_url"
        settings.stopId = "G28S"
        settings.gtfsStopsUpdated = fetchTime - 1000
        every { api.hasStops() } returns true
        every { api.stopsSource() } returns "schedule_url"

        feedMessage = TestHelper.resource("feed_message.bin").let {
            FeedMessage.ADAPTER.decode(it)
        }
    }

    @Test
    fun `fetches latest arrivals`() = runBlocking<Unit> {
        coEvery { api.fetchFeedMessage(realtimeUrl) } returns feedMessage
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        every { api.readStops() } returns Fixtures.STOPS_CSV_1

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 3
        val first = latest.arrivals[0]
        first.destination shouldBe "Church Av"
        first.line shouldBe "G"
        first.lineColor shouldBe "6CBE45"
        first.time shouldBe "Due"
        first.secondsToStop shouldBe 30
        first.realtime shouldBe true
        val second = latest.arrivals[1]
        second.displayName shouldBe "G - Church Av"
        second.time shouldBe "8 min"
        second.secondsToStop shouldBe 506
        val third = latest.arrivals[2]
        third.displayName shouldBe "G - Church Av"
        third.time shouldBe "16 min"
        third.secondsToStop shouldBe 956
    }

    @Test
    fun `non-MTA feed uses route ID without colour`() = runBlocking<Unit> {
        settings.gtfsRealtime = "https://other-feed.example.com/gtfs"
        coEvery { api.fetchFeedMessage("https://other-feed.example.com/gtfs") } returns feedMessage
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        every { api.readStops() } returns Fixtures.STOPS_CSV_1

        val latest = arrivals.latest()

        val first = latest.arrivals[0]
        first.line shouldBe "G"
        first.lineColor shouldBe null
    }

    @Test
    fun `fetches latest arrivals with station`() = runBlocking<Unit> {
        coEvery { api.fetchFeedMessage(realtimeUrl) } returns feedMessage
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        every { api.readStops() } returns Fixtures.STOPS_CSV_1

        val latest = arrivals.latest()

        latest.station shouldBe "Nassau Av"
    }

    @Test
    fun `throws NoDataException if no arrivals match stop`() = runBlocking<Unit> {
        settings.stopId = "1234"
        coEvery { api.fetchFeedMessage(realtimeUrl) } returns feedMessage
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
        coEvery { api.fetchFeedMessage(realtimeUrl) } returns feedMessage

        val latest = arrivals.latest()

        coVerify { api.downloadStops("schedule_url") }
        latest.arrivals shouldHaveSize 3
    }

    @Test
    fun `passes api key as default query param`() = runBlocking<Unit> {
        val expectedAuth = ApiAuth.QueryParam("api_key", "test_key")
        settings.gtfsApiKey = "test_key"
        settings.gtfsStopsUpdated = fetchTime - 172801
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        coEvery { api.downloadStops("schedule_url", auth = expectedAuth) } returns Fixtures.STOPS_CSV_1
        coEvery { api.fetchFeedMessage(realtimeUrl, expectedAuth) } returns feedMessage

        val latest = arrivals.latest()

        coVerify { api.downloadStops("schedule_url", auth = expectedAuth) }
        coVerify { api.fetchFeedMessage(realtimeUrl, expectedAuth) }
        latest.arrivals shouldHaveSize 3
    }

    @Test
    fun `passes api key as header auth`() = runBlocking<Unit> {
        val expectedAuth = ApiAuth.Header("Authorization", "apikey mytoken")
        settings.gtfsApiKey = "apikey mytoken"
        settings.gtfsApiKeyParam = "header:Authorization"
        settings.gtfsStopsUpdated = fetchTime - 172801
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        coEvery { api.downloadStops("schedule_url", auth = expectedAuth) } returns Fixtures.STOPS_CSV_1
        coEvery { api.fetchFeedMessage(realtimeUrl, expectedAuth) } returns feedMessage

        val latest = arrivals.latest()

        coVerify { api.downloadStops("schedule_url", auth = expectedAuth) }
        coVerify { api.fetchFeedMessage(realtimeUrl, expectedAuth) }
        latest.arrivals shouldHaveSize 3
    }

    @Test
    fun `passes api key with custom query param`() = runBlocking<Unit> {
        val expectedAuth = ApiAuth.QueryParam("app_id", "test_key")
        settings.gtfsApiKey = "test_key"
        settings.gtfsApiKeyParam = "app_id"
        settings.gtfsStopsUpdated = fetchTime - 172801
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        coEvery { api.downloadStops("schedule_url", auth = expectedAuth) } returns Fixtures.STOPS_CSV_1
        coEvery { api.fetchFeedMessage(realtimeUrl, expectedAuth) } returns feedMessage

        val latest = arrivals.latest()

        coVerify { api.downloadStops("schedule_url", auth = expectedAuth) }
        coVerify { api.fetchFeedMessage(realtimeUrl, expectedAuth) }
        latest.arrivals shouldHaveSize 3
    }

    @Test
    fun `uses file modification time when settings timestamp is zero`() = runBlocking<Unit> {
        settings.gtfsStopsUpdated = 0L
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        every { api.stopsLastModifiedEpochSeconds() } returns fetchTime - 1000
        coEvery { api.fetchFeedMessage(realtimeUrl) } returns feedMessage
        every { api.readStops() } returns Fixtures.STOPS_CSV_1

        val latest = arrivals.latest()

        coVerify(exactly = 0) { api.downloadStops(any(), any(), any()) }
        latest.arrivals shouldHaveSize 3
    }

    @Test
    fun `downloads stops when file modification time is stale and settings timestamp is zero`() = runBlocking<Unit> {
        settings.gtfsStopsUpdated = 0L
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        every { api.stopsLastModifiedEpochSeconds() } returns fetchTime - 172801
        coEvery { api.downloadStops("schedule_url") } returns Fixtures.STOPS_CSV_1
        coEvery { api.fetchFeedMessage(realtimeUrl) } returns feedMessage

        val latest = arrivals.latest()

        coVerify { api.downloadStops("schedule_url") }
        latest.arrivals shouldHaveSize 3
    }

    @Test
    fun `downloads stops when cached source does not match schedule url`() = runBlocking<Unit> {
        settings.gtfsStopsUpdated = 0L
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        every { api.stopsSource() } returns "old_schedule_url"
        coEvery { api.downloadStops("schedule_url") } returns Fixtures.STOPS_CSV_1
        coEvery { api.fetchFeedMessage(realtimeUrl) } returns feedMessage

        val latest = arrivals.latest()

        coVerify { api.downloadStops("schedule_url") }
        latest.arrivals shouldHaveSize 3
    }

    @Test
    fun `downloads stops when cached source does not match even with fresh timestamp`() = runBlocking<Unit> {
        settings.gtfsStopsUpdated = fetchTime - 1000
        every { clock.now() } returns Instant.fromEpochSeconds(fetchTime)
        every { api.stopsSource() } returns "old_schedule_url"
        coEvery { api.downloadStops("schedule_url") } returns Fixtures.STOPS_CSV_1
        coEvery { api.fetchFeedMessage(realtimeUrl) } returns feedMessage

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
