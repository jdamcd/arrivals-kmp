package com.jdamcd.arrivals

actual class Settings actual constructor() {
    actual var mode = SettingsConfig.MODE_GTFS
    actual var stationId = SettingsConfig.STATION_ID_DEFAULT
    actual var platform = SettingsConfig.PLATFORM_DEFAULT
    actual var tflDirection = SettingsConfig.TFL_DIRECTION_DEFAULT
    actual var gtfsStopsUpdated = 0L
    actual var gtfsRealtime = SettingsConfig.GTFS_REALTIME_DEFAULT
    actual var gtfsSchedule = SettingsConfig.GTFS_SCHEDULE_DEFAULT
    actual var gtfsApiKey = SettingsConfig.GTFS_API_KEY_DEFAULT
    actual var gtfsApiKeyParam = SettingsConfig.GTFS_API_KEY_PARAM_DEFAULT
    actual var bvgLine = SettingsConfig.BVG_LINE_DEFAULT
}
