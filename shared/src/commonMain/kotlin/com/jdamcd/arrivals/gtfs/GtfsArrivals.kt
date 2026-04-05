package com.jdamcd.arrivals.gtfs

import com.google.transit.realtime.FeedEntity
import com.google.transit.realtime.FeedMessage
import com.google.transit.realtime.TripUpdate
import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.ArrivalsInfo
import com.jdamcd.arrivals.LineBadge
import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.gtfs.system.Mta
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

@OptIn(kotlin.time.ExperimentalTime::class)
internal class GtfsArrivals(
    private val api: GtfsApi,
    private val clock: Clock,
    private val settings: Settings
) : Arrivals {

    private lateinit var stops: GtfsStops
    private lateinit var routes: GtfsRoutes

    private val auth: ApiAuth?
        get() = ApiAuth.parse(settings.gtfsApiKey, settings.gtfsApiKeyParam)

    @Throws(NoDataException::class, CancellationException::class)
    override suspend fun latest(): ArrivalsInfo {
        updateSchedule()
        val model: ArrivalsInfo
        try {
            model = formatArrivals(api.fetchFeedMessage(settings.gtfsRealtime, auth))
        } catch (_: Exception) {
            throw NoDataException("No connection")
        }
        if (model.arrivals.isEmpty()) {
            throw NoDataException("No arrivals found")
        }
        return model
    }

    private suspend fun updateSchedule() {
        try {
            if (!hasFreshSchedule()) {
                api.downloadSchedule(settings.gtfsSchedule, auth = auth)
                stops = GtfsStops(api.readStops())
                routes = loadRoutes()
                settings.gtfsStopsUpdated = clock.now().epochSeconds
            } else if (!::stops.isInitialized) {
                stops = GtfsStops(api.readStops())
                routes = loadRoutes()
            }
        } catch (_: Exception) {
            throw NoDataException("Failed to load stops")
        }
    }

    private fun hasFreshSchedule(): Boolean {
        val twoDaysInSeconds = 48 * 60 * 60
        if (!api.hasStops() || api.stopsSource() != settings.gtfsSchedule) return false
        val updatedAt = if (settings.gtfsStopsUpdated > 0L) {
            settings.gtfsStopsUpdated
        } else {
            api.stopsLastModifiedEpochSeconds()
        }
        return updatedAt + twoDaysInSeconds > clock.now().epochSeconds
    }

    private fun formatArrivals(feedMessage: FeedMessage): ArrivalsInfo {
        val stop = settings.stopId
        val arrivals = getNextArrivalsForStop(stop, feedMessage.entity)
        return ArrivalsInfo(stops.stopIdToName(stop) ?: stop, arrivals)
    }

    private fun getNextArrivalsForStop(
        stopId: String,
        feedItems: List<FeedEntity>
    ): List<Arrival> = feedItems
        .asSequence()
        .mapNotNull { it.trip_update }
        .flatMap { tripUpdate ->
            tripUpdate.stop_time_update
                .filter { it.stop_id == stopId }
                .map { createArrival(tripUpdate, it) }
        }
        .filter { it.secondsToStop >= 0 }
        .sortedBy { it.secondsToStop }
        .take(3)
        .toList()

    private fun createArrival(
        tripUpdate: TripUpdate,
        stopTimeUpdate: TripUpdate.StopTimeUpdate
    ): Arrival {
        val seconds = secondsToStop(stopTimeUpdate.arrival?.time ?: stopTimeUpdate.departure?.time)
        val routeId = tripUpdate.trip.route_id
        val style = routes.styleFor(routeId)
        val destinationId = tripUpdate.stop_time_update.last().stop_id
        val destinationName = destinationId?.let { stops.stopIdToName(it) ?: it } ?: "Unknown"
        val lineLabel = style?.let { it.label ?: routeId }
        val lineBadge = lineLabel?.let {
            LineBadge(
                label = it,
                color = style.color,
                textColor = style.textColor,
                express = routes.isExpress(routeId)
            )
        }
        return Arrival(
            id = stopTimeUpdate.hashCode(),
            destination = destinationName,
            secondsToStop = seconds,
            line = lineLabel,
            lineBadge = lineBadge
        )
    }

    private fun loadRoutes(): GtfsRoutes = GtfsRoutes(
        routes = api.readRoutes() ?: "",
        agencyExpressOverrides = mapOf(Mta.AGENCY_ID to Mta.expressOverrides)
    )

    private fun secondsToStop(time: Long?): Int {
        if (time == null) return Int.MAX_VALUE
        val now = clock.now().epochSeconds
        return (time - now).toInt()
    }
}

data class RouteStyle(
    val color: String,
    val label: String? = null,
    val textColor: String? = null
)
