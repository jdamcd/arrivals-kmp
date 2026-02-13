package com.jdamcd.arrivals.tfl

import com.jdamcd.arrivals.BuildKonfig
import com.jdamcd.arrivals.NoDataException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.Serializable

internal class TflApi(private val client: HttpClient) {

    suspend fun fetchArrivals(station: String): List<ApiArrival> = try {
        request("$BASE_URL/StopPoint/$station/Arrivals") {
            parameter("app_key", BuildKonfig.TFL_APP_KEY)
        }
    } catch (_: JsonConvertException) {
        // API returns empty body for terminal stations
        emptyList()
    }

    suspend fun searchStations(query: String): ApiSearchResult = request("$BASE_URL/StopPoint/Search") {
        parameter("app_key", BuildKonfig.TFL_APP_KEY)
        parameter("query", query)
        parameter("modes", "dlr,elizabeth-line,overground,tube,tram")
        parameter("tflOperatedNationalRailStationsOnly", true)
    }

    suspend fun stopDetails(id: String): ApiStopPoint = request("$BASE_URL/StopPoint/$id") {
        parameter("app_key", BuildKonfig.TFL_APP_KEY)
    }

    private suspend inline fun <reified T> request(
        url: String,
        crossinline parameters: HttpRequestBuilder.() -> Unit
    ): T {
        val response = try {
            client.get(url) { parameters() }
        } catch (_: Exception) {
            throw NoDataException("Can't connect to TfL API")
        }
        checkResponse(response)
        return response.body()
    }

    private fun checkResponse(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            throw NoDataException(
                when (response.status) {
                    HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                        "TfL API app key error"

                    else ->
                        "Can't connect to TfL API"
                }
            )
        }
    }
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
