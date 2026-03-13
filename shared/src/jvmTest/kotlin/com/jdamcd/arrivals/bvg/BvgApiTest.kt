package com.jdamcd.arrivals.bvg

import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.jsonResponse
import com.jdamcd.arrivals.mockClient
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BvgApiTest {

    @Test
    fun `searchStops deserializes response`() = runBlocking<Unit> {
        val api = BvgApi(
            mockClient {
                jsonResponse(
                    """
                    [
                        {
                            "type": "stop",
                            "id": "900100003",
                            "name": "S+U Alexanderplatz Bhf (Berlin)",
                            "products": {
                                "suburban": true,
                                "subway": true,
                                "tram": true,
                                "bus": true,
                                "ferry": false,
                                "express": false,
                                "regional": true
                            }
                        },
                        {
                            "type": "stop",
                            "id": "900100024",
                            "name": "S+U Alexanderplatz Bhf/Dircksenstr. (Berlin)",
                            "products": {
                                "suburban": false,
                                "subway": false,
                                "tram": true,
                                "bus": true,
                                "ferry": false,
                                "express": false,
                                "regional": false
                            }
                        }
                    ]
                    """
                )
            }
        )

        val results = api.searchStops("Alexanderplatz")

        results shouldHaveSize 2
        results[0].id shouldBe "900100003"
        results[0].name shouldBe "S+U Alexanderplatz Bhf (Berlin)"
        results[0].type shouldBe "stop"
        results[0].products shouldNotBe null
        results[0].products!!.subway shouldBe true
    }

    @Test
    fun `fetchDepartures deserializes response`() = runBlocking<Unit> {
        val api = BvgApi(
            mockClient {
                jsonResponse(
                    """
                    {
                        "departures": [
                            {
                                "tripId": "1|2008|6|86|9032026",
                                "direction": "S Westkreuz (Berlin)",
                                "line": {
                                    "name": "S5",
                                    "product": "suburban"
                                },
                                "when": "2026-03-09T12:48:00+01:00",
                                "plannedWhen": "2026-03-09T12:48:00+01:00",
                                "delay": 0,
                                "platform": "4",
                                "plannedPlatform": "4"
                            },
                            {
                                "tripId": "1|82407|0|86|9032026",
                                "direction": "Pankow",
                                "line": {
                                    "name": "U2",
                                    "product": "subway"
                                },
                                "when": null,
                                "plannedWhen": "2026-03-09T12:50:00+01:00",
                                "delay": null,
                                "platform": "2 (U2)",
                                "plannedPlatform": "2 (U2)"
                            }
                        ],
                        "realtimeDataUpdatedAt": 1773056917
                    }
                    """
                )
            }
        )

        val response = api.fetchDepartures("900100003")

        response.departures shouldHaveSize 2
        response.departures[0].direction shouldBe "S Westkreuz (Berlin)"
        response.departures[0].line?.name shouldBe "S5"
        response.departures[0].line?.product shouldBe "suburban"
        response.departures[0].departureTime shouldBe "2026-03-09T12:48:00+01:00"
        response.departures[0].platform shouldBe "4"
        response.departures[1].departureTime shouldBe null
    }

    @Test
    fun `fetchStop deserializes response`() = runBlocking<Unit> {
        val api = BvgApi(
            mockClient {
                jsonResponse(
                    """
                    {
                        "type": "stop",
                        "id": "900100003",
                        "name": "S+U Alexanderplatz Bhf (Berlin)",
                        "products": {
                            "suburban": true,
                            "subway": true,
                            "tram": true,
                            "bus": true,
                            "ferry": false,
                            "express": false,
                            "regional": true
                        }
                    }
                    """
                )
            }
        )

        val stop = api.fetchStop("900100003")

        stop.id shouldBe "900100003"
        stop.name shouldBe "S+U Alexanderplatz Bhf (Berlin)"
    }

    @Test
    fun `throws NoDataException on error`() = runBlocking<Unit> {
        val api = BvgApi(
            mockClient {
                jsonResponse("", HttpStatusCode.InternalServerError)
            }
        )

        val e = assertFailsWith<NoDataException> {
            api.fetchDepartures("900100003")
        }
        e.message shouldBe "Can't connect to BVG API"
    }
}
