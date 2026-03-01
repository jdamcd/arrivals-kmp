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
        feedMessage = TestHelper.resource("feed_message.pb").let {
            FeedMessage.ADAPTER.decode(it)
        }
    }

    @Test
    fun `finds all stops from feed message`() = runBlocking<Unit> {
        coEvery { api.downloadStops("schedule_url", "test_folder") } returns Fixtures.STOPS_CSV_1
        coEvery { api.fetchFeedMessage("realtime_url") } returns feedMessage

        val results = search.getStops("realtime_url")

        results.size shouldBe 6
        val first = results[0]
        first.id shouldBe "F27N"
        first.name shouldBe "Church Av (F27N)"
        first.isHub shouldBe false
    }

    @Test
    fun `passes api key to api calls`() = runBlocking<Unit> {
        val searchWithKey = GtfsStopSearch(api, "https://example.com/schedule", "test_folder", "test_key")
        coEvery { api.downloadStops("https://example.com/schedule", "test_folder", "test_key") } returns Fixtures.STOPS_CSV_1
        coEvery { api.fetchFeedMessage("https://example.com/feed", "test_key") } returns feedMessage

        val results = searchWithKey.getStops("https://example.com/feed")

        results.size shouldBe 6
    }
}
