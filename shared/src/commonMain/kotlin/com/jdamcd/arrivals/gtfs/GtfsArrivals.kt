package com.jdamcd.arrivals.gtfs

import com.google.transit.realtime.FeedEntity
import com.google.transit.realtime.FeedMessage
import com.google.transit.realtime.TripUpdate
import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.ArrivalsInfo
import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.formatTime
import kotlinx.datetime.Clock
import kotlin.coroutines.cancellation.CancellationException

internal class GtfsArrivals(
    private val api: GtfsApi,
    private val clock: Clock,
    private val settings: Settings
) : Arrivals {

    private lateinit var stops: GtfsStops

    @Throws(NoDataException::class, CancellationException::class)
    override suspend fun latest(): ArrivalsInfo {
        updateStops()
        val model: ArrivalsInfo
        try {
            model = formatArrivals(api.fetchFeedMessage(settings.gtfsRealtime))
        } catch (e: Exception) {
            throw NoDataException("No connection")
        }
        if (model.arrivals.isEmpty()) {
            throw NoDataException("No arrivals found")
        }
        return model
    }

    private suspend fun updateStops() {
        try {
            if (!hasFreshStops()) {
                stops = GtfsStops(api.downloadStops(settings.gtfsSchedule))
                settings.gtfsStopsUpdated = clock.now().epochSeconds
            } else if (!::stops.isInitialized) {
                stops = GtfsStops(api.readStops())
            }
        } catch (e: Exception) {
            throw NoDataException("Failed to load stops")
        }
    }

    private fun hasFreshStops(): Boolean {
        val twoDaysInSeconds = 48 * 60 * 60
        return settings.gtfsStopsUpdated + twoDaysInSeconds > clock.now().epochSeconds
    }

    private fun formatArrivals(feedMessage: FeedMessage): ArrivalsInfo {
        val stop = settings.gtfsStop
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
        val destinationId = tripUpdate.stop_time_update.last().stop_id!!
        val destinationName = stops.stopIdToName(destinationId) ?: destinationId
        val seconds = secondsToStop(stopTimeUpdate.arrival?.time ?: stopTimeUpdate.departure?.time)
        return Arrival(
            stopTimeUpdate.hashCode(),
            "${tripUpdate.trip.route_id} - $destinationName",
            formatTime(seconds),
            seconds
        )
    }

    private fun secondsToStop(time: Long?): Int {
        if (time == null) {
            return Int.MAX_VALUE
        } else {
            val now = clock.now().epochSeconds
            return (time - now).toInt()
        }
    }
}
