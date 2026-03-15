package com.jdamcd.arrivals

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

internal abstract class JsonApiClient(
    private val client: HttpClient,
    private val apiName: String
) {
    protected suspend fun executeRequest(
        url: String,
        parameters: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        val response = try {
            client.get(url) { parameters() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            throw NoDataException("Can't connect to $apiName")
        }
        if (!response.status.isSuccess()) {
            throw NoDataException(
                if (response.status.value in 400..499) {
                    "$apiName error"
                } else {
                    "Can't connect to $apiName"
                }
            )
        }
        return response
    }
}
