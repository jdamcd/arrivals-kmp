package com.jdamcd.arrivals.tfl

import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.jsonResponse
import com.jdamcd.arrivals.mockClient
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
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
        e.message shouldBe "TfL API app key error"
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
        e.message shouldBe "TfL API app key error"
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
}
