package com.jdamcd.arrivals.darwin

import com.jdamcd.arrivals.BuildKonfig
import com.jdamcd.arrivals.JsonApiClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

internal class DarwinApi(client: HttpClient) :
    JsonApiClient(
        client = client,
        apiName = "Darwin API"
    ) {

    suspend fun fetchDepartures(crsCode: String, numRows: Int = 20): ApiDarwinBoard = executeRequest("$BASE_URL/departures/$crsCode/$numRows") {
        parameter("accessToken", BuildKonfig.DARWIN_KEY)
        parameter("expand", "false")
    }.body()

    suspend fun searchCrs(query: String): List<ApiStationSearch> = executeRequest("$BASE_URL/crs/$query") {
        parameter("accessToken", BuildKonfig.DARWIN_KEY)
    }.body()
}

@Serializable
internal data class ApiDarwinBoard(
    val generatedAt: String,
    val locationName: String,
    val crs: String,
    val trainServices: List<ApiTrainService>? = null
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
    val destination: List<ApiCallingPoint>
)

@Serializable
internal data class ApiCallingPoint(
    val locationName: String,
    val crs: String,
    val via: String? = null
)

@Serializable
internal data class ApiStationSearch(
    val crsCode: String,
    val stationName: String
)

private const val BASE_URL = "https://huxley2.azurewebsites.net"
