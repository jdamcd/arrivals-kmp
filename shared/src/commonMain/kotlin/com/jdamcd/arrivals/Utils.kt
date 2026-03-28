package com.jdamcd.arrivals

import kotlin.math.roundToInt

internal const val MAX_SECONDS_AHEAD = 7200 // 2 hours

fun formatTime(seconds: Int, realtime: Boolean = true): String {
    val base = if (seconds < 60) "Due" else "${(seconds / 60f).roundToInt()} min"
    return if (realtime) base else "$base*"
}

fun stripPlatform(input: String): String = if (input.startsWith("Platform ", ignoreCase = true)) {
    input.substring(9).trim()
} else {
    input.trim()
}

fun matchesPlatformFilter(platform: String, filter: String): Boolean {
    val platformNumber = stripPlatform(platform)
    val filterNumber = stripPlatform(filter)

    if (!platformNumber.startsWith(filterNumber, ignoreCase = true)) {
        return false
    }

    if (platformNumber.length == filterNumber.length) {
        return true // Exact match
    }

    if (filterNumber.last().isLetter()) {
        return false // Letter-ending filter must be exact (e.g. "A" doesn't match "AB")
    }

    val nextChar = platformNumber[filterNumber.length]
    return !nextChar.isDigit() // Allow "2A" but not "21" when filter is "2"
}

/*
 * The London Underground LED font supports A-Z, a-z, 0-9 and - ' & * + : , .
 * This utility normalises common symbols to ASCII and strips unsupported chars.
 */
private val LED_FILTER = Regex("[^a-zA-Z0-9 \\-'&*+:,.]")

fun filterLedChars(input: String): String = LED_FILTER.replace(
    input
        .replace('\u2018', '\'')
        .replace('\u2019', '\'')
        .replace('\u2013', '-')
        .replace('\u2014', '-'),
    ""
)
