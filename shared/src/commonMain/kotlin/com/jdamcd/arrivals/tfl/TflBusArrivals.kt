package com.jdamcd.arrivals.tfl

import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.ArrivalsInfo
import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.StopDetails
import com.jdamcd.arrivals.StopResult
import com.jdamcd.arrivals.TflSearch
import kotlin.coroutines.cancellation.CancellationException

internal class TflBusArrivals(
    private val api: TflApi,
    private val settings: Settings
) : Arrivals,
    TflSearch {

    @Throws(NoDataException::class, CancellationException::class)
    override suspend fun latest(count: Int): ArrivalsInfo {
        val stopId = settings.stopId
        if (stopId.isEmpty()) throw NoDataException("No bus stop configured")
        val model = formatArrivals(api.fetchArrivals(stopId), count)
        if (model.arrivals.isEmpty()) {
            throw NoDataException("No arrivals found")
        }
        return model
    }

    @Throws(Exception::class, CancellationException::class)
    override suspend fun searchStops(query: String): List<StopResult> {
        val results = api.searchBusStops(query).matches.map {
            StopResult(
                id = it.id,
                name = formatStopName(it.name, letter = null, towards = it.towards),
                isHub = isStopGroup(it.id)
            )
        }
        // Search matches carry no stop letter, and groups can share a name (London has
        // ten "Victoria Road"s), so rows need a lookup to be told apart
        val sameNamedGroups = results
            .filter { it.isHub }
            .groupBy({ it.name }, { it.id })
            .values
            .filter { it.size > 1 }
            .flatten()
        val ids = results.filterNot { it.isHub }.map { it.id } + sameNamedGroups
        if (ids.isEmpty()) return results
        val stopPoints = try {
            api.stopDetails(ids)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The lookup only refines names, so the plain results are still usable without it
            return results
        }
        // A stop comes back nested in its parent when it has one
        val found = stopPoints.flatMap { flatten(it) }.associateBy { it.naptanId }
        return results.map { result -> found[result.id]?.let { describe(result, it) } ?: result }
    }

    @Throws(Exception::class, CancellationException::class)
    override suspend fun stopDetails(id: String): StopDetails {
        // A group inside an interchange comes back as the whole interchange
        val stopPoint = api.stopDetails(id).let { root -> flatten(root).firstOrNull { it.naptanId == id } ?: root }
        return StopDetails(stopPoint.naptanId, stopPoint.commonName, busStopPoints(stopPoint).map { stopResult(it) })
    }

    private fun describe(result: StopResult, stop: ApiStopPoint): StopResult = when {
        result.isHub -> {
            val towards = busStopPoints(stop)
                .mapNotNull { towards(it)?.takeIf { value -> value.isNotBlank() } }
                .distinct()
                .joinToString(" / ")
            result.copy(name = formatStopName(result.name, letter = null, towards = towards))
        }

        stop.stopType == BUS_STOP_TYPE -> stopResult(stop)

        else -> result
    }

    private fun flatten(stop: ApiStopPoint): List<ApiStopPoint> = listOf(stop) + stop.children.flatMap { flatten(it) }

    // Stops can sit below intermediate clusters and bus stations,
    // e.g. HUBZCW -> 490G000438 (Canada Water Bus Station) -> its bays
    private fun busStopPoints(stop: ApiStopPoint): List<ApiStopPoint> = when {
        stop.stopType != BUS_STOP_TYPE -> stop.children.flatMap { busStopPoints(it) }

        // The type also covers tram stops and stops TfL doesn't serve, which never have bus arrivals
        "bus" in stop.modes -> listOf(stop)

        else -> emptyList()
    }

    private fun stopResult(stop: ApiStopPoint): StopResult {
        // Indicator first: stopLetter is derived from it even when it isn't a letter ("o/s 33" -> "33")
        val indicator = listOfNotNull(stop.indicator, stop.stopLetter).firstOrNull { it.isNotBlank() }
        val towards = towards(stop)
        // Fall back to the raw indicator (e.g. "->N") so directional
        // twins with no letter or towards stay distinguishable
        val marker = cleanLetter(indicator) ?: indicator?.takeIf { towards.isNullOrBlank() }
        return StopResult(
            id = stop.naptanId,
            name = formatStopName(stop.commonName, marker, towards),
            isHub = false
        )
    }

    private fun formatArrivals(apiArrivals: List<ApiArrival>, count: Int): ArrivalsInfo {
        val line = settings.line
        val matching = apiArrivals.filter {
            line.isEmpty() ||
                it.lineName.equals(line, ignoreCase = true) ||
                it.lineId.equals(line, ignoreCase = true)
        }
        val arrivals = matching
            .asSequence()
            .sortedBy { it.timeToStation }
            // TfL can repeat the same call with a slightly different time
            .distinctBy { arrivalId(it) }
            .take(count)
            .map {
                Arrival(
                    id = arrivalId(it),
                    destination = it.destinationName,
                    secondsToStop = it.timeToStation,
                    line = route(it),
                    lineBadge = TflLines.busBadge(route(it))
                )
            }
            .toList()
        // The route as TfL names it rather than as typed, e.g. "N185" for a filter of "n185"
        val filteredRoute = matching.firstOrNull()?.takeIf { line.isNotEmpty() }?.let { route(it) }
        return ArrivalsInfo(
            station = stationInfo(apiArrivals.firstOrNull(), filteredRoute),
            arrivals = arrivals
        )
    }

    private fun stationInfo(arrival: ApiArrival?, filteredRoute: String?): String {
        if (arrival == null) return ""
        val station = formatStopName(arrival.stationName, cleanLetter(arrival.platformName), towards = null)
        return if (filteredRoute != null) "$station: $filteredRoute" else station
    }

    // Rail-style hashing of the whole record churns every poll. The prediction id is stable but
    // per vehicle and stop, so it repeats when a bus calls twice (terminating, then departing the other way)
    private fun arrivalId(arrival: ApiArrival) = if (arrival.id.isEmpty()) {
        arrival.hashCode()
    } else {
        listOf(arrival.id, arrival.direction, arrival.destinationName).hashCode()
    }

    private fun route(arrival: ApiArrival) = arrival.lineName.ifEmpty { arrival.lineId }

    private fun towards(stop: ApiStopPoint): String? = stop.additionalProperties
        .firstOrNull { it.key == "Towards" }
        ?.value

    // Posted letters are upper case or numeric ("K", "B1", "31"). Excludes compass pseudo-letters
    // ("->E"), NaPTAN qualifiers ("opp", "adj") and the arrivals feed's literal "null"
    private fun cleanLetter(value: String?): String? = value
        ?.removePrefix("Stop ")
        ?.removePrefix("Stand ")
        ?.takeIf { it.length in 1..3 && it.all { char -> char.isUpperCase() || char.isDigit() } }

    private fun formatStopName(name: String, letter: String?, towards: String?) = buildString {
        append(name)
        if (letter != null) append(" ($letter)")
        if (!towards.isNullOrBlank()) append(" towards $towards")
    }
}

private const val BUS_STOP_TYPE = "NaptanPublicBusCoachTram"

// NaPTAN stop groups are "<3-digit ATCO area>G<suffix>" (e.g. 490G..., 400G..., 910G...);
// leaf stops like 490000248G only ever have a trailing G. TfL's own interchanges are "HUB<code>"
private fun isStopGroup(id: String) = id.startsWith("HUB") || id.getOrNull(3) == 'G'
