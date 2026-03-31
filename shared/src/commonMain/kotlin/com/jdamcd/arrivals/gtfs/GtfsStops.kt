package com.jdamcd.arrivals.gtfs

internal class GtfsStops(stops: String) {

    private val stopIdToName: Map<String, String> =
        stops
            .split("\n")
            .filter { it.isNotBlank() }
            .let { lines ->
                val header = parseCsvLine(lines.first())
                val stopIdIndex = header.indexOf("stop_id")
                val stopNameIndex = header.indexOf("stop_name")
                lines
                    .drop(1) // Skip header
                    .mapNotNull { line ->
                        val parts = parseCsvLine(line)
                        if (parts.size > maxOf(stopIdIndex, stopNameIndex)) {
                            parts[stopIdIndex] to parts[stopNameIndex]
                        } else {
                            null
                        }
                    }
            }
            .toMap()

    fun stopIdToName(stopId: String) = stopIdToName[stopId]
}

internal fun parseCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    for (char in line) {
        when (char) {
            '\r' -> {}

            '"' -> inQuotes = !inQuotes

            ',' if !inQuotes -> {
                fields.add(current.toString())
                current.clear()
            }

            else -> current.append(char)
        }
    }
    fields.add(current.toString())
    return fields
}
