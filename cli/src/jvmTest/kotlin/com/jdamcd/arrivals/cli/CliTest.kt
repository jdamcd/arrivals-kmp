package com.jdamcd.arrivals.cli

import com.github.ajalt.clikt.command.test
import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.ArrivalsInfo
import com.jdamcd.arrivals.InMemorySettings
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.SettingsConfig
import com.jdamcd.arrivals.StopDetails
import com.jdamcd.arrivals.StopResult
import com.jdamcd.arrivals.TflSearch
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class CliTest {

    private val settings: Settings = InMemorySettings()
    private val arrivals = mockk<Arrivals>()
    private val tflSearch = mockk<TflSearch>()
    private val tflBusSearch = mockk<TflSearch>()

    @BeforeTest
    fun setup() {
        startKoin {
            modules(
                module {
                    single { settings }
                    single<Arrivals> { arrivals }
                    single<TflSearch> { tflSearch }
                    single<TflSearch>(named("tflBus")) { tflBusSearch }
                }
            )
        }
    }

    @AfterTest
    fun teardown() = stopKoin()

    @Test
    fun `tfl command maps options to settings and prints arrivals`() = runBlocking<Unit> {
        coEvery { arrivals.latest(any()) } returns ArrivalsInfo("Shoreditch High Street", listOf(Arrival(1, "Liverpool Street", 120)))

        val result = buildCli().test("tfl --station 910GSHRDHST --platform 2")

        result.statusCode shouldBe 0
        result.output shouldContain "Shoreditch High Street"
        result.output shouldContain "Liverpool Street"
        settings.mode shouldBe SettingsConfig.MODE_TFL
        settings.stopId shouldBe "910GSHRDHST"
        settings.platform shouldBe "2"
    }

    @Test
    fun `json flag outputs serialized arrivals`() = runBlocking<Unit> {
        coEvery { arrivals.latest(any()) } returns ArrivalsInfo("Clapham Junction", listOf(Arrival(1, "Waterloo", 60)))

        val result = buildCli().test("--json tfl --station CLJ")

        result.statusCode shouldBe 0
        result.output shouldContain "\"station\":\"Clapham Junction\""
        result.output shouldContain "\"displayName\":\"Waterloo\""
    }

    @Test
    fun `fetch failure exits non-zero and prints message`() = runBlocking<Unit> {
        coEvery { arrivals.latest(any()) } throws RuntimeException("upstream down")

        val result = buildCli().test("tfl --station CLJ")

        result.statusCode shouldBe 1
        result.output shouldContain "upstream down"
    }

    @Test
    fun `json fetch failure outputs error response`() = runBlocking<Unit> {
        coEvery { arrivals.latest(any()) } throws RuntimeException("upstream down")

        val result = buildCli().test("--json tfl --station CLJ")

        result.statusCode shouldBe 1
        result.output shouldContain "\"error\":\"upstream down\""
    }

    @Test
    fun `search tfl formats results with ids`() = runBlocking<Unit> {
        coEvery { tflSearch.searchStops(any()) } returns listOf(StopResult("910GSHRDHST", "Shoreditch High Street", isHub = false))

        val result = buildCli().test("search tfl shoreditch")

        result.output shouldContain "Shoreditch High Street (910GSHRDHST)"
    }

    @Test
    fun `search tfl reports no results`() = runBlocking<Unit> {
        coEvery { tflSearch.searchStops(any()) } returns emptyList()

        val result = buildCli().test("search tfl nowhere")

        result.output shouldContain "No results found"
    }

    @Test
    fun `tfl-bus command maps options to settings and prints arrivals`() = runBlocking<Unit> {
        coEvery { arrivals.latest(any()) } returns ArrivalsInfo(
            "East Dulwich Station (D)",
            listOf(Arrival(1, "Victoria", 300, line = "185"))
        )

        val result = buildCli().test("tfl-bus --station 490001090D --line 185")

        result.statusCode shouldBe 0
        result.output shouldContain "East Dulwich Station (D)"
        result.output shouldContain "185 - Victoria"
        settings.mode shouldBe SettingsConfig.MODE_TFL_BUS
        settings.stopId shouldBe "490001090D"
        settings.line shouldBe "185"
    }

    @Test
    fun `search tfl-bus formats results and expands stop groups`() = runBlocking<Unit> {
        coEvery { tflBusSearch.searchStops(any()) } returns listOf(
            StopResult("490001090D", "East Dulwich Station towards Camberwell Or Peckham", isHub = false),
            StopResult("490G00006336", "East Dulwich Sainsbury's", isHub = true)
        )
        coEvery { tflBusSearch.stopDetails("490G00006336") } returns StopDetails(
            "490G00006336",
            "East Dulwich Sainsbury's",
            listOf(StopResult("490006336N", "East Dulwich Sainsbury's (N) towards North Dulwich", isHub = false))
        )

        val result = buildCli().test("search tfl-bus dulwich")

        result.output shouldContain "East Dulwich Station towards Camberwell Or Peckham (490001090D)"
        result.output shouldContain "East Dulwich Sainsbury's:"
        result.output shouldContain "  East Dulwich Sainsbury's (N) towards North Dulwich (490006336N)"
    }

    @Test
    fun `search tfl-bus heads stop groups with their search names`() = runBlocking<Unit> {
        coEvery { tflBusSearch.searchStops(any()) } returns listOf(
            StopResult("490G1", "Victoria Road towards Ilford", isHub = true),
            StopResult("490G2", "Victoria Road towards Acton", isHub = true)
        )
        coEvery { tflBusSearch.stopDetails("490G1") } returns
            StopDetails("490G1", "Victoria Road", listOf(StopResult("1", "Victoria Road (A) towards Ilford", isHub = false)))
        coEvery { tflBusSearch.stopDetails("490G2") } returns
            StopDetails("490G2", "Victoria Road", listOf(StopResult("2", "Victoria Road (B) towards Acton", isHub = false)))

        val result = buildCli().test("search tfl-bus victoria")

        result.output.lines().filter { it.isNotBlank() } shouldBe listOf(
            "Victoria Road towards Ilford:",
            "  Victoria Road (A) towards Ilford (1)",
            "Victoria Road towards Acton:",
            "  Victoria Road (B) towards Acton (2)"
        )
    }

    @Test
    fun `search tfl-bus keeps result order and earlier rows when a group lookup fails`() = runBlocking<Unit> {
        coEvery { tflBusSearch.searchStops(any()) } returns listOf(
            StopResult("490G1", "Slow Group", isHub = true),
            StopResult("490G2", "Fast Group", isHub = true),
            StopResult("490G3", "Broken Group", isHub = true),
            StopResult("4901", "Never Printed", isHub = false)
        )
        // Lookups run concurrently, so the first finishes last
        coEvery { tflBusSearch.stopDetails("490G1") } coAnswers {
            delay(100)
            StopDetails("490G1", "Slow Group", listOf(StopResult("1", "Slow Stop", isHub = false)))
        }
        coEvery { tflBusSearch.stopDetails("490G2") } returns
            StopDetails("490G2", "Fast Group", listOf(StopResult("2", "Fast Stop", isHub = false)))
        coEvery { tflBusSearch.stopDetails("490G3") } throws RuntimeException("upstream down")

        val result = buildCli().test("search tfl-bus anything")

        result.statusCode shouldBe 1
        result.output.lines().filter { it.isNotBlank() } shouldBe listOf(
            "Slow Group:",
            "  Slow Stop (1)",
            "Fast Group:",
            "  Fast Stop (2)",
            "upstream down"
        )
    }
}
