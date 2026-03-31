package com.jdamcd.arrivals.gtfs.csv

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
