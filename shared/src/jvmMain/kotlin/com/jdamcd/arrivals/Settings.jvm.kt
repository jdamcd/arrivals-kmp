package com.jdamcd.arrivals

actual class Settings actual constructor() {
    actual var mode = SettingsConfig.MODE_TFL
    actual var stopId = ""
    actual var platform = ""
    actual var line = ""
    actual var direction = SettingsConfig.DIRECTION_DEFAULT
    actual var gtfsStopsUpdated = 0L
    actual var gtfsRealtime = SettingsConfig.GTFS_REALTIME_DEFAULT
    actual var gtfsSchedule = SettingsConfig.GTFS_SCHEDULE_DEFAULT
    actual var gtfsApiKey = SettingsConfig.GTFS_API_KEY_DEFAULT
    actual var gtfsApiKeyParam = SettingsConfig.GTFS_API_KEY_PARAM_DEFAULT
}
