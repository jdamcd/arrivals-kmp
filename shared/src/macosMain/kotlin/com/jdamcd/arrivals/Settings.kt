package com.jdamcd.arrivals

import platform.Foundation.NSUserDefaults

actual class Settings actual constructor() {
    private val defaults: NSUserDefaults = NSUserDefaults(suiteName = SettingsConfig.STORE_NAME)

    // Shared
    actual var mode: String
        get() = defaults.stringForKey(SettingsConfig.MODE) ?: SettingsConfig.MODE_TFL
        set(value) {
            defaults.setObject(value, SettingsConfig.MODE)
        }

    actual var stopId: String
        get() = defaults.stringForKey(SettingsConfig.STOP_ID) ?: SettingsConfig.STOP_ID_DEFAULT
        set(value) {
            defaults.setObject(value, SettingsConfig.STOP_ID)
        }

    actual var platform: String
        get() = defaults.stringForKey(SettingsConfig.PLATFORM) ?: SettingsConfig.PLATFORM_DEFAULT
        set(value) {
            defaults.setObject(value, SettingsConfig.PLATFORM)
        }

    // TfL
    actual var tflDirection: String
        get() = defaults.stringForKey(SettingsConfig.TFL_DIRECTION) ?: SettingsConfig.TFL_DIRECTION_DEFAULT
        set(value) {
            defaults.setObject(value, SettingsConfig.TFL_DIRECTION)
        }

    // GTFS
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

    actual var gtfsApiKey: String
        get() = defaults.stringForKey(SettingsConfig.GTFS_API_KEY) ?: SettingsConfig.GTFS_API_KEY_DEFAULT
        set(value) {
            defaults.setObject(value, SettingsConfig.GTFS_API_KEY)
        }

    actual var gtfsApiKeyParam: String
        get() = defaults.stringForKey(SettingsConfig.GTFS_API_KEY_PARAM) ?: SettingsConfig.GTFS_API_KEY_PARAM_DEFAULT
        set(value) {
            defaults.setObject(value, SettingsConfig.GTFS_API_KEY_PARAM)
        }

    // BVG
    actual var bvgLine: String
        get() = defaults.stringForKey(SettingsConfig.BVG_LINE) ?: SettingsConfig.BVG_LINE_DEFAULT
        set(value) {
            defaults.setObject(value, SettingsConfig.BVG_LINE)
        }
}
