/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.util

import fanpoll.infra.base.json.json
import fanpoll.infra.base.response.*
import fanpoll.infra.main
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.ktor.application.Application
import io.ktor.server.engine.ApplicationEngineEnvironment
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.createTestEnvironment
import kotlinx.serialization.json.*

object SingleKtorTestApplicationEngine {

    val instance: TestApplicationEngine by lazy {
        TestApplicationEngine(createTestEnvironment()) {}.apply {
            start()
            application.main {
                listOf(SinglePostgreSQLContainer, SingleRedisContainer).forEach {
                    it.configure(this)
                }
            }
        }
    }
}

fun TestApplicationResponse.jsonObject(): JsonObject = content?.let { json.parseToJsonElement(it).jsonObject }
    ?: error("response content is empty or not a json object")

fun TestApplicationResponse.code(): ResponseCode = jsonObject()["code"]?.let { json.decodeFromJsonElement(it) }
    ?: InfraResponseCode.OK

fun TestApplicationResponse.message(): String? = jsonObject()["message"]?.jsonPrimitive?.content

fun TestApplicationResponse.dataJson(): JsonElement? = jsonObject()["data"]

fun TestApplicationResponse.dataJsonObject(): JsonObject =
    dataJson()?.jsonObject ?: error("response data is neither exist or a json object")

fun TestApplicationResponse.dataJsonArray(): JsonArray =
    dataJson()?.jsonArray ?: error("response data is neither exist or a json array")

inline fun <reified T> TestApplicationResponse.data(): T = dataJsonObject().let { json.decodeFromJsonElement(it) }

inline fun <reified T> TestApplicationResponse.dataList(): List<T> = dataJsonArray().map { json.decodeFromJsonElement(it) }

fun TestApplicationResponse.response(): ResponseDTO = json.decodeFromJsonElement(jsonObject())

fun TestApplicationResponse.dataResponse(): DataResponseDTO = json.decodeFromJsonElement(jsonObject())

fun TestApplicationResponse.pagingDataResponse(): PagingDataResponseDTO = json.decodeFromJsonElement(jsonObject())

fun TestApplicationResponse.errorResponse(): ErrorResponseDTO = json.decodeFromJsonElement(jsonObject())

suspend fun <R> FunSpecContainerScope.withTestApplicationInKotestContext(
    moduleFunction: Application.() -> Unit,
    test: suspend TestApplicationEngine.(FunSpecContainerScope) -> R
): R {
    val context = this
    return withTestApplicationInKotestContext(createTestEnvironment()) {
        moduleFunction(application)
        test(context)
    }
}

suspend fun <R> FunSpecContainerScope.withTestApplicationInKotestContext(
    environment: ApplicationEngineEnvironment = createTestEnvironment(),
    configure: TestApplicationEngine.Configuration.() -> Unit = {},
    test: suspend TestApplicationEngine.(FunSpecContainerScope) -> R
): R {
    val context = this
    val engine = TestApplicationEngine(environment, configure)
    engine.start()
    try {
        return engine.test(context)
    } finally {
        engine.stop(0L, 0L)
    }
}
