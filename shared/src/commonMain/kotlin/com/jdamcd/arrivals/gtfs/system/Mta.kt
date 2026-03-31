package com.jdamcd.arrivals.gtfs.system

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

    const val AGENCY_ID = "MTA NYCT"

    val expressOverrides = mapOf(
        "FX" to "F",
        "6X" to "6",
        "7X" to "7",
        "SS" to "SI"
    )
}
