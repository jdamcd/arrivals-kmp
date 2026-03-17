package com.jdamcd.arrivals

import platform.Foundation.NSUserDefaults

actual class Settings actual constructor() {
    private val defaults: NSUserDefaults = NSUserDefaults(suiteName = SettingsConfig.STORE_NAME)

    companion object {
        private const val COLD_START_STOP = "910GSHRDHST"
        private const val COLD_START_PLATFORM = "2"
    }

    actual var mode: String
        get() = defaults.stringForKey(SettingsConfig.MODE) ?: SettingsConfig.MODE_TFL
        set(value) {
            defaults.setObject(value, SettingsConfig.MODE)
        }

    actual var stopId: String
        get() = defaults.stringForKey(SettingsConfig.STOP) ?: COLD_START_STOP
        set(value) {
            defaults.setObject(value, SettingsConfig.STOP)
        }

    actual var platform: String
        get() = defaults.stringForKey(SettingsConfig.PLATFORM) ?: COLD_START_PLATFORM
        set(value) {
            defaults.setObject(value, SettingsConfig.PLATFORM)
        }

    actual var line: String
        get() = defaults.stringForKey(SettingsConfig.LINE) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.LINE)
        }

    actual var direction: String
        get() = defaults.stringForKey(SettingsConfig.DIRECTION) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.DIRECTION)
        }

    actual var gtfsStopsUpdated: Long
        get() = defaults.integerForKey(SettingsConfig.GTFS_STOPS_UPDATED)
        set(value) {
            defaults.setInteger(value, SettingsConfig.GTFS_STOPS_UPDATED)
        }

    actual var gtfsRealtime: String
        get() = defaults.stringForKey(SettingsConfig.GTFS_REALTIME) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.GTFS_REALTIME)
        }

    actual var gtfsSchedule: String
        get() = defaults.stringForKey(SettingsConfig.GTFS_SCHEDULE) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.GTFS_SCHEDULE)
        }

    actual var gtfsApiKey: String
        get() = defaults.stringForKey(SettingsConfig.GTFS_API_KEY) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.GTFS_API_KEY)
        }

    actual var gtfsApiKeyParam: String
        get() = defaults.stringForKey(SettingsConfig.GTFS_API_KEY_PARAM) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.GTFS_API_KEY_PARAM)
        }
}
