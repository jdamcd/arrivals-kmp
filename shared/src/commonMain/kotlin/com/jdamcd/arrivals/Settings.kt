package com.jdamcd.arrivals

expect class Settings() {
    var mode: String
    var stationId: String
    var platform: String
    var tflDirection: String
    var gtfsStopsUpdated: Long
    var gtfsRealtime: String
    var gtfsSchedule: String
    var gtfsApiKey: String
    var gtfsApiKeyParam: String
    var bvgLine: String
}

object SettingsConfig {
    const val STORE_NAME = "arrivals_settings"

    const val MODE = "mode"
    const val MODE_TFL = "tfl"
    const val MODE_GTFS = "gtfs"
    const val MODE_DARWIN = "darwin"
    const val MODE_BVG = "bvg"

    const val STATION_ID = "station_id"
    const val STATION_ID_DEFAULT = ""

    const val PLATFORM = "platform"
    const val PLATFORM_DEFAULT = ""

    const val TFL_DIRECTION = "tfl_direction"
    const val TFL_DIRECTION_DEFAULT = "all"

    const val GTFS_STOPS_UPDATED = "gtfs_stops_updated"
    const val GTFS_REALTIME = "gtfs_realtime"
    const val GTFS_REALTIME_DEFAULT = "https://api-endpoint.mta.info/Dataservice/mtagtfsfeeds/nyct%2Fgtfs-ace"
    const val GTFS_SCHEDULE = "gtfs_schedule"
    const val GTFS_SCHEDULE_DEFAULT = "http://web.mta.info/developers/data/nyct/subway/google_transit.zip"
    const val GTFS_API_KEY = "gtfs_api_key"
    const val GTFS_API_KEY_DEFAULT = ""
    const val GTFS_API_KEY_PARAM = "gtfs_api_key_param"
    const val GTFS_API_KEY_PARAM_DEFAULT = ""

    const val BVG_LINE = "bvg_line"
    const val BVG_LINE_DEFAULT = ""
}
