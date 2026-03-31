package com.jdamcd.arrivals.tfl

import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.Settings
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
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
        ApiArrival(123, "Test Stop", "Platform 2", "outbound", "New Cross", 456, "windrush"),
        ApiArrival(124, "Test Stop", "Platform 2", "outbound", "Crystal Palace Rail Station", 10, "windrush"),
        ApiArrival(125, "Test Stop", "Platform 1", "inbound", "Dalston Junction", 10, "mildmay"),
        ApiArrival(126, "Test Stop", "Platform 1", "inbound", "Highbury & Islington Underground Station", 456, "mildmay")
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
        first.displayTime shouldBe "Due"
        first.secondsToStop shouldBe 10
        first.line shouldBe null
        first.displayName shouldBe "Crystal Palace"
        first.lineBadge?.label shouldBe "O"
        first.lineBadge?.color shouldBe "D22730"
        val second = latest.arrivals[1]
        second.destination shouldBe "New Cross"
        second.displayTime shouldBe "8 min"
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
    fun `terminal station throws when no timetable available`() = runBlocking<Unit> {
        settings.platform = ""
        val terminalResponse = listOf(
            ApiArrival(1, "Brixton Underground Station", "Platform 1", null, "Brixton Underground Station", 60),
            ApiArrival(2, "Brixton Underground Station", "Platform 1", null, "Brixton Underground Station", 120),
            ApiArrival(3, "Brixton Underground Station", "Platform 1", "outbound", "Walthamstow Central Underground Station", 180)
        )
        coEvery { api.fetchArrivals("123") } returns terminalResponse

        assertFailsWith<NoDataException> {
            arrivals.latest()
        }
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
        latest.arrivals[0].displayTime shouldBe "10 min*"
        latest.arrivals[0].realtime shouldBe false
        latest.arrivals[1].destination shouldBe "Walthamstow Central"
        latest.arrivals[1].displayTime shouldBe "20 min*"
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

    @Test
    fun `filter out terminal platforms but keep through lines`() = runBlocking<Unit> {
        settings.platform = ""
        // Simulates H&I: Platforms 1-2 terminal (Windrush), Platforms 7-8 through line (Mildmay)
        val response = listOf(
            ApiArrival(1, "Highbury & Islington Rail Station", "Platform 1", null, "Highbury & Islington Rail Station", 60),
            ApiArrival(2, "Highbury & Islington Rail Station", "Platform 1", null, "Dalston Junction Rail Station", 3),
            ApiArrival(3, "Highbury & Islington Rail Station", "Platform 7", "outbound", "Stratford Rail Station", 200),
            ApiArrival(4, "Highbury & Islington Rail Station", "Platform 8", "inbound", "Richmond Rail Station", 400)
        )
        coEvery { api.fetchArrivals("123") } returns response

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 2
        latest.arrivals[0].destination shouldBe "Stratford"
        latest.arrivals[0].realtime shouldBe true
        latest.arrivals[1].destination shouldBe "Richmond"
        latest.arrivals[1].realtime shouldBe true
    }

    @Test
    fun `all terminal platforms and no schedule throws NoDataException`() = runBlocking<Unit> {
        settings.platform = ""
        // Simulates Overground end of line
        val response = listOf(
            ApiArrival(1, "New Cross ELL Rail Station", "Platform 1", null, "New Cross ELL Rail Station", 300),
            ApiArrival(2, "New Cross ELL Rail Station", "Platform 1", null, "New Cross ELL Rail Station", 600),
            ApiArrival(3, "New Cross ELL Rail Station", "Platform 1", null, "Dalston Junction Rail Station", 3, lineId = "windrush")
        )
        coEvery { api.fetchArrivals("123") } returns response

        // Platform 1 is terminal, no other platforms → NoDataException
        assertFailsWith<NoDataException> {
            arrivals.latest()
        }
    }

    @Test
    fun `self-ref and non-self-ref on same terminal platform uses timetable`() = runBlocking<Unit> {
        settings.platform = ""
        val response = listOf(
            ApiArrival(1, "Brixton Underground Station", "Platform 1", null, "Brixton Underground Station", 60, lineId = "victoria"),
            ApiArrival(2, "Brixton Underground Station", "Platform 1", null, "Brixton Underground Station", 120, lineId = "victoria"),
            ApiArrival(3, "Brixton Underground Station", "Platform 1", "outbound", "Walthamstow Central Underground Station", 180, lineId = "victoria")
        )
        coEvery { api.fetchArrivals("123") } returns response
        coEvery { api.fetchTimetable("victoria", "123") } returns timetableResponse()

        val latest = arrivals.latest()

        // All on Platform 1 which has self-ref → all filtered → timetable
        latest.arrivals shouldHaveSize 2
        latest.arrivals[0].destination shouldBe "Walthamstow Central"
        latest.arrivals[0].realtime shouldBe false
    }

    @Test
    fun `empty destination treated as self-referencing at terminal`() = runBlocking<Unit> {
        settings.platform = ""
        val response = listOf(
            ApiArrival(1, "Amersham Underground Station", "Platform 1", null, "", 60, lineId = "metropolitan"),
            ApiArrival(2, "Amersham Underground Station", "Platform 1", null, "", 120, lineId = "metropolitan")
        )
        coEvery { api.fetchArrivals("123") } returns response
        coEvery { api.fetchTimetable("metropolitan", "123") } returns timetableResponse()

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 2
        latest.arrivals[0].realtime shouldBe false
    }

    @Test
    fun `empty destinations filtered from non-terminal platform arrivals`() = runBlocking<Unit> {
        settings.platform = ""
        // Empty dest on Platform 1 (terminal), valid arrivals on Platform 2
        val response = listOf(
            ApiArrival(1, "Test Stop", "Platform 1", "outbound", "", 60),
            ApiArrival(2, "Test Stop", "Platform 2", "outbound", "Chesham Underground Station", 120),
            ApiArrival(3, "Test Stop", "Platform 2", "outbound", "Amersham Underground Station", 180),
            ApiArrival(4, "Test Stop", "Platform 2", "outbound", "Uxbridge Underground Station", 240)
        )
        coEvery { api.fetchArrivals("123") } returns response

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 3
        latest.arrivals[0].destination shouldBe "Chesham"
    }

    @Test
    fun `terminal station with empty lineIds throws NoDataException`() = runBlocking<Unit> {
        settings.platform = ""
        val response = listOf(
            ApiArrival(1, "Brixton Underground Station", "Platform 1", null, "Brixton Underground Station", 60),
            ApiArrival(2, "Brixton Underground Station", "Platform 1", null, "Brixton Underground Station", 120)
        )
        coEvery { api.fetchArrivals("123") } returns response

        assertFailsWith<NoDataException> {
            arrivals.latest()
        }
    }

    @Test
    fun `multiple lineIds at terminal merges departures`() = runBlocking<Unit> {
        settings.platform = ""
        val terminalResponse = listOf(
            ApiArrival(1, "Stockwell Underground Station", "Platform 1", null, "Stockwell Underground Station", 60, lineId = "victoria"),
            ApiArrival(2, "Stockwell Underground Station", "Platform 1", null, "Stockwell Underground Station", 120, lineId = "northern")
        )
        coEvery { api.fetchArrivals("123") } returns terminalResponse
        coEvery { api.fetchTimetable("victoria", "123") } returns timetableResponse()
        coEvery { api.fetchTimetable("northern", "123") } returns northernTimetableResponse()

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 3
        // Sorted by secondsToStop: northern 5 min, victoria 10 min, northern 15 min
        latest.arrivals[0].destination shouldBe "Morden"
        latest.arrivals[0].displayTime shouldBe "5 min*"
        latest.arrivals[1].destination shouldBe "Walthamstow Central"
        latest.arrivals[1].displayTime shouldBe "10 min*"
        latest.arrivals[2].destination shouldBe "Morden"
        latest.arrivals[2].displayTime shouldBe "15 min*"
    }

    @Test
    fun `timetable error for one line still returns other line departures`() = runBlocking<Unit> {
        settings.platform = ""
        val terminalResponse = listOf(
            ApiArrival(1, "Stockwell Underground Station", "Platform 1", null, "Stockwell Underground Station", 60, lineId = "victoria"),
            ApiArrival(2, "Stockwell Underground Station", "Platform 1", null, "Stockwell Underground Station", 120, lineId = "northern")
        )
        coEvery { api.fetchArrivals("123") } returns terminalResponse
        coEvery { api.fetchTimetable("victoria", "123") } throws RuntimeException("API error")
        coEvery { api.fetchTimetable("northern", "123") } returns northernTimetableResponse()

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 2
        latest.arrivals[0].destination shouldBe "Morden"
    }

    @Test
    fun `scheduled departures more than 2 hours in the future are filtered out`() = runBlocking<Unit> {
        settings.platform = ""
        val terminalResponse = listOf(
            ApiArrival(1, "Brixton Underground Station", "Platform 1", null, "Brixton Underground Station", 60, lineId = "victoria")
        )
        val farFutureTimetable = ApiTimetableResponse(
            stops = listOf(
                ApiTimetableStation("940GZZLUBXN", "Brixton Underground Station"),
                ApiTimetableStation("940GZZLUWWL", "Walthamstow Central Underground Station")
            ),
            timetable = ApiTimetable(
                routes = listOf(
                    ApiTimetableRoute(
                        stationIntervals = listOf(
                            ApiStationInterval(
                                id = "0",
                                intervals = listOf(ApiStopInterval("940GZZLUWWL", 30.0))
                            )
                        ),
                        schedules = listOf(
                            ApiTimetableSchedule(
                                name = "Saturday",
                                knownJourneys = listOf(
                                    ApiKnownJourney("20", "00", 0) // 8 hours away from 12:00
                                )
                            )
                        )
                    )
                )
            )
        )
        coEvery { api.fetchArrivals("123") } returns terminalResponse
        coEvery { api.fetchTimetable("victoria", "123") } returns farFutureTimetable

        assertFailsWith<NoDataException> {
            arrivals.latest()
        }
    }

    @Test
    fun `invalid hour in timetable journey is skipped`() = runBlocking<Unit> {
        settings.platform = ""
        val terminalResponse = listOf(
            ApiArrival(1, "Brixton Underground Station", "Platform 1", null, "Brixton Underground Station", 60, lineId = "victoria")
        )
        val badJourneyTimetable = ApiTimetableResponse(
            stops = listOf(
                ApiTimetableStation("940GZZLUBXN", "Brixton Underground Station"),
                ApiTimetableStation("940GZZLUWWL", "Walthamstow Central Underground Station")
            ),
            timetable = ApiTimetable(
                routes = listOf(
                    ApiTimetableRoute(
                        stationIntervals = listOf(
                            ApiStationInterval(
                                id = "0",
                                intervals = listOf(ApiStopInterval("940GZZLUWWL", 30.0))
                            )
                        ),
                        schedules = listOf(
                            ApiTimetableSchedule(
                                name = "Saturday",
                                knownJourneys = listOf(
                                    ApiKnownJourney("abc", "10", 0), // Invalid hour
                                    ApiKnownJourney("12", "10", 0) // Valid
                                )
                            )
                        )
                    )
                )
            )
        )
        coEvery { api.fetchArrivals("123") } returns terminalResponse
        coEvery { api.fetchTimetable("victoria", "123") } returns badJourneyTimetable

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 1
        latest.arrivals[0].displayTime shouldBe "10 min*"
    }

    @Test
    fun `findScheduleForDay weekday matches Monday to Friday range`() {
        val schedules = listOf(
            ApiTimetableSchedule("Monday - Friday", knownJourneys = emptyList())
        )
        findScheduleForDay(schedules, DayOfWeek.TUESDAY)?.name shouldBe "Monday - Friday"
    }

    @Test
    fun `findScheduleForDay Friday exact match takes priority over range`() {
        val schedules = listOf(
            ApiTimetableSchedule("Monday - Thursday", knownJourneys = emptyList()),
            ApiTimetableSchedule("Friday", knownJourneys = emptyList())
        )
        findScheduleForDay(schedules, DayOfWeek.FRIDAY)?.name shouldBe "Friday"
    }

    @Test
    fun `findScheduleForDay Sunday falls back to first schedule`() {
        val schedules = listOf(
            ApiTimetableSchedule("Saturday", knownJourneys = emptyList()),
            ApiTimetableSchedule("Monday - Friday", knownJourneys = emptyList())
        )
        findScheduleForDay(schedules, DayOfWeek.SUNDAY)?.name shouldBe "Saturday"
    }

    @Test
    fun `findScheduleForDay Mondays pattern matches other weekdays`() {
        val schedules = listOf(
            ApiTimetableSchedule("Mondays", knownJourneys = emptyList())
        )
        findScheduleForDay(schedules, DayOfWeek.TUESDAY)?.name shouldBe "Mondays"
    }

    @Test
    fun `findScheduleForDay weekday pattern matches weekday`() {
        val schedules = listOf(
            ApiTimetableSchedule("weekday", knownJourneys = emptyList())
        )
        findScheduleForDay(schedules, DayOfWeek.WEDNESDAY)?.name shouldBe "weekday"
    }

    @Test
    fun `findScheduleForDay empty schedules returns null`() {
        findScheduleForDay(emptyList(), DayOfWeek.MONDAY) shouldBe null
    }

    @Test
    fun `resolveDestinations with empty intervals`() {
        val response = ApiTimetableResponse(
            stops = listOf(ApiTimetableStation("A", "Station A")),
            timetable = ApiTimetable(
                routes = listOf(
                    ApiTimetableRoute(
                        stationIntervals = listOf(
                            ApiStationInterval(id = "0", intervals = emptyList())
                        )
                    )
                )
            )
        )
        resolveDestinations(response) shouldBe emptyMap()
    }

    @Test
    fun `resolveDestinations with unknown stopId`() {
        val response = ApiTimetableResponse(
            stops = listOf(ApiTimetableStation("A", "Station A")),
            timetable = ApiTimetable(
                routes = listOf(
                    ApiTimetableRoute(
                        stationIntervals = listOf(
                            ApiStationInterval(
                                id = "0",
                                intervals = listOf(ApiStopInterval("UNKNOWN", 10.0))
                            )
                        )
                    )
                )
            )
        )
        resolveDestinations(response) shouldBe emptyMap()
    }

    @Test
    fun `resolveDestinations with non-numeric interval id`() {
        val response = ApiTimetableResponse(
            stops = listOf(ApiTimetableStation("A", "Station A")),
            timetable = ApiTimetable(
                routes = listOf(
                    ApiTimetableRoute(
                        stationIntervals = listOf(
                            ApiStationInterval(
                                id = "abc",
                                intervals = listOf(ApiStopInterval("A", 10.0))
                            )
                        )
                    )
                )
            )
        )
        resolveDestinations(response) shouldBe emptyMap()
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

    private fun northernTimetableResponse() = ApiTimetableResponse(
        stops = listOf(
            ApiTimetableStation("940GZZLUMED", "Camden Town Underground Station"),
            ApiTimetableStation("940GZZLUMDN", "Morden Underground Station")
        ),
        timetable = ApiTimetable(
            routes = listOf(
                ApiTimetableRoute(
                    stationIntervals = listOf(
                        ApiStationInterval(
                            id = "0",
                            intervals = listOf(
                                ApiStopInterval("940GZZLUMDN", 25.0)
                            )
                        )
                    ),
                    schedules = listOf(
                        ApiTimetableSchedule(
                            name = "Saturday",
                            knownJourneys = listOf(
                                ApiKnownJourney("12", "05", 0),
                                ApiKnownJourney("12", "15", 0)
                            )
                        )
                    )
                )
            )
        )
    )
}
