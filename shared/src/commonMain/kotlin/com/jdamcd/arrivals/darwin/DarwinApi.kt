package com.jdamcd.arrivals.darwin

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
import kotlinx.serialization.Serializable

internal class DarwinApi(private val client: HttpClient) {

    suspend fun fetchDepartures(crsCode: String, numRows: Int = 20): ApiDarwinBoard = request("$BASE_URL/departures/$crsCode/$numRows") {
        parameter("accessToken", BuildKonfig.DARWIN_ACCESS_TOKEN)
        parameter("expand", "false")
    }

    suspend fun searchCrs(query: String): List<ApiStationSearch> = request("$BASE_URL/crs/$query") {
        parameter("accessToken", BuildKonfig.DARWIN_ACCESS_TOKEN)
    }

    private suspend inline fun <reified T> request(
        url: String,
        crossinline parameters: HttpRequestBuilder.() -> Unit
    ): T {
        val response = try {
            client.get(url) { parameters() }
        } catch (_: Exception) {
            throw NoDataException("Can't connect to Darwin API")
        }
        checkResponse(response)
        return response.body()
    }

    private fun checkResponse(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            throw NoDataException(
                when (response.status) {
                    HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                        "Darwin access token error"
                    else ->
                        "Can't connect to Darwin API"
                }
            )
        }
    }
}

@Serializable
internal data class ApiDarwinBoard(
    val generatedAt: String,
    val locationName: String,
    val crs: String,
    val trainServices: List<ApiTrainService>? = null,
    val busServices: List<ApiTrainService>? = null,
    val nrccMessages: List<ApiNrccMessage>? = null
)

@Serializable
internal data class ApiTrainService(
    val serviceIdUrlSafe: String,
    val std: String? = null,
    val etd: String,
    val platform: String? = null,
    val operator: String,
    val operatorCode: String,
    val isCancelled: Boolean = false,
    val cancelReason: String? = null,
    val delayReason: String? = null,
    val destination: List<ApiCallingPoint>,
    val origin: List<ApiCallingPoint>? = null,
    val rsid: String? = null
)

@Serializable
internal data class ApiCallingPoint(
    val locationName: String,
    val crs: String,
    val via: String? = null
)

@Serializable
internal data class ApiNrccMessage(
    val value: String
)

@Serializable
internal data class ApiStationSearch(
    val crsCode: String,
    val stationName: String
)

private const val BASE_URL = "https://huxley2.azurewebsites.net"
