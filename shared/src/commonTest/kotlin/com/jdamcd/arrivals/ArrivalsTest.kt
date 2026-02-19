package com.jdamcd.arrivals

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ArrivalsTest {

    @Test
    fun `formatTime as Due`() {
        formatTime(0) shouldBe "Due"
    }

    @Test
    fun `formatTime with multiple mins`() {
        formatTime(300) shouldBe "5 min"
    }

    @Test
    fun `formatTime as Due below 1 min`() {
        formatTime(59) shouldBe "Due"
        formatTime(60) shouldBe "1 min"
    }
}
