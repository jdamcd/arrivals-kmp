package com.jdamcd.arrivals.gtfs

import com.jdamcd.arrivals.Fixtures
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GtfsStopsTest {

    private lateinit var stops: GtfsStops

    @Test
    fun `maps stop ID to name`() {
        stops = GtfsStops(Fixtures.STOPS_CSV_1)

        stops.stopIdToName("G28") shouldBe "Nassau Av"
        stops.stopIdToName("G22S") shouldBe "Court Sq"
    }

    @Test
    fun `maps stop ID to name with different CSV orderings`() {
        stops = GtfsStops(Fixtures.STOPS_CSV_2)

        stops.stopIdToName("1141") shouldBe "Evergreen Blvd & Farview Dr"
        stops.stopIdToName("3254") shouldBe "Hwy 99 & NE 104th St"
    }

    @Test
    fun `maps invalid stop ID to null`() {
        stops = GtfsStops(Fixtures.STOPS_CSV_1)

        stops.stopIdToName("123456") shouldBe null
    }

    @Test
    fun `maps stop ID to name with quoted CSV fields`() {
        stops = GtfsStops(Fixtures.STOPS_CSV_QUOTED)

        stops.stopIdToName("place-north") shouldBe "North Station"
        stops.stopIdToName("place-TC") shouldBe "Transit Center, Bay 1"
    }

    @Test
    fun `does not map header row`() {
        stops = GtfsStops(Fixtures.STOPS_CSV_2)

        stops.stopIdToName("stop_id") shouldBe null
    }
}
