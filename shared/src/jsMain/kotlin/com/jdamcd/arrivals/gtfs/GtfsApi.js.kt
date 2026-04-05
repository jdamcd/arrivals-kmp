package com.jdamcd.arrivals.gtfs

import com.google.transit.realtime.FeedMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes

internal actual fun createGtfsApi(client: HttpClient): GtfsApi = GtfsApiJs(client)

internal class GtfsApiJs(private val client: HttpClient) : GtfsApi {

    override suspend fun fetchFeedMessage(url: String, auth: ApiAuth?): FeedMessage {
        val bodyBytes = client.get(url) {
            auth.applyTo(this)
        }.bodyAsBytes()
        return FeedMessage.ADAPTER.decode(bodyBytes)
    }

    override suspend fun downloadSchedule(url: String, folder: String, auth: ApiAuth?): Unit =
        throw UnsupportedOperationException("GTFS schedule download is not supported in browser")

    override fun hasStops(): Boolean = false

    override fun stopsLastModifiedEpochSeconds(): Long = 0L

    override fun stopsSource(): String? = null

    override fun readStops(folder: String): String =
        throw UnsupportedOperationException("Filesystem access is not supported in browser")

    override fun readRoutes(folder: String): String? =
        throw UnsupportedOperationException("Filesystem access is not supported in browser")
}
