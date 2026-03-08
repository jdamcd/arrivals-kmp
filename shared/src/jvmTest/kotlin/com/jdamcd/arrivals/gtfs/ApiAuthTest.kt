package com.jdamcd.arrivals.gtfs

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ApiAuthTest {

    @Test
    fun `returns null when key is empty`() {
        ApiAuth.parse("") shouldBe null
        ApiAuth.parse("", "app_id") shouldBe null
    }

    @Test
    fun `defaults to api_key query param`() {
        val auth = ApiAuth.parse("key")
        auth.shouldBeInstanceOf<ApiAuth.QueryParam>()
        auth.name shouldBe "api_key"
        auth.key shouldBe "key"
    }

    @Test
    fun `defaults to api_key query param when param is empty`() {
        val auth = ApiAuth.parse("key", "")
        auth.shouldBeInstanceOf<ApiAuth.QueryParam>()
        auth.name shouldBe "api_key"
        auth.key shouldBe "key"
    }

    @Test
    fun `parses bare param name as query param`() {
        val auth = ApiAuth.parse("key", "app_id")
        auth.shouldBeInstanceOf<ApiAuth.QueryParam>()
        auth.name shouldBe "app_id"
        auth.key shouldBe "key"
    }

    @Test
    fun `parses query prefix as query param`() {
        val auth = ApiAuth.parse("key", "query:token")
        auth.shouldBeInstanceOf<ApiAuth.QueryParam>()
        auth.name shouldBe "token"
        auth.key shouldBe "key"
    }

    @Test
    fun `parses header prefix as header auth`() {
        val auth = ApiAuth.parse("key", "header:Authorization")
        auth.shouldBeInstanceOf<ApiAuth.Header>()
        auth.name shouldBe "Authorization"
        auth.key shouldBe "key"
    }
}
