package com.jdamcd.arrivals.gtfs

import com.google.transit.realtime.FeedMessage
import com.jdamcd.arrivals.Fixtures
import com.jdamcd.arrivals.TestHelper
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test

class GtfsStopSearchTest {

    private val api = mockk<GtfsApi>()
    private val search = GtfsStopSearch(api, "schedule_url", "test_folder")

    private lateinit var feedMessage: FeedMessage

    @BeforeTest
    fun setup() {
        feedMessage = TestHelper.resource("feed_message.bin").let {
            FeedMessage.ADAPTER.decode(it)
        }
    }

    @Test
    fun `finds all stops from feed message`() = runBlocking<Unit> {
        coEvery { api.downloadSchedule("schedule_url", "test_folder") } returns Fixtures.STOPS_CSV_1
        coEvery { api.fetchFeedMessage("realtime_url") } returns feedMessage

        val results = search.getStops("realtime_url")

        results.size shouldBe 6
        val first = results[0]
        first.id shouldBe "F27N"
        first.name shouldBe "Church Av (F27N)"
        first.isHub shouldBe false
    }

    @Test
    fun `natural sort key orders numbers numerically`() {
        val names = listOf("104 St", "7 Av", "Canal St", "14 St", "2 Av")
        val sorted = names.sortedBy { naturalSortKey(it) }
        sorted shouldBe listOf("2 Av", "7 Av", "14 St", "104 St", "Canal St")
    }

    @Test
    fun `passes auth to api calls`() = runBlocking<Unit> {
        val auth = ApiAuth.QueryParam("api_key", "test_key")
        val searchWithKey = GtfsStopSearch(api, "https://example.com/schedule", "test_folder", auth)
        coEvery { api.downloadSchedule("https://example.com/schedule", "test_folder", auth) } returns Fixtures.STOPS_CSV_1
        coEvery { api.fetchFeedMessage("https://example.com/feed", auth) } returns feedMessage

        val results = searchWithKey.getStops("https://example.com/feed")

        results.size shouldBe 6
    }
}
