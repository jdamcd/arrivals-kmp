package com.jdamcd.arrivals

actual class Settings actual constructor() {
    actual var mode = SettingsConfig.MODE_TFL
    actual var tflStopId = SettingsConfig.TFL_STOP_ID_DEFAULT
    actual var tflPlatform = SettingsConfig.TFL_PLATFORM_DEFAULT
    actual var tflDirection = SettingsConfig.TFL_DIRECTION_DEFAULT
    actual var gtfsStopsUpdated = 0L
    actual var gtfsRealtime = SettingsConfig.GTFS_REALTIME_DEFAULT
    actual var gtfsSchedule = SettingsConfig.GTFS_SCHEDULE_DEFAULT
    actual var gtfsStop = SettingsConfig.GTFS_STOP_DEFAULT
}
