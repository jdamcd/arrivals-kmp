package com.jdamcd.arrivals.darwin

import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.ArrivalsInfo
import com.jdamcd.arrivals.DarwinSearch
import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.StopResult
import com.jdamcd.arrivals.formatTime
import kotlin.coroutines.cancellation.CancellationException

@OptIn(kotlin.time.ExperimentalTime::class)
internal class DarwinArrivals(
    private val api: DarwinApi,
    private val settings: Settings,
    private val clock: kotlin.time.Clock
) : DarwinSearch {

    @Throws(NoDataException::class, CancellationException::class)
    suspend fun latest(): ArrivalsInfo {
        val model: ArrivalsInfo
        try {
            val board = api.fetchDepartures(settings.darwinCrsCode)
            model = formatArrivals(board)
        } catch (e: Exception) {
            throw NoDataException("No connection")
        }
        if (model.arrivals.isEmpty()) {
            throw NoDataException("No departures found")
        }
        return model
    }

    @Throws(CancellationException::class)
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
            ?.filter { matchesPlatformFilter(it.platform) }
            ?.map { service -> createArrival(service, referenceTime) }
            ?.sortedBy { it.secondsToStop }
            ?.take(3)
            ?.toList()
            ?: emptyList()

        return ArrivalsInfo(station = station, arrivals = arrivals)
    }

    private fun formatStationName(board: ApiDarwinBoard): String {
        val baseName = board.locationName.removeSuffix(" Rail Station").trim()
        return if (settings.darwinPlatform.isNotEmpty()) {
            "$baseName: Platform ${settings.darwinPlatform}"
        } else {
            baseName
        }
    }

    private fun matchesPlatformFilter(platform: String?): Boolean {
        if (settings.darwinPlatform.isEmpty()) return true
        return platform?.contains(settings.darwinPlatform, ignoreCase = true) ?: false
    }

    private fun createArrival(service: ApiTrainService, referenceTime: Long): Arrival {
        val destination = service.destination.lastOrNull()?.locationName ?: "Unknown"
        val seconds = calculateSecondsToStop(service.std, service.etd, referenceTime)

        return Arrival(
            id = service.serviceIdUrlSafe.hashCode(),
            destination = destination,
            time = formatTime(seconds),
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
        val isoPattern = """(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})""".toRegex()
        val match = isoPattern.find(timestamp)
        if (match != null) {
            val (_, _, _, hours, minutes, seconds) = match.destructured
            ((hours.toInt() * 3600) + (minutes.toInt() * 60) + seconds.toInt()).toLong()
        } else {
            clock.now().epochSeconds.toLong()
        }
    } catch (e: Exception) {
        clock.now().epochSeconds.toLong()
    }

    private fun isValidDeparture(etd: String): Boolean = etd.contains(":") || etd == "On time"
}
