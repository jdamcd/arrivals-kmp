package com.jdamcd.arrivals.gtfs

import com.jdamcd.arrivals.gtfs.csv.parseCsvRows

class GtfsStops(stops: String) {

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
