package com.jdamcd.arrivals.tfl

import com.jdamcd.arrivals.BuildKonfig
import com.jdamcd.arrivals.JsonApiClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.Serializable

internal class TflApi(client: HttpClient) :
    JsonApiClient(
        client = client,
        apiName = "TfL API"
    ) {

    suspend fun fetchArrivals(station: String): List<ApiArrival> = try {
        executeRequest("$BASE_URL/StopPoint/$station/Arrivals") {
            parameter("app_key", BuildKonfig.TFL_KEY)
        }.body()
    } catch (_: JsonConvertException) {
        // API returns empty body for terminal stations
        emptyList()
    }

    suspend fun searchStations(query: String): ApiSearchResult = executeRequest("$BASE_URL/StopPoint/Search") {
        parameter("app_key", BuildKonfig.TFL_KEY)
        parameter("query", query)
        parameter("modes", "dlr,elizabeth-line,overground,tube,tram")
        parameter("tflOperatedNationalRailStationsOnly", true)
    }.body()

    suspend fun stopDetails(id: String): ApiStopPoint = executeRequest("$BASE_URL/StopPoint/$id") {
        parameter("app_key", BuildKonfig.TFL_KEY)
    }.body()
}

@Serializable
internal data class ApiArrival(
    val id: Int,
    val stationName: String,
    val platformName: String,
    val direction: String,
    val destinationName: String,
    val timeToStation: Int
)

@Serializable
internal data class ApiSearchResult(
    val matches: List<ApiMatchedStop>
)

@Serializable
internal data class ApiMatchedStop(
    val id: String,
    val name: String
)

@Serializable
internal data class ApiStopPoint(
    val commonName: String,
    val naptanId: String,
    val stopType: String,
    val children: List<ApiStopPoint>
)

private const val BASE_URL = "https://api.tfl.gov.uk"
