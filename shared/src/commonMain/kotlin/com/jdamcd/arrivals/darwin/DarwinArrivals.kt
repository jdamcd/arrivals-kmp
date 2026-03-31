package com.jdamcd.arrivals.darwin

import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.ArrivalsInfo
import com.jdamcd.arrivals.MAX_SECONDS_AHEAD
import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.StopResult
import com.jdamcd.arrivals.StopSearch
import com.jdamcd.arrivals.matchesPlatformFilter
import com.jdamcd.arrivals.stripPlatform
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

private val LONDON = TimeZone.of("Europe/London")

@OptIn(kotlin.time.ExperimentalTime::class)
internal class DarwinArrivals(
    private val api: DarwinApi,
    private val settings: Settings,
    private val clock: Clock
) : Arrivals,
    StopSearch {

    @Throws(NoDataException::class, CancellationException::class)
    override suspend fun latest(): ArrivalsInfo {
        val model = formatArrivals(api.fetchDepartures(settings.stopId))
        if (model.arrivals.isEmpty()) {
            throw NoDataException("No departures found")
        }
        return model
    }

    @Throws(Exception::class, CancellationException::class)
    override suspend fun searchStops(query: String): List<StopResult> = api.searchCrs(query).map {
        StopResult(
            id = it.crsCode,
            name = "${it.stationName} (${it.crsCode})",
            isHub = false
        )
    }

    private fun formatArrivals(board: ApiDarwinBoard): ArrivalsInfo {
        val station = formatStationName(board)
        val referenceTime = parseGeneratedAt(board.generatedAt)

        val arrivals = board.trainServices
            ?.asSequence()
            ?.filter { !it.isCancelled }
            ?.filter { isValidDeparture(it.etd) }
            ?.filter {
                settings.platform.isEmpty() ||
                    (it.platform != null && matchesPlatformFilter(it.platform, settings.platform))
            }
            ?.map { service -> createArrival(service, referenceTime) }
            ?.filter { it.secondsToStop < MAX_SECONDS_AHEAD }
            ?.sortedBy { it.secondsToStop }
            ?.take(3)
            ?.toList()
            ?: emptyList()

        return ArrivalsInfo(station = station, arrivals = arrivals)
    }

    private fun formatStationName(board: ApiDarwinBoard): String {
        val baseName = board.locationName.removeSuffix(" Rail Station").trim()
        return if (settings.platform.isNotEmpty()) {
            "$baseName: Platform ${stripPlatform(settings.platform)}"
        } else {
            baseName
        }
    }

    private fun createArrival(service: ApiTrainService, referenceTime: Long): Arrival {
        val destination = service.destination.lastOrNull()?.locationName ?: "Unknown"
        val seconds = calculateSecondsToStop(service.std, service.etd, referenceTime)

        return Arrival(
            id = service.serviceIdUrlSafe.hashCode(),
            destination = destination,
            secondsToStop = seconds
        )
    }

    private fun calculateSecondsToStop(std: String?, etd: String, referenceSeconds: Long): Int {
        val departureTime = when {
            etd == "On time" && std != null -> std
            etd.contains(":") -> etd
            else -> return Int.MAX_VALUE
        }

        val (departureHours, departureMinutes) = parseTimeString(departureTime)
        val currentSeconds = (referenceSeconds % 86400).toInt()
        val currentHours = currentSeconds / 3600
        val currentMinutes = (currentSeconds % 3600) / 60

        val departureSecondsOfDay = (departureHours * 3600) + (departureMinutes * 60)
        val currentSecondsOfDay = (currentHours * 3600) + (currentMinutes * 60)

        var diff = departureSecondsOfDay - currentSecondsOfDay

        if (diff < 0) {
            diff += 86400
        }

        return diff
    }

    private fun parseTimeString(time: String): Pair<Int, Int> {
        val parts = time.split(":")
        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        return Pair(hours, minutes)
    }

    private fun parseGeneratedAt(timestamp: String): Long = try {
        val instant = Instant.parse(timestamp)
        val local = instant.toLocalDateTime(LONDON)
        ((local.hour * 3600) + (local.minute * 60) + local.second).toLong()
    } catch (_: Exception) {
        clock.now().epochSeconds
    }

    private fun isValidDeparture(etd: String): Boolean = etd.contains(":") || etd == "On time"
}
