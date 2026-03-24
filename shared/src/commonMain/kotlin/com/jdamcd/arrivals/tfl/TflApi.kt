package com.jdamcd.arrivals.tfl

import com.jdamcd.arrivals.BuildKonfig
import com.jdamcd.arrivals.JsonApiClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

internal class TflApi(client: HttpClient) :
    JsonApiClient(
        client = client,
        apiName = "TfL API"
    ) {

    suspend fun fetchArrivals(station: String): List<ApiArrival> = executeRequest("$BASE_URL/StopPoint/$station/Arrivals") {
        parameter("app_key", BuildKonfig.TFL_KEY)
    }.body()

    suspend fun searchStations(query: String): ApiSearchResult = executeRequest("$BASE_URL/StopPoint/Search") {
        parameter("app_key", BuildKonfig.TFL_KEY)
        parameter("query", query)
        parameter("modes", "dlr,elizabeth-line,overground,tube,tram")
        parameter("tflOperatedNationalRailStationsOnly", true)
    }.body()

    suspend fun stopDetails(id: String): ApiStopPoint = executeRequest("$BASE_URL/StopPoint/$id") {
        parameter("app_key", BuildKonfig.TFL_KEY)
    }.body()

    suspend fun fetchTimetable(lineId: String, stopId: String): ApiTimetableResponse = executeRequest("$BASE_URL/Line/$lineId/Timetable/$stopId") {
        parameter("app_key", BuildKonfig.TFL_KEY)
    }.body()
}

@Serializable
internal data class ApiArrival(
    val id: Int,
    val stationName: String,
    val platformName: String,
    val direction: String? = null, // null for terminal station arrivals
    val destinationName: String,
    val timeToStation: Int,
    val lineId: String = ""
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

@Serializable
internal data class ApiTimetableResponse(
    val stops: List<ApiTimetableStation> = emptyList(),
    val timetable: ApiTimetable
)

@Serializable
internal data class ApiTimetableStation(
    val id: String,
    val name: String
)

@Serializable
internal data class ApiTimetable(
    val routes: List<ApiTimetableRoute> = emptyList()
)

@Serializable
internal data class ApiTimetableRoute(
    val stationIntervals: List<ApiStationInterval> = emptyList(),
    val schedules: List<ApiTimetableSchedule> = emptyList()
)

@Serializable
internal data class ApiStationInterval(
    val id: String,
    val intervals: List<ApiStopInterval> = emptyList()
)

@Serializable
internal data class ApiStopInterval(
    val stopId: String,
    val timeToArrival: Double
)

@Serializable
internal data class ApiTimetableSchedule(
    val name: String,
    val knownJourneys: List<ApiKnownJourney> = emptyList()
)

@Serializable
internal data class ApiKnownJourney(
    val hour: String,
    val minute: String,
    val intervalId: Int
)

private const val BASE_URL = "https://api.tfl.gov.uk"
