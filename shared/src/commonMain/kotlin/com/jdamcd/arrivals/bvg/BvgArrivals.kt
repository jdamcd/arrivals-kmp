package com.jdamcd.arrivals.bvg

import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.ArrivalsInfo
import com.jdamcd.arrivals.MAX_SECONDS_AHEAD
import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.StopResult
import com.jdamcd.arrivals.StopSearch
import com.jdamcd.arrivals.formatTime
import com.jdamcd.arrivals.matchesPlatformFilter
import com.jdamcd.arrivals.stripPlatform
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
internal class BvgArrivals(
    private val api: BvgApi,
    private val settings: Settings,
    private val clock: Clock
) : Arrivals,
    StopSearch {

    private var cachedStopName: Pair<String, String>? = null

    @Throws(NoDataException::class, CancellationException::class)
    override suspend fun latest(): ArrivalsInfo {
        val stopId = settings.stopId
        if (stopId.isEmpty()) throw NoDataException("No BVG stop configured")
        val stopName = getCachedStopName(stopId)
        val response = api.fetchDepartures(stopId)
        val model = formatArrivals(stopName, response.departures)
        if (model.arrivals.isEmpty()) {
            throw NoDataException("No departures found")
        }
        return model
    }

    private suspend fun getCachedStopName(stopId: String): String {
        val cached = cachedStopName
        if (cached != null && cached.first == stopId) return cached.second
        val name = api.fetchStop(stopId).name ?: stopId
        cachedStopName = stopId to name
        return name
    }

    @Throws(Exception::class, CancellationException::class)
    override suspend fun searchStops(query: String): List<StopResult> = api.searchStops(query)
        .filter { it.type == "stop" && it.id != null && it.name != null }
        .map { StopResult(id = it.id!!, name = it.name!!, isHub = false) }

    private fun formatArrivals(stopName: String, departures: List<ApiBvgDeparture>): ArrivalsInfo {
        val station = formatStationName(stopName)
        val nowSeconds = clock.now().epochSeconds

        val arrivals = departures
            .asSequence()
            .filter {
                settings.line.isEmpty() ||
                    it.line.name.equals(settings.line, ignoreCase = true)
            }
            .filter {
                settings.platform.isEmpty() ||
                    (it.platform != null && matchesPlatformFilter(it.platform, settings.platform))
            }
            .mapNotNull { departure -> createArrival(departure, nowSeconds) }
            .filter { it.secondsToStop in 0 until MAX_SECONDS_AHEAD }
            .sortedBy { it.secondsToStop }
            .take(3)
            .toList()

        return ArrivalsInfo(station = station, arrivals = arrivals)
    }

    private fun formatStationName(stopName: String): String {
        val baseName = stopName
            .removeSuffix(" (Berlin)")
            .removeSuffix(" Bhf")
            .trim()
        val suffix = listOfNotNull(
            settings.line.takeIf { it.isNotEmpty() },
            "Platform ${stripPlatform(settings.platform)}".takeIf { settings.platform.isNotEmpty() }
        ).joinToString(", ")
        return if (suffix.isNotEmpty()) "$baseName: $suffix" else baseName
    }

    private fun createArrival(departure: ApiBvgDeparture, nowSeconds: Long): Arrival? {
        val departureTime = departure.departureTime ?: return null
        val destination = "${departure.line.name} ${departure.direction}"
        val departureSeconds = Instant.parse(departureTime).epochSeconds
        val seconds = (departureSeconds - nowSeconds).toInt()

        return Arrival(
            id = departure.tripId.hashCode(),
            destination = destination,
            time = formatTime(seconds),
            secondsToStop = seconds
        )
    }
}
