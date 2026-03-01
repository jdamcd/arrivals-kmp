package com.jdamcd.arrivals.gtfs.system

import com.jdamcd.arrivals.BuildKonfig

object Bart {

    const val SCHEDULE = "https://api.511.org/transit/datafeeds?operator_id=BA"

    const val REALTIME = "https://api.511.org/transit/tripupdates?agency=BA"

    val API_KEY = BuildKonfig.ORG_511_KEY
}
