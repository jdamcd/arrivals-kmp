package com.jdamcd.arrivals.gtfs

import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.TestHelper
import com.jdamcd.arrivals.bytesResponse
import com.jdamcd.arrivals.mockClient
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertFailsWith

class GtfsApiTest {

    private val feedUrl = "https://feed.example.com/gtfs"
    private val scheduleUrl = "https://feed.example.com/schedule.zip"
    private val baseDir = System.getProperty("java.io.tmpdir")

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

    @Test
    fun `downloadSchedule unpacks schedule into the target folder`() = runBlocking<Unit> {
        val folder = "test_unpack"
        val api = GtfsApi(
            mockClient { bytesResponse(zipBytes("stops.txt" to STOPS, "routes.txt" to ROUTES)) }
        )
        try {
            api.downloadSchedule(scheduleUrl, folder)

            api.readStops(folder) shouldBe STOPS
            api.readRoutes(folder) shouldBe ROUTES
        } finally {
            cleanup(folder)
        }
    }

    @Test
    fun `downloads to different folders keep separate schedules`() = runBlocking<Unit> {
        val api = GtfsApi(
            mockClient { request ->
                val stops = if (request.url.toString().contains("mta")) {
                    "stop_id,stop_name\nM1,MTA Stop\n"
                } else {
                    "stop_id,stop_name\nB1,BART Stop\n"
                }
                bytesResponse(zipBytes("stops.txt" to stops))
            }
        )
        try {
            api.downloadSchedule("https://example.com/mta.zip", "test_mta")
            api.downloadSchedule("https://example.com/bart.zip", "test_bart")

            api.readStops("test_mta") shouldContain "MTA Stop"
            api.readStops("test_bart") shouldContain "BART Stop"
        } finally {
            cleanup("test_mta")
            cleanup("test_bart")
        }
    }

    @Test
    fun `downloadSchedule uses a folder-specific temp zip, not the shared path`() = runBlocking<Unit> {
        val folder = "test_isolation"
        val sharedTempZip = "$baseDir/gtfs.zip".toPath()
        val api = GtfsApi(
            mockClient { bytesResponse(zipBytes("stops.txt" to STOPS)) }
        )
        try {
            FileSystem.SYSTEM.write(sharedTempZip) { writeUtf8("sentinel") }

            api.downloadSchedule(scheduleUrl, folder)

            // A non-default folder download must not touch the legacy shared temp path
            FileSystem.SYSTEM.exists(sharedTempZip) shouldBe true
            FileSystem.SYSTEM.read(sharedTempZip) { readUtf8() } shouldBe "sentinel"
        } finally {
            FileSystem.SYSTEM.delete(sharedTempZip)
            cleanup(folder)
        }
    }

    @Test
    fun `downloadSchedule removes the temp zip after success`() = runBlocking<Unit> {
        val folder = "test_cleanup_ok"
        val api = GtfsApi(
            mockClient { bytesResponse(zipBytes("stops.txt" to STOPS)) }
        )
        try {
            api.downloadSchedule(scheduleUrl, folder)

            FileSystem.SYSTEM.exists(tempZip(folder)) shouldBe false
        } finally {
            cleanup(folder)
        }
    }

    @Test
    fun `downloadSchedule removes the temp zip when unpacking fails`() = runBlocking<Unit> {
        val folder = "test_cleanup_fail"
        val api = GtfsApi(
            mockClient { bytesResponse(byteArrayOf(1, 2, 3, 4)) } // 200, but not a valid zip
        )
        try {
            assertFailsWith<Exception> {
                api.downloadSchedule(scheduleUrl, folder)
            }

            FileSystem.SYSTEM.exists(tempZip(folder)) shouldBe false
        } finally {
            cleanup(folder)
        }
    }

    private fun tempZip(folder: String) = "$baseDir/$folder.zip".toPath()

    private fun cleanup(folder: String) {
        FileSystem.SYSTEM.deleteRecursively("$baseDir/$folder".toPath())
        FileSystem.SYSTEM.delete(tempZip(folder))
    }

    private fun zipBytes(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.encodeToByteArray())
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private companion object {
        const val STOPS = "stop_id,stop_name\nG28N,Nassau Av\n"
        const val ROUTES = "route_id,route_color\nG,6CBE45\n"
    }
}
