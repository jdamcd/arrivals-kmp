package com.jdamcd.arrivals

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun mockClient(
    handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
): HttpClient = HttpClient(MockEngine) {
    engine {
        addHandler(handler)
    }
    install(ContentNegotiation) {
        json(
            Json {
                isLenient = true
                ignoreUnknownKeys = true
            }
        )
    }
}

fun MockRequestHandleScope.jsonResponse(
    content: String,
    status: HttpStatusCode = HttpStatusCode.OK
) = respond(
    content = content,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, "application/json")
)
