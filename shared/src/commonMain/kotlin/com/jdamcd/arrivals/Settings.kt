package com.jdamcd.arrivals

expect class Settings() {
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
}

fun Settings.clearStopConfig() {
    stopId = ""
    platform = ""
    line = ""
    direction = SettingsConfig.DIRECTION_DEFAULT
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
    const val DIRECTION_DEFAULT = "all"

    const val GTFS_STOPS_UPDATED = "gtfs_stops_updated"
    const val GTFS_REALTIME = "gtfs_realtime"
    const val GTFS_REALTIME_DEFAULT = "https://api-endpoint.mta.info/Dataservice/mtagtfsfeeds/nyct%2Fgtfs-ace"
    const val GTFS_SCHEDULE = "gtfs_schedule"
    const val GTFS_SCHEDULE_DEFAULT = "http://web.mta.info/developers/data/nyct/subway/google_transit.zip"
    const val GTFS_API_KEY = "gtfs_api_key"
    const val GTFS_API_KEY_DEFAULT = ""
    const val GTFS_API_KEY_PARAM = "gtfs_api_key_param"
    const val GTFS_API_KEY_PARAM_DEFAULT = ""
}
