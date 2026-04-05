package com.jdamcd.arrivals.web

import com.google.transit.realtime.FeedMessage
import com.google.transit.realtime.TripUpdate
import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.ArrivalsInfo
import com.jdamcd.arrivals.LineBadge
import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.gtfs.GtfsRoutes
import com.jdamcd.arrivals.gtfs.GtfsStops
import com.jdamcd.arrivals.gtfs.system.Mta
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class MtaArrivals(
    private val stopId: String,
    feedKey: String
) {
    private val feedUrl = Mta.realtime[feedKey]
        ?: error("Unknown MTA feed: $feedKey. Valid keys: ${Mta.realtime.keys}")

    private val stops = GtfsStops(MTA_STOPS_CSV)
    private val routes = GtfsRoutes(MTA_ROUTES_CSV, mapOf(Mta.AGENCY_ID to Mta.expressOverrides))
    private val client = HttpClient()
    private val clock = Clock.System

    suspend fun fetch(): ArrivalsInfo {
        val feedMessage: FeedMessage
        try {
            val bytes = client.get(feedUrl).bodyAsBytes()
            feedMessage = FeedMessage.ADAPTER.decode(bytes)
        } catch (e: Exception) {
            throw NoDataException("No connection")
        }
        val arrivals = getNextArrivalsForStop(feedMessage)
        if (arrivals.isEmpty()) {
            throw NoDataException("No arrivals found")
        }
        return ArrivalsInfo(stops.stopIdToName(stopId) ?: stopId, arrivals)
    }

    private fun getNextArrivalsForStop(feedMessage: FeedMessage): List<Arrival> = feedMessage.entity
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
        val routeId = tripUpdate.trip.route_id
        val style = routes.styleFor(routeId)
        val destinationId = tripUpdate.stop_time_update.last().stop_id!!
        val destinationName = stops.stopIdToName(destinationId) ?: destinationId
        val seconds = secondsToStop(stopTimeUpdate.arrival?.time ?: stopTimeUpdate.departure?.time)
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

    private fun secondsToStop(time: Long?): Int {
        if (time == null) return Int.MAX_VALUE
        val now = clock.now().epochSeconds
        return (time - now).toInt()
    }
}
