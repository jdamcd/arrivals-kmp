package com.jdamcd.arrivals.darwin

import com.jdamcd.arrivals.NoDataException
import com.jdamcd.arrivals.jsonResponse
import com.jdamcd.arrivals.mockClient
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DarwinApiTest {

    @Test
    fun `fetchDepartures deserializes response`() = runBlocking<Unit> {
        val api = DarwinApi(
            mockClient {
                jsonResponse(
                    """
                    {
                        "generatedAt": "2025-11-25T10:00:00Z",
                        "locationName": "Clapham Junction",
                        "crs": "CLJ",
                        "trainServices": [
                            {
                                "serviceIdUrlSafe": "abc123",
                                "std": "10:15",
                                "etd": "On time",
                                "platform": "5",
                                "operator": "Southern",
                                "operatorCode": "SN",
                                "isCancelled": false,
                                "destination": [
                                    {"locationName": "London Victoria", "crs": "VIC"}
                                ]
                            }
                        ]
                    }
                    """
                )
            }
        )

        val board = api.fetchDepartures("CLJ")

        board.locationName shouldBe "Clapham Junction"
        board.crs shouldBe "CLJ"
        val services = board.trainServices!!
        services shouldHaveSize 1
        services[0].etd shouldBe "On time"
        services[0].destination[0].locationName shouldBe "London Victoria"
    }

    @Test
    fun `fetchDepartures handles null train services`() = runBlocking<Unit> {
        val api = DarwinApi(
            mockClient {
                jsonResponse(
                    """
                    {
                        "generatedAt": "2025-11-25T10:00:00Z",
                        "locationName": "Clapham Junction",
                        "crs": "CLJ"
                    }
                    """
                )
            }
        )

        val board = api.fetchDepartures("CLJ")

        board.trainServices shouldBe null
    }

    @Test
    fun `throws NoDataException with token message on 401`() = runBlocking<Unit> {
        val api = DarwinApi(
            mockClient {
                jsonResponse("", HttpStatusCode.Unauthorized)
            }
        )

        val e = assertFailsWith<NoDataException> {
            api.fetchDepartures("CLJ")
        }
        e.message shouldBe "Darwin API error"
    }

    @Test
    fun `throws NoDataException with token message on 400`() = runBlocking<Unit> {
        val api = DarwinApi(
            mockClient {
                jsonResponse("", HttpStatusCode.BadRequest)
            }
        )

        val e = assertFailsWith<NoDataException> {
            api.fetchDepartures("CLJ")
        }
        e.message shouldBe "Darwin API error"
    }

    @Test
    fun `throws NoDataException with connection message on 500`() = runBlocking<Unit> {
        val api = DarwinApi(
            mockClient {
                jsonResponse("", HttpStatusCode.InternalServerError)
            }
        )

        val e = assertFailsWith<NoDataException> {
            api.fetchDepartures("CLJ")
        }
        e.message shouldBe "Can't connect to Darwin API"
    }

    @Test
    fun `searchCrs deserializes response`() = runBlocking<Unit> {
        val api = DarwinApi(
            mockClient {
                jsonResponse(
                    """
                    [
                        {"crsCode": "CLJ", "stationName": "Clapham Junction"},
                        {"crsCode": "CLP", "stationName": "Clapham"}
                    ]
                    """
                )
            }
        )

        val results = api.searchCrs("clap")

        results shouldHaveSize 2
        results[0].crsCode shouldBe "CLJ"
        results[0].stationName shouldBe "Clapham Junction"
    }
}
