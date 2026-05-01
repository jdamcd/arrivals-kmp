package com.jdamcd.arrivals.tfl

import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.ArrivalsInfo
import com.jdamcd.arrivals.MAX_SECONDS_AHEAD
import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.StopDetails
import com.jdamcd.arrivals.StopResult
import com.jdamcd.arrivals.TflSearch
import com.jdamcd.arrivals.matchesPlatformFilter
import com.jdamcd.arrivals.stripPlatform
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val LONDON = TimeZone.of("Europe/London")

@OptIn(ExperimentalTime::class)
internal class TflArrivals(
    private val api: TflApi,
    private val settings: Settings,
    private val clock: Clock
) : Arrivals,
    TflSearch {

    @Throws(NoDataException::class, CancellationException::class)
    override suspend fun latest(): ArrivalsInfo {
        val apiArrivals = api.fetchArrivals(settings.stopId)
        val station = stationInfo(apiArrivals.firstOrNull()?.stationName ?: "")

        val terminalPlatforms = terminalPlatforms(apiArrivals)
        val filtered = apiArrivals.filter { it.platformName !in terminalPlatforms }

        if (terminalPlatforms.isNotEmpty() && filtered.isEmpty()) {
            // Real-time data at terminals shows arrivals, not departures — use schedule
            val lineIds = apiArrivals.map { it.lineId }.filter { it.isNotEmpty() }.toSet()
            if (lineIds.isNotEmpty()) {
                val scheduled = scheduledDepartures(lineIds, station)
                if (scheduled.arrivals.isNotEmpty()) return scheduled
            }
            throw NoDataException("No arrivals found")
        }

        val model = formatArrivals(filtered, station)
        if (model.arrivals.isEmpty()) {
            throw NoDataException("No arrivals found")
        }
        return model
    }

    @Throws(Exception::class, CancellationException::class)
    override suspend fun searchStops(query: String): List<StopResult> = api
        .searchStations(query)
        .matches
        .map { StopResult(it.id, it.name, it.id.startsWith("HUB")) }

    @Throws(Exception::class, CancellationException::class)
    override suspend fun stopDetails(id: String): StopDetails {
        val stopPoint = api.stopDetails(id)
        return StopDetails(
            stopPoint.naptanId,
            stopPoint.commonName,
            stopPoint.children
                .filter { it.stopType == "NaptanMetroStation" || it.stopType == "NaptanRailStation" }
                .map { StopResult(it.naptanId, it.commonName, it.naptanId.startsWith("HUB")) }
        )
    }

    private fun terminalPlatforms(apiArrivals: List<ApiArrival>): Set<String> = apiArrivals
        .filter {
            val dest = formatStation(it.destinationName)
            dest.isEmpty() || dest == formatStation(it.stationName)
        }
        .map { it.platformName }
        .toSet()

    private fun formatArrivals(apiArrivals: List<ApiArrival>, station: String): ArrivalsInfo {
        val arrivals =
            apiArrivals
                .asSequence()
                .sortedBy { it.timeToStation }
                .filter {
                    val dest = formatStation(it.destinationName)
                    dest.isNotEmpty() && dest != formatStation(it.stationName)
                }
                .filter {
                    settings.platform.isEmpty() ||
                        matchesPlatformFilter(it.platformName, settings.platform)
                }
                .filter { arrival ->
                    !hasDirectionFilter() ||
                        (arrival.direction?.contains(settings.direction) == true)
                }.take(3)
                .map {
                    Arrival(
                        // DLR arrivals all have the same ID, so use hash
                        id = it.hashCode(),
                        destination = formatStation(it.destinationName),
                        secondsToStop = it.timeToStation,
                        lineBadge = TflLines.badgeFor(it.lineId)
                    )
                }
                .toList()
        return ArrivalsInfo(
            station = station,
            arrivals = arrivals
        )
    }

    private suspend fun scheduledDepartures(
        lineIds: Set<String>,
        station: String
    ): ArrivalsInfo = coroutineScope {
        val now = clock.now().toLocalDateTime(LONDON)
        val departures = lineIds
            .map { lineId -> async { departuresForLine(lineId, now) } }
            .awaitAll()
            .flatten()
            .sortedBy { it.secondsToStop }
            .take(3)
        ArrivalsInfo(station = station, arrivals = departures)
    }

    // A failure for one line shouldn't blank out the others
    private suspend fun departuresForLine(lineId: String, now: LocalDateTime): List<Arrival> = try {
        val response = api.fetchTimetable(lineId, settings.stopId)
        val destinations = resolveDestinations(response)
        response.timetable.routes.flatMap { route ->
            departuresForRoute(route, destinations, now, lineId)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptyList()
    }

    private fun departuresForRoute(
        route: ApiTimetableRoute,
        destinations: Map<Int, String>,
        now: LocalDateTime,
        lineId: String
    ): List<Arrival> {
        val schedule = findScheduleForDay(route.schedules, now.dayOfWeek) ?: return emptyList()
        return schedule.knownJourneys
            .mapNotNull { toArrival(it, destinations, now, lineId) }
            .filter { it.secondsToStop < MAX_SECONDS_AHEAD }
    }

    private fun toArrival(
        journey: ApiKnownJourney,
        destinations: Map<Int, String>,
        now: LocalDateTime,
        lineId: String
    ): Arrival? {
        val seconds = secondsUntilDeparture(journey, now)
        if (seconds < 0) return null
        val destination = destinations[journey.intervalId] ?: return null
        return Arrival(
            id = journey.hashCode(),
            destination = destination,
            secondsToStop = seconds,
            realtime = false,
            lineBadge = TflLines.badgeFor(lineId)
        )
    }

    private fun hasDirectionFilter() = settings.direction.isNotEmpty() && settings.direction != "all"

    private fun stationInfo(name: String): String {
        val station = formatStation(name)
        return if (station.isEmpty()) {
            station
        } else if (settings.platform.isNotEmpty()) {
            "$station: Platform ${stripPlatform(settings.platform)}"
        } else if (hasDirectionFilter()) {
            "$station: ${formatDirection(settings.direction)}"
        } else {
            station
        }
    }
}

internal fun findScheduleForDay(
    schedules: List<ApiTimetableSchedule>,
    dayOfWeek: DayOfWeek
): ApiTimetableSchedule? {
    val dayName = dayOfWeek.name.lowercase()
    // Try exact day name match (handles "Saturday", "Sunday", "Friday")
    schedules.firstOrNull { it.name.lowercase().contains(dayName) }?.let { return it }
    // For weekdays without exact match (e.g. Tuesday in "Monday - Thursday"), try range patterns
    if (dayOfWeek <= DayOfWeek.FRIDAY) {
        schedules.firstOrNull {
            val lower = it.name.lowercase()
            lower.contains("weekday") ||
                (lower.contains("monday") && (lower.contains("friday") || lower.contains("thursday")))
        }?.let { return it }
        // Catch "Mondays" as weekday default (e.g. Piccadilly uses "Mondays" for Mon-Thu)
        schedules.firstOrNull { it.name.lowercase().contains("monday") }?.let { return it }
    }
    // Fall back to first schedule
    return schedules.firstOrNull()
}

internal fun resolveDestinations(response: ApiTimetableResponse): Map<Int, String> {
    val stationNames = response.stops.associate { it.id to formatStation(it.name) }
    return response.timetable.routes.flatMap { route ->
        route.stationIntervals.mapNotNull { interval ->
            val lastStop = interval.intervals.lastOrNull() ?: return@mapNotNull null
            val name = stationNames[lastStop.stopId] ?: return@mapNotNull null
            interval.id.toIntOrNull()?.let { it to name }
        }
    }.toMap()
}

private fun secondsUntilDeparture(journey: ApiKnownJourney, now: LocalDateTime): Int {
    val hour = journey.hour.toIntOrNull() ?: return -1
    val minute = journey.minute.toIntOrNull() ?: return -1
    val departureMinutes = hour * 60 + minute
    val nowMinutes = now.hour * 60 + now.minute
    return (departureMinutes - nowMinutes) * 60 - now.second
}

private fun formatStation(name: String) = name
    .replace("Rail Station", "")
    .replace("Underground Station", "")
    .replace("DLR Station", "")
    .replace("(London)", "")
    .replace("ELL", "")
    .trim()

private fun formatDirection(direction: String) = direction.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}
