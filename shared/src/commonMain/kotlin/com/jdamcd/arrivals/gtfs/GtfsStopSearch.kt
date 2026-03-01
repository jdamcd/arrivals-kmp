package com.jdamcd.arrivals.gtfs

import com.jdamcd.arrivals.GtfsSearch
import com.jdamcd.arrivals.StopResult

internal class GtfsStopSearch(
    private val api: GtfsApi,
    private val scheduleUrl: String,
    private val cacheFolder: String,
    private val apiKey: String = ""
) : GtfsSearch {

    private lateinit var stops: GtfsStops

    private suspend fun updateStops() {
        if (!::stops.isInitialized) {
            stops = GtfsStops(api.downloadStops(scheduleUrl, cacheFolder, apiKey))
        }
    }

    override suspend fun getStops(feedUrl: String): List<StopResult> {
        updateStops()
        val feedMessage = api.fetchFeedMessage(feedUrl, apiKey)

        return feedMessage.entity
            .asSequence()
            .mapNotNull { it.trip_update }
            .flatMap { it.stop_time_update }
            .mapNotNull { it.stop_id }
            .distinct()
            .filter { stops.stopIdToName(it) != null }
            .map { StopResult(it, "${stops.stopIdToName(it)!!} ($it)", false) }
            .toList()
            .sortedBy { it.name }
    }
}
