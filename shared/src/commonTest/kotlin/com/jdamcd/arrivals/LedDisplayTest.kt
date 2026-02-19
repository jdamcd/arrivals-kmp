package com.jdamcd.arrivals

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LedDisplayTest {

    @Test
    fun `filterLedChars keeps supported characters`() {
        filterLedChars("ABCxyz") shouldBe "ABCxyz"
        filterLedChars("123") shouldBe "123"
        filterLedChars("-'&*+:,.") shouldBe "-'&*+:,."
        filterLedChars("Clapham Junction") shouldBe "Clapham Junction"
    }

    @Test
    fun `filterLedChars removes unsupported characters`() {
        filterLedChars("Kensington (Olympia)") shouldBe "Kensington Olympia"
        filterLedChars("Queenstown Road [Battersea]") shouldBe "Queenstown Road Battersea"
    }

    @Test
    fun `filterLedChars normalises curly apostrophes`() {
        filterLedChars("King\u2018s Cross") shouldBe "King's Cross"
        filterLedChars("Shepherd\u2019s Bush") shouldBe "Shepherd's Bush"
    }

    @Test
    fun `filterLedChars normalises dashes`() {
        filterLedChars("Times Square\u201342 St") shouldBe "Times Square-42 St"
        filterLedChars("Times Square\u201442 St") shouldBe "Times Square-42 St"
    }

    @Test
    fun `filterLedChars handles empty string`() {
        filterLedChars("") shouldBe ""
    }
}
