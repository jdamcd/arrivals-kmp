package com.jdamcd.arrivals.bvg

import com.jdamcd.arrivals.JsonApiClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal class BvgApi(client: HttpClient) :
    JsonApiClient(
        client = client,
        apiName = "BVG API"
    ) {

    suspend fun searchStops(query: String): List<ApiBvgLocation> = executeRequest("$BASE_URL/locations") {
        parameter("query", query)
        parameter("results", 10)
    }.body()

    suspend fun fetchDepartures(stopId: String, duration: Int = 15): ApiBvgDepartureResponse = executeRequest("$BASE_URL/stops/$stopId/departures") {
        parameter("duration", duration)
        parameter("suburban", true)
        parameter("subway", true)
        parameter("tram", true)
        parameter("bus", false)
        parameter("ferry", false)
        parameter("express", false)
        parameter("regional", false)
    }.body()

    suspend fun fetchStop(stopId: String): ApiBvgLocation = executeRequest("$BASE_URL/stops/$stopId").body()
}

@Serializable
internal data class ApiBvgDepartureResponse(
    val departures: List<ApiBvgDeparture>
)

@Serializable
internal data class ApiBvgDeparture(
    val tripId: String,
    val direction: String,
    val line: ApiBvgLine,
    @SerialName("when")
    val departureTime: String? = null,
    val plannedWhen: String,
    val delay: Int? = null,
    val platform: String? = null,
    val plannedPlatform: String? = null
)

@Serializable
internal data class ApiBvgLine(
    val name: String,
    val product: String
)

@Serializable
internal data class ApiBvgLocation(
    val type: String,
    val id: String? = null,
    val name: String? = null,
    val products: ApiBvgProducts? = null
)

@Serializable
internal data class ApiBvgProducts(
    val suburban: Boolean = false,
    val subway: Boolean = false,
    val tram: Boolean = false,
    val bus: Boolean = false,
    val ferry: Boolean = false,
    val express: Boolean = false,
    val regional: Boolean = false
)

private const val BASE_URL = "https://v6.bvg.transport.rest"
