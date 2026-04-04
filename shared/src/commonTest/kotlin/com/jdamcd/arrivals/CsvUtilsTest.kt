package com.jdamcd.arrivals.gtfs.csv
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CsvUtilsTest {

    @Test
    fun `parses simple CSV`() {
        val csv = "name,id\nAlice,1\nBob,2"
        val (header, rows) = parseCsvRows(csv)

        header shouldBe listOf("name", "id")
        rows shouldHaveSize 2
        rows[0] shouldBe listOf("Alice", "1")
        rows[1] shouldBe listOf("Bob", "2")
    }

    @Test
    fun `returns empty for empty input`() {
        val (header, rows) = parseCsvRows("")
        header.shouldBeEmpty()
        rows.shouldBeEmpty()
    }

    @Test
    fun `returns empty rows for header-only input`() {
        val (header, rows) = parseCsvRows("name,id")
        header shouldBe listOf("name", "id")
        rows.shouldBeEmpty()
    }

    @Test
    fun `handles quoted fields with commas`() {
        val csv = "name,description\nAlice,\"Hello, world\""
        val (_, rows) = parseCsvRows(csv)

        rows shouldHaveSize 1
        rows[0] shouldBe listOf("Alice", "Hello, world")
    }

    @Test
    fun `strips all quote characters from fields`() {
        val csv = "name,value\nTest,\"say \"\"hello\"\"\""
        val (_, rows) = parseCsvRows(csv)

        rows shouldHaveSize 1
        rows[0][1] shouldBe "say hello"
    }

    @Test
    fun `strips carriage returns`() {
        val csv = "name,id\r\nAlice,1\r\nBob,2\r\n"
        val (header, rows) = parseCsvRows(csv)

        header shouldBe listOf("name", "id")
        rows shouldHaveSize 2
        rows[0] shouldBe listOf("Alice", "1")
    }

    @Test
    fun `skips blank lines`() {
        val csv = "name,id\n\nAlice,1\n\nBob,2\n"
        val (_, rows) = parseCsvRows(csv)

        rows shouldHaveSize 2
    }

    @Test
    fun `handles trailing comma as empty field`() {
        val csv = "a,b,c\n1,2,"
        val (_, rows) = parseCsvRows(csv)

        rows[0] shouldBe listOf("1", "2", "")
    }
}
