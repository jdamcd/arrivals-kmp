package com.jdamcd.arrivals.cli

import com.github.ajalt.clikt.command.test
import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.ArrivalsInfo
import com.jdamcd.arrivals.InMemorySettings
import com.jdamcd.arrivals.Settings
import com.jdamcd.arrivals.SettingsConfig
import com.jdamcd.arrivals.StopResult
import com.jdamcd.arrivals.TflSearch
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class CliTest {

    private val settings: Settings = InMemorySettings()
    private val arrivals = mockk<Arrivals>()
    private val tflSearch = mockk<TflSearch>()

    @BeforeTest
    fun setup() {
        startKoin {
            modules(
                module {
                    single { settings }
                    single<Arrivals> { arrivals }
                    single<TflSearch> { tflSearch }
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
}
