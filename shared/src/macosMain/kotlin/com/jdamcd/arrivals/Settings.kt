package com.jdamcd.arrivals

import platform.Foundation.NSUserDefaults

actual class Settings actual constructor() {
    private val defaults: NSUserDefaults = NSUserDefaults(suiteName = SettingsConfig.STORE_NAME)

    actual var mode: String
        get() = defaults.stringForKey(SettingsConfig.MODE) ?: SettingsConfig.MODE_TFL
        set(value) {
            defaults.setObject(value, SettingsConfig.MODE)
        }

    actual var tflStopId: String
        get() = defaults.stringForKey(SettingsConfig.TFL_STOP_ID) ?: SettingsConfig.TFL_STOP_ID_DEFAULT
        set(value) {
            defaults.setObject(value, SettingsConfig.TFL_STOP_ID)
        }

    actual var tflPlatform: String
        get() = defaults.stringForKey(SettingsConfig.TFL_PLATFORM) ?: SettingsConfig.TFL_PLATFORM_DEFAULT
        set(value) {
            defaults.setObject(value, SettingsConfig.TFL_PLATFORM)
        }

    actual var tflDirection: String
        get() = defaults.stringForKey(SettingsConfig.TFL_DIRECTION) ?: SettingsConfig.TFL_DIRECTION_DEFAULT
        set(value) {
            defaults.setObject(value, SettingsConfig.TFL_DIRECTION)
        }

    actual var gtfsStopsUpdated: Long
        get() = defaults.integerForKey(SettingsConfig.GTFS_STOPS_UPDATED)
        set(value) {
            defaults.setInteger(value, SettingsConfig.GTFS_STOPS_UPDATED)
        }

    actual var gtfsRealtime: String
        get() = defaults.stringForKey(SettingsConfig.GTFS_REALTIME) ?: SettingsConfig.GTFS_REALTIME_DEFAULT
        set(value) {
            defaults.setObject(value, SettingsConfig.GTFS_REALTIME)
        }

    actual var gtfsSchedule: String
        get() = defaults.stringForKey(SettingsConfig.GTFS_SCHEDULE) ?: SettingsConfig.GTFS_SCHEDULE_DEFAULT
        set(value) {
            defaults.setObject(value, SettingsConfig.GTFS_SCHEDULE)
        }

    actual var gtfsStop: String
        get() = defaults.stringForKey(SettingsConfig.GTFS_STOP) ?: SettingsConfig.GTFS_STOP_DEFAULT
        set(value) {
            defaults.setObject(value, SettingsConfig.GTFS_STOP)
        }
}
