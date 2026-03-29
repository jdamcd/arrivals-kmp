package com.jdamcd.arrivals.gtfs.system

import com.jdamcd.arrivals.gtfs.RouteStyle

object Mta {

    const val SCHEDULE = "http://web.mta.info/developers/data/nyct/subway/google_transit.zip"
    const val REALTIME_BASE = "https://api-endpoint.mta.info/Dataservice/mtagtfsfeeds"

    val realtime = mapOf(
        "ACE" to "${REALTIME_BASE}/nyct%2Fgtfs-ace",
        "BDFM" to "${REALTIME_BASE}/nyct%2Fgtfs-bdfm",
        "G" to "${REALTIME_BASE}/nyct%2Fgtfs-g",
        "JZ" to "${REALTIME_BASE}/nyct%2Fgtfs-jz",
        "NQRW" to "${REALTIME_BASE}/nyct%2Fgtfs-nqrw",
        "L" to "${REALTIME_BASE}/nyct%2Fgtfs-l",
        "1-7" to "${REALTIME_BASE}/nyct%2Fgtfs",
        "SIR" to "${REALTIME_BASE}/nyct%2Fgtfs-si"
    )

    val routeStyles: Map<String, RouteStyle> = mapOf(
        "1" to RouteStyle(color = "EE352E"),
        "2" to RouteStyle(color = "EE352E"),
        "3" to RouteStyle(color = "EE352E"),
        "4" to RouteStyle(color = "00933C"),
        "5" to RouteStyle(color = "00933C"),
        "6" to RouteStyle(color = "00933C"),
        "7" to RouteStyle(color = "B933AD"),
        "A" to RouteStyle(color = "2850AD"),
        "C" to RouteStyle(color = "2850AD"),
        "E" to RouteStyle(color = "2850AD"),
        "B" to RouteStyle(color = "FF6319"),
        "D" to RouteStyle(color = "FF6319"),
        "F" to RouteStyle(color = "FF6319"),
        "M" to RouteStyle(color = "FF6319"),
        "G" to RouteStyle(color = "6CBE45"),
        "J" to RouteStyle(color = "996633"),
        "Z" to RouteStyle(color = "996633"),
        "L" to RouteStyle(color = "A7A9AC"),
        "N" to RouteStyle(color = "FCCC0A"),
        "Q" to RouteStyle(color = "FCCC0A"),
        "R" to RouteStyle(color = "FCCC0A"),
        "W" to RouteStyle(color = "FCCC0A"),
        "S" to RouteStyle(color = "808183"),
        "SI" to RouteStyle(label = "SIR", color = "08179C")
    )
}
