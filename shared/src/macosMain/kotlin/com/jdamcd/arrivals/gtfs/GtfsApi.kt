package com.jdamcd.arrivals.gtfs

import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun getFilesDir(): String {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSApplicationSupportDirectory,
        NSUserDomainMask,
        true
    )
    val appSupportPath = paths.firstOrNull() as? String
        ?: throw IllegalStateException("Application Support Directory not found")
    return appSupportPath
}
