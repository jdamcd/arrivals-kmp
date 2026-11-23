package com.jdamcd.arrivals.tfl

import com.jdamcd.arrivals.BuildKonfig
import com.jdamcd.arrivals.HttpApiClient
import com.jdamcd.arrivals.apiJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

internal class TflApi(client: HttpClient) :
    HttpApiClient(
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

    suspend fun searchBusStops(query: String): ApiSearchResult = executeRequest("$BASE_URL/StopPoint/Search") {
        parameter("app_key", BuildKonfig.TFL_KEY)
        parameter("query", query)
        parameter("modes", "bus")
    }.body()

    suspend fun stopDetails(id: String): ApiStopPoint = executeRequest("$BASE_URL/StopPoint/$id") {
        parameter("app_key", BuildKonfig.TFL_KEY)
    }.body()

    suspend fun stopDetails(ids: List<String>): List<ApiStopPoint> = coroutineScope {
        ids
            .chunked(MAX_STOP_IDS)
            .map { chunk ->
                async {
                    val body = executeRequest("$BASE_URL/StopPoint/${chunk.joinToString(",")}") {
                        parameter("app_key", BuildKonfig.TFL_KEY)
                    }.body<JsonElement>()
                    // A bare object comes back instead of an array when the ids resolve to one
                    // stop point, which includes several ids that share a parent
                    apiJson.decodeFromJsonElement<List<ApiStopPoint>>(body as? JsonArray ?: JsonArray(listOf(body)))
                }
            }.awaitAll()
            .flatten()
    }

    suspend fun fetchTimetable(lineId: String, stopId: String): ApiTimetableResponse = executeRequest("$BASE_URL/Line/$lineId/Timetable/$stopId") {
        parameter("app_key", BuildKonfig.TFL_KEY)
    }.body()
}

@Serializable
internal data class ApiArrival(
    val id: String = "", // numeric for rail, arbitrary string for buses
    val stationName: String,
    val platformName: String,
    val direction: String? = null, // null for terminal station arrivals
    val destinationName: String = "",
    val timeToStation: Int,
    val lineId: String = "",
    val lineName: String = ""
)

@Serializable
internal data class ApiSearchResult(
    val matches: List<ApiMatchedStop>
)

@Serializable
internal data class ApiMatchedStop(
    val id: String,
    val name: String,
    val towards: String? = null
)

@Serializable
internal data class ApiStopPoint(
    val commonName: String,
    val naptanId: String,
    val stopType: String,
    val children: List<ApiStopPoint>,
    val modes: List<String> = emptyList(),
    val stopLetter: String? = null,
    val indicator: String? = null,
    val additionalProperties: List<ApiAdditionalProperty> = emptyList()
)

@Serializable
internal data class ApiAdditionalProperty(
    val key: String = "",
    val value: String = ""
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

// TfL responds 400 to a StopPoint lookup with more than 20 ids
private const val MAX_STOP_IDS = 20
