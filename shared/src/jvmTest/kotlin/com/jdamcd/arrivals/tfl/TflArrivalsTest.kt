package com.jdamcd.arrivals.tfl

import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.Settings
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant

class TflArrivalsTest {

    private val api = mockk<TflApi>()
    private val settings = Settings()

    // 2026-03-07T12:00:00Z = Saturday
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.fromEpochSeconds(1772884800)
    }
    private val arrivals = TflArrivals(api, settings, fixedClock)

    private val response = listOf(
        ApiArrival(123, "Test Stop", "Platform 2", "outbound", "New Cross", 456),
        ApiArrival(124, "Test Stop", "Platform 2", "outbound", "Crystal Palace Rail Station", 10),
        ApiArrival(125, "Test Stop", "Platform 1", "inbound", "Dalston Junction", 10),
        ApiArrival(126, "Test Stop", "Platform 1", "inbound", "Highbury & Islington Underground Station", 456)
    )

    @BeforeTest
    fun setup() {
        settings.stopId = "123"
        settings.platform = "2"
        settings.direction = ""
    }

    @Test
    fun `fetches latest arrivals`() = runBlocking<Unit> {
        coEvery { api.fetchArrivals("123") } returns response

        val latest = arrivals.latest()

        assertEquals(2, latest.arrivals.size)
        val first = latest.arrivals[0]
        first.destination shouldBe "Crystal Palace"
        first.time shouldBe "Due"
        first.secondsToStop shouldBe 10
        val second = latest.arrivals[1]
        second.destination shouldBe "New Cross"
        second.time shouldBe "8 min"
        second.secondsToStop shouldBe 456
    }

    @Test
    fun `formats station name with filters`() = runBlocking<Unit> {
        coEvery { api.fetchArrivals("123") } returns response

        settings.direction = ""
        settings.platform = "2"
        arrivals.latest().station shouldBe "Test Stop: Platform 2"

        settings.direction = "inbound"
        settings.platform = ""
        arrivals.latest().station shouldBe "Test Stop: Inbound"

        settings.direction = ""
        settings.platform = ""
        arrivals.latest().station shouldBe "Test Stop"
    }

    @Test
    fun `returns up to 3 arrivals`() = runBlocking<Unit> {
        settings.platform = ""
        coEvery { api.fetchArrivals("123") } returns response

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 3
    }

    @Test
    fun `throws NoDataException on empty results`() = runBlocking<Unit> {
        coEvery { api.fetchArrivals("123") } returns emptyList()

        assertFailsWith<NoDataException> {
            arrivals.latest()
        }
    }

    @Test
    fun `platform filter matches exact number`() = runBlocking<Unit> {
        val response = listOf(
            ApiArrival(1, "Test Stop", "Platform 2", "all", "Dest A", 100),
            ApiArrival(2, "Test Stop", "Platform 12", "all", "Dest B", 200),
            ApiArrival(3, "Test Stop", "Platform 21", "all", "Dest C", 300)
        )
        coEvery { api.fetchArrivals("123") } returns response

        settings.platform = "21"
        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 1
        latest.arrivals[0].destination shouldBe "Dest C"
    }

    @Test
    fun `platform filter does not match when filter is prefix of platform number`() = runBlocking<Unit> {
        val response = listOf(
            ApiArrival(1, "Test Stop", "Platform 1", "all", "Dest A", 100),
            ApiArrival(2, "Test Stop", "Platform 10", "all", "Dest B", 200),
            ApiArrival(3, "Test Stop", "Platform 11", "all", "Dest C", 300)
        )
        coEvery { api.fetchArrivals("123") } returns response

        settings.platform = "1"
        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 1
        latest.arrivals[0].destination shouldBe "Dest A"
    }

    @Test
    fun `filters self-referencing arrivals at terminal stations`() = runBlocking<Unit> {
        settings.platform = ""
        val terminalResponse = listOf(
            ApiArrival(1, "Brixton Underground Station", "Platform 1", null, "Brixton Underground Station", 60),
            ApiArrival(2, "Brixton Underground Station", "Platform 1", null, "Brixton Underground Station", 120),
            ApiArrival(3, "Brixton Underground Station", "Platform 1", "outbound", "Walthamstow Central Underground Station", 180)
        )
        coEvery { api.fetchArrivals("123") } returns terminalResponse

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 1
        latest.arrivals[0].destination shouldBe "Walthamstow Central"
    }

    @Test
    fun `falls back to timetable when all arrivals are self-referencing`() = runBlocking<Unit> {
        settings.platform = ""
        val terminalResponse = listOf(
            ApiArrival(
                1,
                "Brixton Underground Station",
                "Platform 1",
                null,
                "Brixton Underground Station",
                60,
                lineId = "victoria"
            ),
            ApiArrival(
                2,
                "Brixton Underground Station",
                "Platform 1",
                null,
                "Brixton Underground Station",
                120,
                lineId = "victoria"
            )
        )
        coEvery { api.fetchArrivals("123") } returns terminalResponse
        coEvery { api.fetchTimetable("victoria", "123") } returns timetableResponse()

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 2
        latest.arrivals[0].destination shouldBe "Walthamstow Central"
        latest.arrivals[0].time shouldBe "10 min*"
        latest.arrivals[0].realtime shouldBe false
        latest.arrivals[1].destination shouldBe "Walthamstow Central"
        latest.arrivals[1].time shouldBe "20 min*"
        latest.arrivals[1].realtime shouldBe false
    }

    @Test
    fun `throws NoDataException when timetable has no upcoming departures`() = runBlocking<Unit> {
        settings.platform = ""
        val terminalResponse = listOf(
            ApiArrival(
                1,
                "Brixton Underground Station",
                "Platform 1",
                null,
                "Brixton Underground Station",
                60,
                lineId = "victoria"
            )
        )
        val emptyTimetable = ApiTimetableResponse(
            stops = emptyList(),
            timetable = ApiTimetable(routes = emptyList())
        )
        coEvery { api.fetchArrivals("123") } returns terminalResponse
        coEvery { api.fetchTimetable("victoria", "123") } returns emptyTimetable

        assertFailsWith<NoDataException> {
            arrivals.latest()
        }
    }

    @Test
    fun `platform filter matches number with letter suffix`() = runBlocking<Unit> {
        val response = listOf(
            ApiArrival(1, "Test Stop", "Platform 2", "all", "Dest A", 100),
            ApiArrival(2, "Test Stop", "Platform 2A", "all", "Dest B", 200),
            ApiArrival(3, "Test Stop", "Platform 2B", "all", "Dest C", 300),
            ApiArrival(4, "Test Stop", "Platform 12", "all", "Dest D", 400)
        )
        coEvery { api.fetchArrivals("123") } returns response

        settings.platform = "2"
        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 3
        latest.arrivals[0].destination shouldBe "Dest A"
        latest.arrivals[1].destination shouldBe "Dest B"
        latest.arrivals[2].destination shouldBe "Dest C"
    }

    // Fixed clock is Saturday 12:00 UTC = 12:00 GMT (March, no DST)
    private fun timetableResponse() = ApiTimetableResponse(
        stops = listOf(
            ApiTimetableStation("940GZZLUBXN", "Brixton Underground Station"),
            ApiTimetableStation("940GZZLUSKW", "Stockwell Underground Station"),
            ApiTimetableStation("940GZZLUWWL", "Walthamstow Central Underground Station")
        ),
        timetable = ApiTimetable(
            routes = listOf(
                ApiTimetableRoute(
                    stationIntervals = listOf(
                        ApiStationInterval(
                            id = "0",
                            intervals = listOf(
                                ApiStopInterval("940GZZLUSKW", 2.0),
                                ApiStopInterval("940GZZLUWWL", 30.0)
                            )
                        )
                    ),
                    schedules = listOf(
                        ApiTimetableSchedule(
                            name = "Saturday",
                            knownJourneys = listOf(
                                ApiKnownJourney("12", "10", 0),
                                ApiKnownJourney("12", "20", 0),
                                ApiKnownJourney("11", "50", 0) // In the past
                            )
                        )
                    )
                )
            )
        )
    )
}
