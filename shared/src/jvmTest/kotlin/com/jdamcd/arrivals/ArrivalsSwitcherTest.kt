package com.jdamcd.arrivals

import com.jdamcd.arrivals.darwin.DarwinArrivals
import com.jdamcd.arrivals.gtfs.GtfsArrivals
import com.jdamcd.arrivals.tfl.TflArrivals
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class ArrivalsSwitcherTest {

    private val tfl = mockk<TflArrivals>()
    private val gtfs = mockk<GtfsArrivals>()
    private val darwin = mockk<DarwinArrivals>()
    private val settings = Settings()
    private val switcher = ArrivalsSwitcher(tfl, gtfs, darwin, settings)

    private val tflResult = ArrivalsInfo("TfL Station", emptyList())
    private val gtfsResult = ArrivalsInfo("GTFS Station", emptyList())
    private val darwinResult = ArrivalsInfo("Darwin Station", emptyList())

    @Test
    fun `routes to TfL`() = runBlocking<Unit> {
        settings.mode = SettingsConfig.MODE_TFL
        coEvery { tfl.latest() } returns tflResult

        switcher.latest().station shouldBe "TfL Station"
    }

    @Test
    fun `routes to Darwin`() = runBlocking<Unit> {
        settings.mode = SettingsConfig.MODE_DARWIN
        coEvery { darwin.latest() } returns darwinResult

        switcher.latest().station shouldBe "Darwin Station"
    }

    @Test
    fun `routes to GTFS`() = runBlocking<Unit> {
        settings.mode = SettingsConfig.MODE_GTFS
        coEvery { gtfs.latest() } returns gtfsResult

        switcher.latest().station shouldBe "GTFS Station"
    }

    @Test
    fun `routes to GTFS for unknown mode`() = runBlocking<Unit> {
        settings.mode = "unknown"
        coEvery { gtfs.latest() } returns gtfsResult

        switcher.latest().station shouldBe "GTFS Station"
    }
}
