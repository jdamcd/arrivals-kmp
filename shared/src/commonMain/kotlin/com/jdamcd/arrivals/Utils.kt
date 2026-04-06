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
 *
 * TODO: Investigate Unicode NFKD normalisation to support more languages
 */
private val LED_ALLOWED = Regex("[a-zA-Z0-9 \\-'&*+:,.]")
private val LED_CHAR_MAP = mapOf(
    '\u2018' to "'",
    '\u2019' to "'",
    '\u2013' to "-",
    '\u2014' to "-",
    '\u00df' to "ss",
    '\u00e4' to "ae",
    '\u00f6' to "oe",
    '\u00fc' to "ue",
    '\u00c4' to "Ae",
    '\u00d6' to "Oe",
    '\u00dc' to "Ue"
)

fun filterLedChars(input: String): String = buildString(input.length) {
    for (c in input) {
        val mapped = LED_CHAR_MAP[c]
        when {
            mapped != null -> append(mapped)
            LED_ALLOWED.matches(c.toString()) -> append(c)
        }
    }
}
