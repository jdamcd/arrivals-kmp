package com.jdamcd.arrivals.gtfs

import com.google.transit.realtime.FeedMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter

internal sealed class ApiAuth {
    data class QueryParam(val name: String, val key: String) : ApiAuth()
    data class Header(val name: String, val key: String) : ApiAuth()

    companion object Companion {
        fun parse(key: String, param: String = ""): ApiAuth? {
            if (key.isEmpty()) return null
            if (param.isEmpty()) return QueryParam("api_key", key)
            if (param.startsWith("header:")) return Header(param.removePrefix("header:"), key)
            val name = param.removePrefix("query:")
            return QueryParam(name, key)
        }
    }
}

internal interface GtfsApi {
    suspend fun fetchFeedMessage(url: String, auth: ApiAuth? = null): FeedMessage
    suspend fun downloadSchedule(url: String, folder: String = "gtfs", auth: ApiAuth? = null)
    fun hasStops(): Boolean
    fun stopsLastModifiedEpochSeconds(): Long
    fun stopsSource(): String?
    fun readStops(folder: String = "gtfs"): String
    fun readRoutes(folder: String = "gtfs"): String?
}

internal expect fun createGtfsApi(client: HttpClient): GtfsApi

internal fun ApiAuth?.applyTo(builder: HttpRequestBuilder) {
    when (this) {
        is ApiAuth.QueryParam -> builder.parameter(name, key)
        is ApiAuth.Header -> builder.header(name, key)
        null -> {}
    }
}
