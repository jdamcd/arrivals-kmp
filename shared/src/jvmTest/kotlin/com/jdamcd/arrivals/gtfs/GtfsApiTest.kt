package com.jdamcd.arrivals.gtfs

import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.TestHelper
import com.jdamcd.arrivals.bytesResponse
import com.jdamcd.arrivals.mockClient
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class GtfsApiTest {

    private val feedUrl = "https://feed.example.com/gtfs"
    private val scheduleUrl = "https://feed.example.com/schedule.zip"

    @Test
    fun `fetchFeedMessage decodes protobuf response`() = runBlocking<Unit> {
        val api = GtfsApi(
            mockClient {
                bytesResponse(TestHelper.resource("feed_message.bin"))
            }
        )

        val feed = api.fetchFeedMessage(feedUrl)

        feed.entity.isEmpty() shouldBe false
    }

    @Test
    fun `fetchFeedMessage throws error message on 4xx`() = runBlocking<Unit> {
        val api = GtfsApi(
            mockClient { bytesResponse(ByteArray(0), HttpStatusCode.Forbidden) }
        )

        val e = assertFailsWith<NoDataException> {
            api.fetchFeedMessage(feedUrl)
        }
        e.message shouldBe "GTFS feed error"
    }

    @Test
    fun `fetchFeedMessage throws connection message on 5xx`() = runBlocking<Unit> {
        val api = GtfsApi(
            mockClient { bytesResponse(ByteArray(0), HttpStatusCode.InternalServerError) }
        )

        val e = assertFailsWith<NoDataException> {
            api.fetchFeedMessage(feedUrl)
        }
        e.message shouldBe "Can't connect to GTFS feed"
    }

    @Test
    fun `fetchFeedMessage throws connection message when request fails`() = runBlocking<Unit> {
        val api = GtfsApi(
            mockClient { throw IOException("oh no") }
        )

        val e = assertFailsWith<NoDataException> {
            api.fetchFeedMessage(feedUrl)
        }
        e.message shouldBe "Can't connect to GTFS feed"
    }

    @Test
    fun `downloadSchedule throws error message on 4xx before unpacking`() = runBlocking<Unit> {
        val api = GtfsApi(
            mockClient { bytesResponse(ByteArray(0), HttpStatusCode.Forbidden) }
        )

        val e = assertFailsWith<NoDataException> {
            api.downloadSchedule(scheduleUrl)
        }
        e.message shouldBe "GTFS feed error"
    }

    @Test
    fun `downloadSchedule throws connection message on 5xx before unpacking`() = runBlocking<Unit> {
        val api = GtfsApi(
            mockClient { bytesResponse(ByteArray(0), HttpStatusCode.InternalServerError) }
        )

        val e = assertFailsWith<NoDataException> {
            api.downloadSchedule(scheduleUrl)
        }
        e.message shouldBe "Can't connect to GTFS feed"
    }
}
