package com.jdamcd.arrivals

import platform.Foundation.NSUserDefaults

class NSUserDefaultsSettings : Settings {
    private val defaults: NSUserDefaults = NSUserDefaults(suiteName = SettingsConfig.STORE_NAME)

    override var mode: String
        get() = defaults.stringForKey(SettingsConfig.MODE) ?: SettingsConfig.MODE_TFL
        set(value) {
            defaults.setObject(value, SettingsConfig.MODE)
        }

    override var stopId: String
        get() = defaults.stringForKey(SettingsConfig.STOP) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.STOP)
        }

    override var platform: String
        get() = defaults.stringForKey(SettingsConfig.PLATFORM) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.PLATFORM)
        }

    override var line: String
        get() = defaults.stringForKey(SettingsConfig.LINE) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.LINE)
        }

    override var direction: String
        get() = defaults.stringForKey(SettingsConfig.DIRECTION) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.DIRECTION)
        }

    override var gtfsStopsUpdated: Long
        get() = defaults.integerForKey(SettingsConfig.GTFS_STOPS_UPDATED)
        set(value) {
            defaults.setInteger(value, SettingsConfig.GTFS_STOPS_UPDATED)
        }

    override var gtfsRealtime: String
        get() = defaults.stringForKey(SettingsConfig.GTFS_REALTIME) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.GTFS_REALTIME)
        }

    override var gtfsSchedule: String
        get() = defaults.stringForKey(SettingsConfig.GTFS_SCHEDULE) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.GTFS_SCHEDULE)
        }

    override var gtfsApiKey: String
        get() = defaults.stringForKey(SettingsConfig.GTFS_API_KEY) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.GTFS_API_KEY)
        }

    override var gtfsApiKeyParam: String
        get() = defaults.stringForKey(SettingsConfig.GTFS_API_KEY_PARAM) ?: ""
        set(value) {
            defaults.setObject(value, SettingsConfig.GTFS_API_KEY_PARAM)
        }
}
