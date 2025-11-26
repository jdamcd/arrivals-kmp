package com.jdamcd.arrivals.darwin

import com.jdamcd.arrivals.BuildKonfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

internal class DarwinApi(private val client: HttpClient) {

    suspend fun fetchDepartures(crsCode: String, numRows: Int = 20): ApiDarwinBoard = client.get("$BASE_URL/departures/$crsCode/$numRows") {
        parameter("accessToken", BuildKonfig.DARWIN_ACCESS_TOKEN)
        parameter("expand", "false")
    }.body()

    suspend fun searchCrs(query: String): List<ApiStationSearch> = client.get("$BASE_URL/crs/$query") {
        parameter("accessToken", BuildKonfig.DARWIN_ACCESS_TOKEN)
    }.body()
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
