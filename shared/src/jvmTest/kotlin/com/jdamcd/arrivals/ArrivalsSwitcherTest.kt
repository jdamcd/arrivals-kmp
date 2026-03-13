package com.jdamcd.arrivals

import com.jdamcd.arrivals.bvg.BvgArrivals
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
    private val bvg = mockk<BvgArrivals>()
    private val settings = Settings()
    private val switcher = ArrivalsSwitcher(tfl, gtfs, darwin, bvg, settings)

    private val tflResult = ArrivalsInfo("TfL Station", emptyList())
    private val gtfsResult = ArrivalsInfo("GTFS Station", emptyList())
    private val darwinResult = ArrivalsInfo("Darwin Station", emptyList())
    private val bvgResult = ArrivalsInfo("BVG Station", emptyList())

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
    fun `routes to BVG`() = runBlocking<Unit> {
        settings.mode = SettingsConfig.MODE_BVG
        coEvery { bvg.latest() } returns bvgResult

        switcher.latest().station shouldBe "BVG Station"
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
