package com.jdamcd.arrivals.gtfs

internal class GtfsStops(stops: String) {

    private val stopIdToName: Map<String, String> = buildMap {
        val (header, rows) = parseCsvRows(stops)
        val stopIdIndex = header.indexOf("stop_id")
        val stopNameIndex = header.indexOf("stop_name")
        for (parts in rows) {
            if (parts.size > maxOf(stopIdIndex, stopNameIndex)) {
                put(parts[stopIdIndex], parts[stopNameIndex])
            }
        }
    }

    fun stopIdToName(stopId: String) = stopIdToName[stopId]
}

internal fun parseCsvRows(csv: String): Pair<List<String>, List<List<String>>> {
    val lines = csv.split("\n").filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList<String>() to emptyList()
    val header = parseCsvLine(lines.first())
    val rows = lines.drop(1).map { parseCsvLine(it) }
    return header to rows
}

private fun parseCsvLine(line: String): List<String> {
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
