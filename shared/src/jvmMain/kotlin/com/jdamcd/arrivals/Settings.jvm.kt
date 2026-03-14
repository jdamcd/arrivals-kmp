package com.jdamcd.arrivals

actual class Settings actual constructor() {
    // Shared
    actual var mode = SettingsConfig.MODE_GTFS
    actual var stopId = SettingsConfig.STOP_ID_DEFAULT
    actual var platform = SettingsConfig.PLATFORM_DEFAULT

    // TfL
    actual var tflDirection = SettingsConfig.TFL_DIRECTION_DEFAULT

    // GTFS
    actual var gtfsStopsUpdated = 0L
    actual var gtfsRealtime = SettingsConfig.GTFS_REALTIME_DEFAULT
    actual var gtfsSchedule = SettingsConfig.GTFS_SCHEDULE_DEFAULT
    actual var gtfsApiKey = SettingsConfig.GTFS_API_KEY_DEFAULT
    actual var gtfsApiKeyParam = SettingsConfig.GTFS_API_KEY_PARAM_DEFAULT

    // BVG
    actual var bvgLine = SettingsConfig.BVG_LINE_DEFAULT
}
