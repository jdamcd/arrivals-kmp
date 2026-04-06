package com.jdamcd.arrivals

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class UtilsTest {

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

    @Test
    fun `stripPlatform strips Platform prefix`() {
        stripPlatform("Platform 2") shouldBe "2"
    }

    @Test
    fun `stripPlatform allows letter suffix`() {
        stripPlatform("Platform 2A") shouldBe "2A"
    }

    @Test
    fun `stripPlatform trims whitespace`() {
        stripPlatform(" 3 ") shouldBe "3"
    }

    @Test
    fun `stripPlatform returns plain number unchanged`() {
        stripPlatform("5") shouldBe "5"
    }

    @Test
    fun `matchesPlatformFilter exact match`() {
        matchesPlatformFilter("2", "2") shouldBe true
    }

    @Test
    fun `matchesPlatformFilter strips Platform prefix`() {
        matchesPlatformFilter("Platform 2", "2") shouldBe true
    }

    @Test
    fun `matchesPlatformFilter allows letter suffix`() {
        matchesPlatformFilter("2A", "2") shouldBe true
        matchesPlatformFilter("2B", "2") shouldBe true
    }

    @Test
    fun `matchesPlatformFilter matches letter suffix`() {
        matchesPlatformFilter("2", "2A") shouldBe false
    }

    @Test
    fun `matchesPlatformFilter rejects digit suffix`() {
        matchesPlatformFilter("21", "2") shouldBe false
        matchesPlatformFilter("12", "1") shouldBe false
    }

    @Test
    fun `matchesPlatformFilter rejects non-matching platform`() {
        matchesPlatformFilter("3", "2") shouldBe false
    }

    @Test
    fun `matchesPlatformFilter exact match for letter-only platform`() {
        matchesPlatformFilter("A", "A") shouldBe true
        matchesPlatformFilter("Platform D", "D") shouldBe true
    }

    @Test
    fun `matchesPlatformFilter rejects letter suffix for letter-only filter`() {
        matchesPlatformFilter("AB", "A") shouldBe false
        matchesPlatformFilter("Platform AB", "A") shouldBe false
    }

    @Test
    fun `matchesPlatformFilter rejects digit suffix for letter-only filter`() {
        matchesPlatformFilter("A1", "A") shouldBe false
    }

    @Test
    fun `matchesPlatformFilter strips filter input`() {
        matchesPlatformFilter("2", "Platform 2") shouldBe true
    }

    @Test
    fun `formatTime scheduled appends asterisk`() {
        formatTime(300, realtime = false) shouldBe "5 min*"
    }

    @Test
    fun `formatTime scheduled and Due appends asterisk`() {
        formatTime(30, realtime = false) shouldBe "Due*"
    }

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
    fun `filterLedChars normalises eszett`() {
        filterLedChars("Warschauer Stra\u00dfe") shouldBe "Warschauer Strasse"
    }

    @Test
    fun `filterLedChars normalises umlauts`() {
        filterLedChars("M\u00f6ckernbr\u00fccke") shouldBe "Moeckernbruecke"
        filterLedChars("Sch\u00f6nhauser Allee") shouldBe "Schoenhauser Allee"
        filterLedChars("M\u00e4rkisches Museum") shouldBe "Maerkisches Museum"
        filterLedChars("\u00c4\u00d6\u00dc") shouldBe "AeOeUe"
    }

    @Test
    fun `filterLedChars handles empty string`() {
        filterLedChars("") shouldBe ""
    }
}
