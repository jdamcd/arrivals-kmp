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

class TflArrivalsTest {

    private val api = mockk<TflApi>()
    private val settings = Settings()
    private val arrivals = TflArrivals(api, settings)

    private val response = listOf(
        ApiArrival(123, "Test Stop", "Platform 2", "outbound", "New Cross", 456),
        ApiArrival(124, "Test Stop", "Platform 2", "outbound", "Crystal Palace Rail Station", 10),
        ApiArrival(125, "Test Stop", "Platform 1", "inbound", "Dalston Junction", 10),
        ApiArrival(126, "Test Stop", "Platform 1", "inbound", "Highbury & Islington Underground Station", 456)
    )

    @BeforeTest
    fun setup() {
        settings.tflStopId = "123"
        settings.tflPlatform = "2"
        settings.tflDirection = "all"
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

        settings.tflDirection = "all"
        settings.tflPlatform = "2"
        arrivals.latest().station shouldBe "Test Stop: Platform 2"

        settings.tflDirection = "inbound"
        settings.tflPlatform = ""
        arrivals.latest().station shouldBe "Test Stop: Inbound"

        settings.tflDirection = "all"
        settings.tflPlatform = ""
        arrivals.latest().station shouldBe "Test Stop"
    }

    @Test
    fun `returns up to 3 arrivals`() = runBlocking<Unit> {
        settings.tflPlatform = ""
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

        settings.tflPlatform = "21"
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

        settings.tflPlatform = "1"
        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 1
        latest.arrivals[0].destination shouldBe "Dest A"
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

        settings.tflPlatform = "2"
        val latest = arrivals.latest()

        latest.arrivals shouldHaveSize 3
        latest.arrivals[0].destination shouldBe "Dest A"
        latest.arrivals[1].destination shouldBe "Dest B"
        latest.arrivals[2].destination shouldBe "Dest C"
    }
}
