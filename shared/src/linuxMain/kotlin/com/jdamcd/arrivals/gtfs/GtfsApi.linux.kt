package com.jdamcd.arrivals.gtfs

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString
import platform.posix.mkdtemp

@OptIn(ExperimentalForeignApi::class)
actual fun getFilesDir(): String {
    val template = "/tmp/arrivals_XXXXXX"
    val tempDir = mkdtemp(template.cstr)?.toKString()
    return tempDir ?: "/tmp"
}
