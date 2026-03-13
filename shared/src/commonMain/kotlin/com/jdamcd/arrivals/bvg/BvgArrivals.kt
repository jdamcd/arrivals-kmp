package com.jdamcd.arrivals.bvg

import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.ArrivalsInfo
import com.jdamcd.arrivals.BvgSearch
import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.StopResult
import com.jdamcd.arrivals.formatTime
import com.jdamcd.arrivals.matchesPlatformFilter
import com.jdamcd.arrivals.stripPlatform
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.coroutines.cancellation.CancellationException

internal class BvgArrivals(
    private val api: BvgApi,
    private val settings: Settings,
    private val clock: Clock
) : Arrivals,
    BvgSearch {

    private var cachedStopName: Pair<String, String>? = null

    @Throws(NoDataException::class, CancellationException::class)
    override suspend fun latest(): ArrivalsInfo {
        val stopId = settings.stationId
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

    @Throws(CancellationException::class)
    override suspend fun searchStops(query: String): List<StopResult> = api.searchStops(query)
        .filter { it.type == "stop" && it.id != null && it.name != null }
        .map { StopResult(id = it.id!!, name = it.name!!, isHub = false) }

    private fun formatArrivals(stopName: String, departures: List<ApiBvgDeparture>): ArrivalsInfo {
        val station = formatStationName(stopName)
        val nowSeconds = clock.now().epochSeconds

        val arrivals = departures
            .asSequence()
            .filter { it.departureTime != null }
            .filter {
                settings.bvgLine.isEmpty() ||
                    it.line?.name.equals(settings.bvgLine, ignoreCase = true)
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
            settings.bvgLine.takeIf { it.isNotEmpty() },
            "Platform ${stripPlatform(settings.platform)}".takeIf { settings.platform.isNotEmpty() }
        ).joinToString(", ")
        return if (suffix.isNotEmpty()) "$baseName: $suffix" else baseName
    }

    private fun createArrival(departure: ApiBvgDeparture, nowSeconds: Long): Arrival? {
        val departureTime = departure.departureTime ?: return null
        val direction = departure.direction ?: "Unknown"
        val lineName = departure.line?.name
        val destination = if (lineName != null) "$lineName $direction" else direction

        val departureSeconds = try {
            Instant.parse(departureTime).epochSeconds
        } catch (_: Exception) {
            return null
        }
        val seconds = (departureSeconds - nowSeconds).toInt()

        return Arrival(
            id = departure.tripId.hashCode(),
            destination = destination,
            time = formatTime(seconds),
            secondsToStop = seconds
        )
    }

    companion object {
        private const val MAX_SECONDS_AHEAD = 7200 // 2 hours
    }
}
