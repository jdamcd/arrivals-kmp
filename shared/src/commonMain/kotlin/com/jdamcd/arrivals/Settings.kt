package com.jdamcd.arrivals

import com.jdamcd.arrivals.gtfs.system.Mta

interface Settings {
    var mode: String
    var stopId: String
    var platform: String
    var line: String
    var direction: String
    var gtfsStopsUpdated: Long
    var gtfsRealtime: String
    var gtfsSchedule: String
    var gtfsApiKey: String
    var gtfsApiKeyParam: String

    fun applyColdStart() {
        saveGtfsConfig(
            stopId = "A42N",
            realtimeUrl = Mta.realtime["ACE"]!!,
            scheduleUrl = Mta.SCHEDULE
        )
    }

    fun clearStopConfig() {
        stopId = ""
        platform = ""
        line = ""
        direction = ""
        gtfsStopsUpdated = 0
    }

    fun saveGtfsConfig(
        stopId: String,
        realtimeUrl: String,
        scheduleUrl: String,
        apiKey: String = "",
        apiKeyParam: String = ""
    ) {
        clearStopConfig()
        this.stopId = stopId
        gtfsRealtime = realtimeUrl
        gtfsSchedule = scheduleUrl
        gtfsApiKey = apiKey
        gtfsApiKeyParam = apiKeyParam
        mode = SettingsConfig.MODE_GTFS
    }
}

class InMemorySettings : Settings {
    override var mode = SettingsConfig.MODE_TFL
    override var stopId = ""
    override var platform = ""
    override var line = ""
    override var direction = ""
    override var gtfsStopsUpdated = 0L
    override var gtfsRealtime = ""
    override var gtfsSchedule = ""
    override var gtfsApiKey = ""
    override var gtfsApiKeyParam = ""
}

object SettingsConfig {
    const val STORE_NAME = "arrivals_settings"

    const val MODE = "mode"
    const val MODE_TFL = "tfl"
    const val MODE_GTFS = "gtfs"
    const val MODE_DARWIN = "darwin"
    const val MODE_BVG = "bvg"

    const val STOP = "stop"
    const val PLATFORM = "platform"
    const val LINE = "line"
    const val DIRECTION = "direction"

    const val GTFS_STOPS_UPDATED = "gtfs_stops_updated"
    const val GTFS_REALTIME = "gtfs_realtime"
    const val GTFS_SCHEDULE = "gtfs_schedule"
    const val GTFS_API_KEY = "gtfs_api_key"
    const val GTFS_API_KEY_PARAM = "gtfs_api_key_param"
}
