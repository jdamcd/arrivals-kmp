package com.jdamcd.arrivals.darwin

import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.Settings
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Clock

@OptIn(kotlin.time.ExperimentalTime::class)
class DarwinArrivalsTest {

    private val api = mockk<DarwinApi>()
    private val settings = Settings()
    private val clock = Clock.System
    private val arrivals = DarwinArrivals(api, settings, clock)

    private val mockBoard = ApiDarwinBoard(
        generatedAt = "2025-11-25T10:00:00Z",
        locationName = "Clapham Junction",
        crs = "CLJ",
        trainServices = listOf(
            ApiTrainService(
                serviceIdUrlSafe = "service1",
                std = "10:15",
                etd = "10:15",
                platform = "5",
                operator = "Southern",
                operatorCode = "SN",
                isCancelled = false,
                destination = listOf(
                    ApiCallingPoint("London Victoria", "VIC")
                )
            ),
            ApiTrainService(
                serviceIdUrlSafe = "service2",
                std = "10:20",
                etd = "On time",
                platform = "7",
                operator = "South Western Railway",
                operatorCode = "SW",
                isCancelled = false,
                destination = listOf(
                    ApiCallingPoint("London Waterloo", "WAT")
                )
            ),
            ApiTrainService(
                serviceIdUrlSafe = "service3",
                std = "10:25",
                etd = "Cancelled",
                platform = "3",
                operator = "Southern",
                operatorCode = "SN",
                isCancelled = true,
                destination = listOf(
                    ApiCallingPoint("Brighton", "BTN")
                )
            ),
            ApiTrainService(
                serviceIdUrlSafe = "service4",
                std = "10:30",
                etd = "Delayed",
                platform = "2",
                operator = "Southern",
                operatorCode = "SN",
                isCancelled = false,
                destination = listOf(
                    ApiCallingPoint("Horsham", "HRH")
                )
            )
        )
    )

    @BeforeTest
    fun setup() {
        settings.darwinCrsCode = "CLJ"
        settings.darwinPlatform = ""
    }

    @Test
    fun `fetches latest departures`() = runBlocking<Unit> {
        coEvery { api.fetchDepartures("CLJ", any()) } returns mockBoard

        val latest = arrivals.latest()

        latest.station shouldBe "Clapham Junction"
        latest.arrivals shouldHaveSize 2
        latest.arrivals[0].destination shouldBe "London Victoria"
    }

    @Test
    fun `filters cancelled trains`() = runBlocking<Unit> {
        coEvery { api.fetchDepartures("CLJ", any()) } returns mockBoard

        val latest = arrivals.latest()

        latest.arrivals.none { it.destination.contains("Brighton") } shouldBe true
    }

    @Test
    fun `filters delayed trains without time`() = runBlocking<Unit> {
        coEvery { api.fetchDepartures("CLJ", any()) } returns mockBoard

        val latest = arrivals.latest()

        latest.arrivals.none { it.destination.contains("Horsham") } shouldBe true
    }

    @Test
    fun `formats station name with platform filter`() = runBlocking<Unit> {
        settings.darwinPlatform = "5"
        coEvery { api.fetchDepartures("CLJ", any()) } returns mockBoard

        val latest = arrivals.latest()

        latest.station shouldBe "Clapham Junction: Platform 5"
    }

    @Test
    fun `applies platform filter`() = runBlocking<Unit> {
        settings.darwinPlatform = "5"
        coEvery { api.fetchDepartures("CLJ", any()) } returns mockBoard

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 1
        latest.arrivals[0].destination shouldBe "London Victoria"
    }

    @Test
    fun `throws NoDataException on empty results`() = runBlocking<Unit> {
        val emptyBoard = mockBoard.copy(trainServices = emptyList())
        coEvery { api.fetchDepartures("CLJ", any()) } returns emptyBoard

        assertFailsWith<NoDataException> {
            arrivals.latest()
        }
    }

    @Test
    fun `throws NoDataException on null train services`() = runBlocking<Unit> {
        val emptyBoard = mockBoard.copy(trainServices = null)
        coEvery { api.fetchDepartures("CLJ", any()) } returns emptyBoard

        assertFailsWith<NoDataException> {
            arrivals.latest()
        }
    }

    @Test
    fun `handles 'On time' ETD correctly`() = runBlocking<Unit> {
        coEvery { api.fetchDepartures("CLJ", any()) } returns mockBoard

        val latest = arrivals.latest()

        latest.arrivals.any { it.destination.contains("Waterloo") } shouldBe true
    }

    @Test
    fun `returns up to 3 arrivals`() = runBlocking<Unit> {
        val manyServices = mockBoard.copy(
            trainServices = List(10) { index ->
                ApiTrainService(
                    serviceIdUrlSafe = "service$index",
                    std = "10:${15 + index}",
                    etd = "10:${15 + index}",
                    platform = "$index",
                    operator = "Test",
                    operatorCode = "TST",
                    isCancelled = false,
                    destination = listOf(ApiCallingPoint("Dest$index", "DST"))
                )
            }
        )
        coEvery { api.fetchDepartures("CLJ", any()) } returns manyServices

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 3
    }

    @Test
    fun `platform filter matches exact number`() = runBlocking<Unit> {
        val board = mockBoard.copy(
            trainServices = listOf(
                ApiTrainService(serviceIdUrlSafe = "s1", std = "10:15", etd = "10:15", platform = "2", operator = "Op", operatorCode = "O", isCancelled = false, destination = listOf(ApiCallingPoint("Dest A", "DA"))),
                ApiTrainService(serviceIdUrlSafe = "s2", std = "10:20", etd = "10:20", platform = "12", operator = "Op", operatorCode = "O", isCancelled = false, destination = listOf(ApiCallingPoint("Dest B", "DB"))),
                ApiTrainService(serviceIdUrlSafe = "s3", std = "10:25", etd = "10:25", platform = "21", operator = "Op", operatorCode = "O", isCancelled = false, destination = listOf(ApiCallingPoint("Dest C", "DC")))
            )
        )
        coEvery { api.fetchDepartures("CLJ", any()) } returns board

        settings.darwinPlatform = "21"
        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 1
        latest.arrivals[0].destination shouldBe "Dest C"
    }

    @Test
    fun `platform filter does not match when filter is prefix of platform number`() = runBlocking<Unit> {
        val board = mockBoard.copy(
            trainServices = listOf(
                ApiTrainService(serviceIdUrlSafe = "s1", std = "10:15", etd = "10:15", platform = "1", operator = "Op", operatorCode = "O", isCancelled = false, destination = listOf(ApiCallingPoint("Dest A", "DA"))),
                ApiTrainService(serviceIdUrlSafe = "s2", std = "10:20", etd = "10:20", platform = "10", operator = "Op", operatorCode = "O", isCancelled = false, destination = listOf(ApiCallingPoint("Dest B", "DB"))),
                ApiTrainService(serviceIdUrlSafe = "s3", std = "10:25", etd = "10:25", platform = "11", operator = "Op", operatorCode = "O", isCancelled = false, destination = listOf(ApiCallingPoint("Dest C", "DC")))
            )
        )
        coEvery { api.fetchDepartures("CLJ", any()) } returns board

        settings.darwinPlatform = "1"
        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 1
        latest.arrivals[0].destination shouldBe "Dest A"
    }

    @Test
    fun `platform filter matches number with letter suffix`() = runBlocking<Unit> {
        val board = mockBoard.copy(
            trainServices = listOf(
                ApiTrainService(serviceIdUrlSafe = "s1", std = "10:15", etd = "10:15", platform = "2", operator = "Op", operatorCode = "O", isCancelled = false, destination = listOf(ApiCallingPoint("Dest A", "DA"))),
                ApiTrainService(serviceIdUrlSafe = "s2", std = "10:20", etd = "10:20", platform = "2A", operator = "Op", operatorCode = "O", isCancelled = false, destination = listOf(ApiCallingPoint("Dest B", "DB"))),
                ApiTrainService(serviceIdUrlSafe = "s3", std = "10:25", etd = "10:25", platform = "2B", operator = "Op", operatorCode = "O", isCancelled = false, destination = listOf(ApiCallingPoint("Dest C", "DC"))),
                ApiTrainService(serviceIdUrlSafe = "s4", std = "10:30", etd = "10:30", platform = "12", operator = "Op", operatorCode = "O", isCancelled = false, destination = listOf(ApiCallingPoint("Dest D", "DD")))
            )
        )
        coEvery { api.fetchDepartures("CLJ", any()) } returns board

        settings.darwinPlatform = "2"
        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 3
        latest.arrivals[0].destination shouldBe "Dest A"
        latest.arrivals[1].destination shouldBe "Dest B"
        latest.arrivals[2].destination shouldBe "Dest C"
    }

    @Test
    fun `excludes departures more than 2 hours away`() = runBlocking<Unit> {
        // generatedAt is 10:00, so 12:01 is more than 2 hours
        val board = mockBoard.copy(
            trainServices = listOf(
                ApiTrainService(serviceIdUrlSafe = "s1", std = "10:30", etd = "10:30", platform = "1", operator = "Op", operatorCode = "O", isCancelled = false, destination = listOf(ApiCallingPoint("Within 2hrs", "W2"))),
                ApiTrainService(serviceIdUrlSafe = "s2", std = "11:59", etd = "11:59", platform = "2", operator = "Op", operatorCode = "O", isCancelled = false, destination = listOf(ApiCallingPoint("Just under 2hrs", "JU"))),
                ApiTrainService(serviceIdUrlSafe = "s3", std = "12:01", etd = "12:01", platform = "3", operator = "Op", operatorCode = "O", isCancelled = false, destination = listOf(ApiCallingPoint("Over 2hrs", "O2")))
            )
        )
        coEvery { api.fetchDepartures("CLJ", any()) } returns board

        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 2
        latest.arrivals[0].destination shouldBe "Within 2hrs"
        latest.arrivals[1].destination shouldBe "Just under 2hrs"
    }
}
