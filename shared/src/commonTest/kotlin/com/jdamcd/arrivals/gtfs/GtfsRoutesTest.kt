package com.jdamcd.arrivals.gtfs

import com.jdamcd.arrivals.Fixtures
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GtfsRoutesTest {

    @Test
    fun `parses route color and short name`() {
        val routes = GtfsRoutes(Fixtures.ROUTES_CSV)

        routes.styleFor("G") shouldBe RouteStyle(color = "6CBE45", label = "G")
        routes.styleFor("A") shouldBe RouteStyle(color = "2850AD", label = "A", textColor = "FFFFFF")
    }

    @Test
    fun `keeps short name up to 3 characters`() {
        val routes = GtfsRoutes(Fixtures.ROUTES_CSV)

        routes.styleFor("AB") shouldBe RouteStyle(color = "AABB00", label = "AB")
        routes.styleFor("RED") shouldBe RouteStyle(color = "FF0000", label = "Red")
    }

    @Test
    fun `truncates names over 3 characters to first letter`() {
        val routes = GtfsRoutes(Fixtures.ROUTES_CSV)

        routes.styleFor("BLUE") shouldBe RouteStyle(color = "0000FF", label = "B")
        routes.styleFor("XPRESS") shouldBe RouteStyle(color = "00FF00", label = "E")
    }

    @Test
    fun `express override changes label and marks as express`() {
        val overrides = mapOf("MTA NYCT" to mapOf("6X" to "6"))
        val routes = GtfsRoutes(Fixtures.ROUTES_CSV, agencyExpressOverrides = overrides)

        routes.styleFor("6X") shouldBe RouteStyle(color = "00933C", label = "6")
        routes.isExpress("6X") shouldBe true
        routes.isExpress("6") shouldBe false
    }

    @Test
    fun `express override resolves style from base route when not in schedule`() {
        val overrides = mapOf("MTA NYCT" to mapOf("SS" to "SI"))
        val routes = GtfsRoutes(Fixtures.ROUTES_CSV, agencyExpressOverrides = overrides)

        routes.styleFor("SS") shouldBe RouteStyle(color = "08179C", label = "SIR", textColor = "FFFFFF")
        routes.isExpress("SS") shouldBe true
    }

    @Test
    fun `express override not applied for non-matching agency`() {
        val overrides = mapOf("Other Agency" to mapOf("6X" to "6"))
        val routes = GtfsRoutes(Fixtures.ROUTES_CSV, agencyExpressOverrides = overrides)

        routes.styleFor("6X") shouldBe RouteStyle(color = "00933C", label = "6X")
        routes.isExpress("6X") shouldBe false
    }

    @Test
    fun `returns null for route with missing color`() {
        val csv = """
            route_id,route_short_name,route_color
            G,G,
        """.trimIndent()
        val routes = GtfsRoutes(csv)

        routes.styleFor("G") shouldBe null
    }

    @Test
    fun `returns null for unknown route`() {
        val routes = GtfsRoutes(Fixtures.ROUTES_CSV)

        routes.styleFor("Z") shouldBe null
    }

    @Test
    fun `handles empty routes data`() {
        val routes = GtfsRoutes("")

        routes.styleFor("G") shouldBe null
    }
}
