package com.jdamcd.arrivals.bvg

import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.Settings
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BvgArrivalsTest {

    private val api = mockk<BvgApi>()
    private val settings = Settings()
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.fromEpochSeconds(1772884800) // 2026-03-07T12:00:00Z
    }
    private val arrivals = BvgArrivals(api, settings, fixedClock)

    private val mockStop = ApiBvgLocation(
        type = "stop",
        id = "900100003",
        name = "S+U Alexanderplatz Bhf (Berlin)"
    )

    private val mockDepartures = ApiBvgDepartureResponse(
        departures = listOf(
            ApiBvgDeparture(
                tripId = "trip1",
                direction = "S Westkreuz (Berlin)",
                line = ApiBvgLine(name = "S5", product = "suburban"),
                departureTime = "2026-03-07T12:05:00+00:00",
                plannedWhen = "2026-03-07T12:05:00+00:00",
                delay = 0,
                platform = "4"
            ),
            ApiBvgDeparture(
                tripId = "trip2",
                direction = "Pankow",
                line = ApiBvgLine(name = "U2", product = "subway"),
                departureTime = "2026-03-07T12:10:00+00:00",
                plannedWhen = "2026-03-07T12:10:00+00:00",
                delay = 0,
                platform = "2 (U2)"
            ),
            ApiBvgDeparture(
                tripId = "trip3",
                direction = "Hauptbahnhof",
                line = ApiBvgLine(name = "U5", product = "subway"),
                departureTime = "2026-03-07T12:15:00+00:00",
                plannedWhen = "2026-03-07T12:15:00+00:00",
                delay = 0,
                platform = "1 (U5)"
            )
        )
    )

    @BeforeTest
    fun setup() {
        settings.stopId = "900100003"
        settings.bvgLine = ""
        settings.platform = ""
    }

    @Test
    fun `fetches latest departures`() = runBlocking<Unit> {
        coEvery { api.fetchStop("900100003") } returns mockStop
        coEvery { api.fetchDepartures("900100003") } returns mockDepartures

        val latest = arrivals.latest()

        latest.station shouldBe "S+U Alexanderplatz"
        latest.arrivals shouldHaveSize 3
        latest.arrivals[0].destination shouldBe "S5 S Westkreuz (Berlin)"
    }

    @Test
    fun `includes line name in destination`() = runBlocking<Unit> {
        coEvery { api.fetchStop("900100003") } returns mockStop
        coEvery { api.fetchDepartures("900100003") } returns mockDepartures

        val latest = arrivals.latest()

        latest.arrivals[0].destination shouldContain "S5"
        latest.arrivals[1].destination shouldContain "U2"
    }

    @Test
    fun `filters cancelled departures`() = runBlocking<Unit> {
        val withCancelled = ApiBvgDepartureResponse(
            departures = listOf(
                ApiBvgDeparture(
                    tripId = "trip1",
                    direction = "Westkreuz",
                    line = ApiBvgLine(name = "S5", product = "suburban"),
                    departureTime = "2026-03-07T12:05:00+00:00",
                    plannedWhen = "2026-03-07T12:05:00+00:00",
                    delay = 0,
                    platform = "4"
                ),
                ApiBvgDeparture(
                    tripId = "trip2",
                    direction = "Pankow",
                    line = ApiBvgLine(name = "U2", product = "subway"),
                    departureTime = null,
                    plannedWhen = "2026-03-07T12:10:00+00:00",
                    delay = null,
                    platform = "2"
                )
            )
        )
        coEvery { api.fetchStop("900100003") } returns mockStop
        coEvery { api.fetchDepartures("900100003") } returns withCancelled

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 1
        latest.arrivals[0].destination shouldContain "S5"
    }

    @Test
    fun `applies line filter`() = runBlocking<Unit> {
        settings.bvgLine = "U2"
        coEvery { api.fetchStop("900100003") } returns mockStop
        coEvery { api.fetchDepartures("900100003") } returns mockDepartures

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 1
        latest.arrivals[0].destination shouldContain "U2"
        latest.arrivals[0].destination shouldContain "Pankow"
    }

    @Test
    fun `line filter is case insensitive`() = runBlocking<Unit> {
        settings.bvgLine = "s5"
        coEvery { api.fetchStop("900100003") } returns mockStop
        coEvery { api.fetchDepartures("900100003") } returns mockDepartures

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 1
        latest.arrivals[0].destination shouldContain "S5"
    }

    @Test
    fun `formats station name with line filter`() = runBlocking<Unit> {
        settings.bvgLine = "U2"
        coEvery { api.fetchStop("900100003") } returns mockStop
        coEvery { api.fetchDepartures("900100003") } returns mockDepartures

        val latest = arrivals.latest()

        latest.station shouldBe "S+U Alexanderplatz: U2"
    }

    @Test
    fun `formats station name with line and platform filter`() = runBlocking<Unit> {
        settings.bvgLine = "U2"
        settings.platform = "2"
        coEvery { api.fetchStop("900100003") } returns mockStop
        coEvery { api.fetchDepartures("900100003") } returns mockDepartures

        val latest = arrivals.latest()

        latest.station shouldBe "S+U Alexanderplatz: U2, Platform 2"
    }

    @Test
    fun `applies platform filter`() = runBlocking<Unit> {
        settings.platform = "4"
        coEvery { api.fetchStop("900100003") } returns mockStop
        coEvery { api.fetchDepartures("900100003") } returns mockDepartures

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 1
        latest.arrivals[0].destination shouldContain "S5"
    }

    @Test
    fun `formats station name with platform filter`() = runBlocking<Unit> {
        settings.platform = "4"
        coEvery { api.fetchStop("900100003") } returns mockStop
        coEvery { api.fetchDepartures("900100003") } returns mockDepartures

        val latest = arrivals.latest()

        latest.station shouldBe "S+U Alexanderplatz: Platform 4"
    }

    @Test
    fun `throws NoDataException on empty results`() = runBlocking<Unit> {
        coEvery { api.fetchStop("900100003") } returns mockStop
        coEvery { api.fetchDepartures("900100003") } returns ApiBvgDepartureResponse(departures = emptyList())

        assertFailsWith<NoDataException> {
            arrivals.latest()
        }
    }

    @Test
    fun `caches stop name across calls`() = runBlocking<Unit> {
        coEvery { api.fetchStop("900100003") } returns mockStop
        coEvery { api.fetchDepartures("900100003") } returns mockDepartures

        arrivals.latest()
        arrivals.latest()

        coVerify(exactly = 1) { api.fetchStop("900100003") }
    }

    @Test
    fun `throws NoDataException when stop not configured`() = runBlocking<Unit> {
        settings.stopId = ""

        val e = assertFailsWith<NoDataException> {
            arrivals.latest()
        }
        e.message shouldBe "No BVG stop configured"
    }

    @Test
    fun `returns up to 3 arrivals`() = runBlocking<Unit> {
        val manyDepartures = ApiBvgDepartureResponse(
            departures = List(10) { index ->
                ApiBvgDeparture(
                    tripId = "trip$index",
                    direction = "Dest$index",
                    line = ApiBvgLine(name = "U$index", product = "subway"),
                    departureTime = "2026-03-07T12:${(5 + index).toString().padStart(2, '0')}:00+00:00",
                    plannedWhen = "2026-03-07T12:${(5 + index).toString().padStart(2, '0')}:00+00:00",
                    delay = 0,
                    platform = "$index"
                )
            }
        )
        coEvery { api.fetchStop("900100003") } returns mockStop
        coEvery { api.fetchDepartures("900100003") } returns manyDepartures

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 3
    }

    @Test
    fun `searches stops and filters to stop type`() = runBlocking<Unit> {
        coEvery { api.searchStops("Alex") } returns listOf(
            ApiBvgLocation(type = "stop", id = "900100003", name = "S+U Alexanderplatz Bhf (Berlin)"),
            ApiBvgLocation(type = "location", id = null, name = "Alexanderplatz, Berlin")
        )

        val results = arrivals.searchStops("Alex")

        results shouldHaveSize 1
        results[0].id shouldBe "900100003"
        results[0].name shouldBe "S+U Alexanderplatz Bhf (Berlin)"
    }
}
