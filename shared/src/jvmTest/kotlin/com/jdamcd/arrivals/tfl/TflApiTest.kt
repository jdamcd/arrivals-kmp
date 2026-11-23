package com.jdamcd.arrivals.tfl

import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.jsonResponse
import com.jdamcd.arrivals.mockClient
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TflApiTest {

    @Test
    fun `fetchArrivals deserializes response`() = runBlocking<Unit> {
        val api = TflApi(
            mockClient {
                jsonResponse(
                    """
                    [
                        {
                            "id": 1,
                            "stationName": "Shoreditch High Street",
                            "platformName": "Platform 2",
                            "direction": "outbound",
                            "destinationName": "New Cross",
                            "timeToStation": 120
                        }
                    ]
                    """
                )
            }
        )

        val arrivals = api.fetchArrivals("910GSHRDHST")

        arrivals shouldHaveSize 1
        arrivals[0].stationName shouldBe "Shoreditch High Street"
        arrivals[0].platformName shouldBe "Platform 2"
        arrivals[0].timeToStation shouldBe 120
    }

    @Test
    fun `fetchArrivals returns empty list for empty body`() = runBlocking<Unit> {
        val api = TflApi(
            mockClient {
                jsonResponse("[]")
            }
        )

        api.fetchArrivals("123") shouldHaveSize 0
    }

    @Test
    fun `throws NoDataException with auth message on 401`() = runBlocking<Unit> {
        val api = TflApi(
            mockClient {
                jsonResponse("", HttpStatusCode.Unauthorized)
            }
        )

        val e = assertFailsWith<NoDataException> {
            api.fetchArrivals("123")
        }
        e.message shouldBe "TfL API error"
    }

    @Test
    fun `throws NoDataException with auth message on 403`() = runBlocking<Unit> {
        val api = TflApi(
            mockClient {
                jsonResponse("", HttpStatusCode.Forbidden)
            }
        )

        val e = assertFailsWith<NoDataException> {
            api.fetchArrivals("123")
        }
        e.message shouldBe "TfL API error"
    }

    @Test
    fun `throws NoDataException with connection message on 500`() = runBlocking<Unit> {
        val api = TflApi(
            mockClient {
                jsonResponse("", HttpStatusCode.InternalServerError)
            }
        )

        val e = assertFailsWith<NoDataException> {
            api.fetchArrivals("123")
        }
        e.message shouldBe "Can't connect to TfL API"
    }

    @Test
    fun `searchStations deserializes response`() = runBlocking<Unit> {
        val api = TflApi(
            mockClient {
                jsonResponse(
                    """
                    {
                        "matches": [
                            {"id": "910GSHRDHST", "name": "Shoreditch High Street"}
                        ]
                    }
                    """
                )
            }
        )

        val result = api.searchStations("shoreditch")

        result.matches shouldHaveSize 1
        result.matches[0].id shouldBe "910GSHRDHST"
        result.matches[0].name shouldBe "Shoreditch High Street"
    }

    @Test
    fun `fetchArrivals accepts string and numeric ids`() = runBlocking<Unit> {
        val api = TflApi(
            mockClient {
                jsonResponse(
                    """
                    [
                        {
                            "id": "-1836802865012345678",
                            "stationName": "East Dulwich Station",
                            "platformName": "D",
                            "destinationName": "Victoria",
                            "timeToStation": 300,
                            "lineId": "185",
                            "lineName": "185"
                        },
                        {
                            "id": 123,
                            "stationName": "East Dulwich Station",
                            "platformName": "D",
                            "destinationName": "Victoria",
                            "timeToStation": 400
                        }
                    ]
                    """
                )
            }
        )

        val arrivals = api.fetchArrivals("490001090D")

        arrivals shouldHaveSize 2
        arrivals[0].id shouldBe "-1836802865012345678"
        arrivals[0].lineName shouldBe "185"
        arrivals[1].id shouldBe "123"
    }

    @Test
    fun `searchBusStops requests bus mode and deserializes towards`() = runBlocking<Unit> {
        val api = TflApi(
            mockClient { request ->
                request.url.parameters["modes"] shouldBe "bus"
                jsonResponse(
                    """
                    {
                        "matches": [
                            {"id": "490001090D", "name": "East Dulwich Station", "towards": "Camberwell Or Peckham"},
                            {"id": "490G00006336", "name": "East Dulwich Sainsbury's"}
                        ]
                    }
                    """
                )
            }
        )

        val result = api.searchBusStops("east dulwich")

        result.matches shouldHaveSize 2
        result.matches[0].towards shouldBe "Camberwell Or Peckham"
        result.matches[1].towards shouldBe null
    }

    @Test
    fun `stopDetails deserializes bus stop fields`() = runBlocking<Unit> {
        val api = TflApi(
            mockClient {
                jsonResponse(
                    """
                    {
                        "commonName": "East Dulwich Sainsbury's",
                        "naptanId": "490G00006336",
                        "stopType": "NaptanOnstreetBusCoachStopPair",
                        "children": [
                            {
                                "commonName": "East Dulwich Sainsbury's",
                                "naptanId": "490006336N",
                                "stopType": "NaptanPublicBusCoachTram",
                                "children": [],
                                "stopLetter": "N",
                                "indicator": "Stop N",
                                "additionalProperties": [
                                    {"category": "Direction", "key": "Towards", "value": "North Dulwich"}
                                ]
                            }
                        ]
                    }
                    """
                )
            }
        )

        val stop = api.stopDetails("490G00006336")

        stop.children shouldHaveSize 1
        stop.children[0].stopLetter shouldBe "N"
        stop.children[0].indicator shouldBe "Stop N"
        stop.children[0].additionalProperties[0].key shouldBe "Towards"
        stop.children[0].additionalProperties[0].value shouldBe "North Dulwich"
    }

    @Test
    fun `stopDetails batches ids within the lookup limit`() = runBlocking<Unit> {
        // Chunks are requested concurrently, so arrival order isn't fixed
        val paths = CopyOnWriteArrayList<String>()
        val api = TflApi(
            mockClient { request ->
                paths += request.url.encodedPath
                val ids = request.url.encodedPath.substringAfterLast("/").split(",")
                val stops = ids.map { """{"commonName": "Stop", "naptanId": "$it", "stopType": "NaptanPublicBusCoachTram", "children": [], "modes": ["bus"]}""" }
                // Matches TfL: a bare object for one stop point, an array for several
                jsonResponse(stops.singleOrNull() ?: stops.joinToString(",", "[", "]"))
            }
        )

        val stops = api.stopDetails((1..21).map { "490$it" })

        paths.toSet() shouldBe setOf(
            "/StopPoint/" + (1..20).joinToString(",") { "490$it" },
            "/StopPoint/49021"
        )
        stops shouldHaveSize 21
        stops[20].naptanId shouldBe "49021"
        stops[20].modes shouldBe listOf("bus")
    }

    @Test
    fun `stopDetails accepts a bare object for ids sharing a parent`() = runBlocking<Unit> {
        val api = TflApi(
            mockClient {
                jsonResponse(
                    """
                    {
                        "commonName": "North Cheam / London Road",
                        "naptanId": "490G00010323",
                        "stopType": "NaptanOnstreetBusCoachStopPair",
                        "children": [
                            {"commonName": "North Cheam / Queen Victoria", "naptanId": "490010323C", "stopType": "NaptanPublicBusCoachTram", "children": []},
                            {"commonName": "North Cheam / Queen Victoria", "naptanId": "490010323D", "stopType": "NaptanPublicBusCoachTram", "children": []}
                        ]
                    }
                    """
                )
            }
        )

        val stops = api.stopDetails(listOf("490010323C", "490010323D"))

        stops shouldHaveSize 1
        stops[0].naptanId shouldBe "490G00010323"
        stops[0].children shouldHaveSize 2
    }
}
