package com.jdamcd.arrivals.tfl

import com.jdamcd.arrivals.InMemorySettings
import com.jdamcd.arrivals.NoDataException
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TflBusArrivalsTest {

    private val api = mockk<TflApi>()
    private val settings = InMemorySettings()
    private val arrivals = TflBusArrivals(api, settings)

    private val mockArrivals = listOf(
        busArrival(id = "1", line = "185", destination = "Victoria", seconds = 600),
        busArrival(id = "2", line = "40", destination = "Clerkenwell Green", seconds = 300),
        busArrival(id = "3", line = "N185", lineId = "n185", destination = "Victoria", seconds = 900),
        busArrival(id = "4", line = "185", destination = "Victoria", seconds = 1200)
    )

    @BeforeTest
    fun setup() {
        settings.stopId = "490001090D"
    }

    @Test
    fun `sorts arrivals by time and limits count`() = runBlocking<Unit> {
        coEvery { api.fetchArrivals("490001090D") } returns mockArrivals

        val latest = arrivals.latest(3)

        latest.arrivals shouldHaveSize 3
        latest.arrivals[0].destination shouldBe "Clerkenwell Green"
        latest.arrivals[0].line shouldBe "40"
        latest.arrivals[1].line shouldBe "185"
        latest.arrivals[2].line shouldBe "N185"
    }

    @Test
    fun `includes stop letter in station name`() = runBlocking<Unit> {
        coEvery { api.fetchArrivals("490001090D") } returns mockArrivals

        arrivals.latest().station shouldBe "East Dulwich Station (D)"
    }

    @Test
    fun `omits invalid stop letter from station name`() = runBlocking<Unit> {
        // "null" is what the feed sends for stops with no posted letter
        for (platform in listOf("->E", "null", "opp")) {
            coEvery { api.fetchArrivals("490001090D") } returns listOf(
                busArrival(id = "1", line = "185", destination = "Victoria", seconds = 600, platform = platform)
            )

            arrivals.latest().station shouldBe "East Dulwich Station"
        }
    }

    @Test
    fun `removes repeated predictions keeping the soonest`() = runBlocking<Unit> {
        coEvery { api.fetchArrivals("490001090D") } returns listOf(
            busArrival(id = "1", line = "185", destination = "Victoria", seconds = 1068),
            busArrival(id = "1", line = "185", destination = "Victoria", seconds = 1044),
            busArrival(id = "2", line = "40", destination = "Clerkenwell Green", seconds = 300)
        )

        val latest = arrivals.latest(3)

        latest.arrivals.map { it.secondsToStop } shouldBe listOf(300, 1044)
    }

    @Test
    fun `keeps both calls when a bus serves the stop in each direction`() = runBlocking<Unit> {
        // Same vehicle and stop, so TfL gives both calls the same prediction id
        coEvery { api.fetchArrivals("490001090D") } returns listOf(
            busArrival(id = "1", line = "324", destination = "Brent Cross, Tesco", seconds = 395, direction = "outbound"),
            busArrival(id = "1", line = "324", destination = "Elstree, Centennial Park", seconds = 515, direction = "inbound")
        )

        val latest = arrivals.latest(3)

        latest.arrivals.map { it.destination } shouldBe listOf("Brent Cross, Tesco", "Elstree, Centennial Park")
        latest.arrivals.map { it.id }.distinct() shouldHaveSize 2
    }

    @Test
    fun `arrival ids are stable between polls`() = runBlocking<Unit> {
        coEvery { api.fetchArrivals("490001090D") } returns listOf(
            busArrival(id = "1", line = "185", destination = "Victoria", seconds = 600)
        ) andThen listOf(
            busArrival(id = "1", line = "185", destination = "Victoria", seconds = 540)
        )

        arrivals.latest().arrivals[0].id shouldBe arrivals.latest().arrivals[0].id
    }

    @Test
    fun `keeps separate predictions that have no id`() = runBlocking<Unit> {
        coEvery { api.fetchArrivals("490001090D") } returns listOf(
            busArrival(id = "", line = "185", destination = "Victoria", seconds = 600),
            busArrival(id = "", line = "185", destination = "Victoria", seconds = 1200)
        )

        arrivals.latest(3).arrivals shouldHaveSize 2
    }

    @Test
    fun `sets route badge and display name`() = runBlocking<Unit> {
        coEvery { api.fetchArrivals("490001090D") } returns mockArrivals

        val arrival = arrivals.latest(1).arrivals[0]

        arrival.displayName shouldBe "40 - Clerkenwell Green"
        arrival.lineBadge?.label shouldBe "40"
        arrival.lineBadge?.color shouldBe "DC241F"
    }

    @Test
    fun `filters by route ignoring case and adds it to station name`() = runBlocking<Unit> {
        settings.line = "n185"
        coEvery { api.fetchArrivals("490001090D") } returns mockArrivals

        val latest = arrivals.latest()

        latest.station shouldBe "East Dulwich Station (D): N185"
        latest.arrivals shouldHaveSize 1
        latest.arrivals[0].line shouldBe "N185"
    }

    @Test
    fun `falls back to lineId when lineName is empty`() = runBlocking<Unit> {
        coEvery { api.fetchArrivals("490001090D") } returns listOf(
            busArrival(id = "1", line = "", lineId = "176", destination = "Penge", seconds = 60)
        )

        arrivals.latest().arrivals[0].line shouldBe "176"
    }

    @Test
    fun `throws NoDataException when no stop configured`() = runBlocking<Unit> {
        settings.stopId = ""

        assertFailsWith<NoDataException> { arrivals.latest() }
    }

    @Test
    fun `throws NoDataException when route filter matches nothing`() = runBlocking<Unit> {
        settings.line = "12"
        coEvery { api.fetchArrivals("490001090D") } returns mockArrivals

        assertFailsWith<NoDataException> { arrivals.latest() }
    }

    @Test
    fun `search marks stop groups as hubs`() = runBlocking<Unit> {
        coEvery { api.searchBusStops("east dulwich") } returns ApiSearchResult(
            matches = listOf(
                ApiMatchedStop("490001090D", "East Dulwich Station", "Camberwell Or Peckham"),
                ApiMatchedStop("490G00006336", "East Dulwich Sainsbury's")
            )
        )
        coEvery { api.stopDetails(listOf("490001090D")) } returns emptyList()

        val results = arrivals.searchStops("east dulwich")

        results[0].id shouldBe "490001090D"
        results[0].name shouldBe "East Dulwich Station towards Camberwell Or Peckham"
        results[0].isHub shouldBe false
        results[1].name shouldBe "East Dulwich Sainsbury's"
        results[1].isHub shouldBe true
    }

    @Test
    fun `search marks stop groups outside London as hubs`() = runBlocking<Unit> {
        coEvery { api.searchBusStops("any") } returns ApiSearchResult(
            matches = listOf(
                ApiMatchedStop("400G4409334A", "Victoria Almshouses"),
                ApiMatchedStop("037G0308", "Slough Grammar School"),
                ApiMatchedStop("910GDARTFD", "Dartford Rail Station"),
                ApiMatchedStop("HUBZCW", "Canada Water"),
                ApiMatchedStop("490000248G", "Victoria Station", "Sloane Square")
            )
        )
        coEvery { api.stopDetails(listOf("490000248G")) } returns emptyList()

        val results = arrivals.searchStops("any")

        // The last is a leaf stop: a trailing G in its id doesn't make it a group
        results.map { it.isHub } shouldBe listOf(true, true, true, true, false)
    }

    @Test
    fun `search adds stop letters to stops`() = runBlocking<Unit> {
        coEvery { api.searchBusStops("victoria") } returns ApiSearchResult(
            matches = listOf(
                ApiMatchedStop("490000248G", "Victoria Station", "Battersea"),
                ApiMatchedStop("490000248H", "Victoria Station", "Parliament Square"),
                ApiMatchedStop("490002139ZZ", "Victoria Station", "Parliament Square"),
                ApiMatchedStop("490G000813", "Victoria Coach Station")
            )
        )
        // Groups with a unique name aren't looked up
        coEvery { api.stopDetails(listOf("490000248G", "490000248H", "490002139ZZ")) } returns listOf(
            busStop("490000248G", "Victoria Station", letter = "G", towards = "Battersea"),
            busStop("490000248H", "Victoria Station", letter = "J", towards = "Parliament Square"),
            // A stop with a parent comes back nested in its group
            ApiStopPoint(
                commonName = "Victoria Station",
                naptanId = "490G00002139",
                stopType = "NaptanOnstreetBusCoachStopPair",
                children = listOf(
                    busStop("490002139ZZ", "Victoria Station", letter = "E", towards = "Parliament Square")
                )
            )
        )

        val results = arrivals.searchStops("victoria")

        results.map { it.id } shouldBe listOf("490000248G", "490000248H", "490002139ZZ", "490G000813")
        results.map { it.name } shouldBe listOf(
            "Victoria Station (G) towards Battersea",
            "Victoria Station (J) towards Parliament Square",
            "Victoria Station (E) towards Parliament Square",
            "Victoria Coach Station"
        )
    }

    @Test
    fun `search tells same-named groups apart by where their stops head`() = runBlocking<Unit> {
        coEvery { api.searchBusStops("victoria road") } returns ApiSearchResult(
            matches = listOf(
                ApiMatchedStop("490G00009432", "Victoria Road"),
                ApiMatchedStop("490G00014064", "Victoria Road"),
                ApiMatchedStop("490G000045", "Victoria Road (New Barnet)")
            )
        )
        coEvery { api.stopDetails(listOf("490G00009432", "490G00014064")) } returns listOf(
            stopGroup(
                "490G00009432",
                "Victoria Road",
                busStop("1", "Victoria Road", letter = "A", towards = "Ilford"),
                busStop("2", "Victoria Road", letter = "B", towards = "Ilford")
            ),
            stopGroup(
                "490G00014064",
                "Victoria Road",
                busStop("3", "Victoria Road", letter = "C", towards = "Northwood Hills"),
                busStop("4", "Victoria Road", letter = "D", towards = "Northolt")
            )
        )

        val results = arrivals.searchStops("victoria road")

        results.map { it.name } shouldBe listOf(
            "Victoria Road towards Ilford",
            "Victoria Road towards Northwood Hills / Northolt",
            "Victoria Road (New Barnet)"
        )
        results.map { it.isHub } shouldBe listOf(true, true, true)
    }

    @Test
    fun `search falls back to plain results when the lookup fails`() = runBlocking<Unit> {
        coEvery { api.searchBusStops("victoria") } returns ApiSearchResult(
            matches = listOf(ApiMatchedStop("490000248G", "Victoria Station", "Battersea"))
        )
        coEvery { api.stopDetails(listOf("490000248G")) } throws NoDataException("TfL API error")

        val results = arrivals.searchStops("victoria")

        results.map { it.name } shouldBe listOf("Victoria Station towards Battersea")
    }

    @Test
    fun `search doesn't swallow cancellation during the lookup`() = runBlocking<Unit> {
        coEvery { api.searchBusStops("victoria") } returns ApiSearchResult(
            matches = listOf(ApiMatchedStop("490000248G", "Victoria Station", "Battersea"))
        )
        coEvery { api.stopDetails(listOf("490000248G")) } throws CancellationException("cancelled")

        assertFailsWith<CancellationException> { arrivals.searchStops("victoria") }
    }

    @Test
    fun `ignores platform and direction settings`() = runBlocking<Unit> {
        settings.platform = "2"
        settings.direction = "inbound"
        coEvery { api.fetchArrivals("490001090D") } returns mockArrivals

        val latest = arrivals.latest(10)

        latest.arrivals shouldHaveSize 4
        latest.station shouldBe "East Dulwich Station (D)"
    }

    @Test
    fun `throws NoDataException for empty response`() = runBlocking<Unit> {
        coEvery { api.fetchArrivals("490001090D") } returns emptyList()

        assertFailsWith<NoDataException> { arrivals.latest() }
    }

    @Test
    fun `stop details formats children with letter and towards`() = runBlocking<Unit> {
        coEvery { api.stopDetails("490G00006336") } returns ApiStopPoint(
            commonName = "East Dulwich Sainsbury's",
            naptanId = "490G00006336",
            stopType = "NaptanOnstreetBusCoachStopPair",
            children = listOf(
                busStop("490006336N", "East Dulwich Sainsbury's", letter = "N", towards = "North Dulwich"),
                busStop("490000453N", "East Dulwich Sainsbury's", letter = "->E", indicator = "->E"),
                ApiStopPoint(
                    commonName = "East Dulwich Sainsbury's",
                    naptanId = "930GEDW",
                    stopType = "NaptanRailStation",
                    children = emptyList()
                )
            )
        )

        val details = arrivals.stopDetails("490G00006336")

        details.name shouldBe "East Dulwich Sainsbury's"
        details.children shouldHaveSize 2
        details.children[0].name shouldBe "East Dulwich Sainsbury's (N) towards North Dulwich"
        details.children[0].isHub shouldBe false
        // Indicator kept so directional twins with no letter stay distinguishable
        details.children[1].name shouldBe "East Dulwich Sainsbury's (->E)"
    }

    @Test
    fun `stop details uses indicator letter when stopLetter missing`() = runBlocking<Unit> {
        coEvery { api.stopDetails("490G1") } returns ApiStopPoint(
            commonName = "Test Stop",
            naptanId = "490G1",
            stopType = "NaptanOnstreetBusCoachStopPair",
            children = listOf(busStop("4901N", "Test Stop", letter = null, indicator = "Stop T"))
        )

        arrivals.stopDetails("490G1").children[0].name shouldBe "Test Stop (T)"
    }

    @Test
    fun `stop details finds bus stops nested below the group`() = runBlocking<Unit> {
        coEvery { api.stopDetails("HUBZCW") } returns ApiStopPoint(
            commonName = "Canada Water",
            naptanId = "HUBZCW",
            stopType = "TransportInterchange",
            children = listOf(
                ApiStopPoint(
                    commonName = "Canada Water Bus Station",
                    naptanId = "490G000438",
                    stopType = "NaptanBusCoachStation",
                    children = listOf(
                        busStop("490000037A", "Canada Water Bus Station", letter = "A"),
                        busStop("490004733B", "Canada Water Bus Station", letter = "B1", towards = "Greenwich")
                    )
                ),
                ApiStopPoint(
                    commonName = "Canada Water Underground Station",
                    naptanId = "940GZZLUCWR",
                    stopType = "NaptanMetroStation",
                    children = emptyList()
                )
            )
        )

        val details = arrivals.stopDetails("HUBZCW")

        details.children.map { it.id } shouldBe listOf("490000037A", "490004733B")
        details.children[0].name shouldBe "Canada Water Bus Station (A)"
        details.children[1].name shouldBe "Canada Water Bus Station (B1) towards Greenwich"
    }

    @Test
    fun `stop details lists only the requested group when TfL returns its interchange`() = runBlocking<Unit> {
        coEvery { api.stopDetails("490G00019922") } returns ApiStopPoint(
            commonName = "Kensington (Olympia)",
            naptanId = "HUBKPA",
            stopType = "TransportInterchange",
            children = listOf(
                stopGroup("490G00007709", "Kensington Olympia", busStop("490007709S", "Kensington Olympia", letter = "P")),
                stopGroup(
                    "490G00019922",
                    "Kensington Olympia / Hammersmith Road",
                    busStop("490007709H", "Kensington Olympia / Hammersmith Road", letter = "V")
                )
            )
        )

        val details = arrivals.stopDetails("490G00019922")

        details.id shouldBe "490G00019922"
        details.name shouldBe "Kensington Olympia / Hammersmith Road"
        details.children.map { it.id } shouldBe listOf("490007709H")
    }

    @Test
    fun `stop details excludes stops without bus services`() = runBlocking<Unit> {
        coEvery { api.stopDetails("910GDARTFD") } returns ApiStopPoint(
            commonName = "Dartford Rail Station",
            naptanId = "910GDARTFD",
            stopType = "NaptanRailStation",
            children = listOf(
                ApiStopPoint(
                    commonName = "Home Gardens",
                    naptanId = "240GHMGRDN",
                    stopType = "NaptanOnstreetBusCoachStopCluster",
                    children = listOf(
                        busStop("2400109092", "Dartford Station", letter = "W", towards = "Crayford"),
                        busStop("240096663", "Home Gardens", letter = "X", modes = emptyList()),
                        busStop("9400ZZCRTEST", "Tram Stop", letter = null, modes = listOf("tram"))
                    )
                )
            )
        )

        val details = arrivals.stopDetails("910GDARTFD")

        details.children.map { it.id } shouldBe listOf("2400109092")
    }

    @Test
    fun `stop details only treats posted letters and numbers as stop letters`() = runBlocking<Unit> {
        coEvery { api.stopDetails("490G1") } returns ApiStopPoint(
            commonName = "Test Stop",
            naptanId = "490G1",
            stopType = "NaptanOnstreetBusCoachStopCluster",
            children = listOf(
                busStop("1", "Heathrow Terminal 5", letter = "9", indicator = "Stand 9", towards = "Central"),
                busStop("2", "Marks Road", letter = null, indicator = "opp", towards = "Whyteleafe"),
                // TfL derives stopLetter from the indicator even when it's a house number
                busStop("3", "Common Road", letter = "33", indicator = "o/s 33", towards = "Redhill"),
                // Qualifier still shown when there's no towards to tell the stops apart
                busStop("4", "Marks Road", letter = null, indicator = "adj"),
                busStop("5", "Blank Road", letter = null, indicator = " ")
            )
        )

        arrivals.stopDetails("490G1").children.map { it.name } shouldBe listOf(
            "Heathrow Terminal 5 (9) towards Central",
            "Marks Road towards Whyteleafe",
            "Common Road towards Redhill",
            "Marks Road (adj)",
            "Blank Road"
        )
    }

    private fun stopGroup(id: String, name: String, vararg stops: ApiStopPoint) = ApiStopPoint(
        commonName = name,
        naptanId = id,
        stopType = "NaptanOnstreetBusCoachStopPair",
        children = stops.toList()
    )

    private fun busStop(
        id: String,
        name: String,
        letter: String?,
        towards: String? = null,
        modes: List<String> = listOf("bus"),
        indicator: String? = letter?.let { "Stop $it" }
    ) = ApiStopPoint(
        commonName = name,
        naptanId = id,
        stopType = "NaptanPublicBusCoachTram",
        children = emptyList(),
        modes = modes,
        stopLetter = letter,
        indicator = indicator,
        additionalProperties = listOfNotNull(towards?.let { ApiAdditionalProperty("Towards", it) })
    )

    private fun busArrival(
        id: String,
        line: String,
        destination: String,
        seconds: Int,
        lineId: String = line.lowercase(),
        platform: String = "D",
        direction: String? = null
    ) = ApiArrival(
        id = id,
        stationName = "East Dulwich Station",
        platformName = platform,
        direction = direction,
        destinationName = destination,
        timeToStation = seconds,
        lineId = lineId,
        lineName = line
    )
}
