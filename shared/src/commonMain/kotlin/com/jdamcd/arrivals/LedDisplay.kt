package com.jdamcd.arrivals

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
