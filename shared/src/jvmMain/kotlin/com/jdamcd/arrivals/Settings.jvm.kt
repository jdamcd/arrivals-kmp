package com.jdamcd.arrivals

actual class Settings actual constructor() {
    actual var mode = SettingsConfig.MODE_TFL
    actual var stopId = ""
    actual var platform = ""
    actual var line = ""
    actual var direction = SettingsConfig.DIRECTION_DEFAULT
    actual var gtfsStopsUpdated = 0L
    actual var gtfsRealtime = ""
    actual var gtfsSchedule = ""
    actual var gtfsApiKey = ""
    actual var gtfsApiKeyParam = ""
}
