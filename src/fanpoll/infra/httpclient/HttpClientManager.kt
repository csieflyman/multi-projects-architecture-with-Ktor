/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.httpclient

import fanpoll.infra.RemoteServiceException
import fanpoll.infra.ResponseCode
import fanpoll.infra.utils.json
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.features.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.content.TextContent
import io.ktor.http.formUrlEncode
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.net.http.HttpConnectTimeoutException

// https://ktor.io/docs/http-client-engines.html#jvm
@OptIn(KtorExperimentalAPI::class)
object HttpClientManager {

    private val logger = KotlinLogging.logger {}

    private val services: MutableList<HttpClientService> = mutableListOf()

    fun register(service: HttpClientService) {
        logger.info("========== init HttpClientManager... ==========")
        require(services.none { it.id == service.id })

        logger.info("create ${service.id} HttpClient...")
        service.client = createClient(service)
        logger.info("${service.id} HttpClient created")

        services.add(service)
        logger.info("========== init HttpClientManager completed ==========")
    }

    private fun createClient(service: HttpClientService): HttpClient {
        val config = service.clientConfig

        return HttpClient(CIO) {

            engine {
                threadsCount = config.threadsCount
                maxConnectionsCount = config.maxConnectionsCount
                requestTimeout = config.requestTimeout

                endpoint {
                    connectTimeout = config.connectTimeout

                    if (config.connectRetryAttempts != null)
                        connectAttempts = config.connectRetryAttempts
                    if (config.keepAliveTime != null)
                        keepAliveTime = config.keepAliveTime
                }
            }

            install(HttpTimeout) {
                connectTimeoutMillis = config.connectTimeout
                requestTimeoutMillis = config.requestTimeout
            }

            Charsets {
                register(Charsets.UTF_8)
            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = config.logLevel
            }

            install(JsonFeature) {
                serializer = KotlinxSerializer(json)
            }

            service.defaultRequest?.also { defaultRequest(it) }

            expectSuccess = false
            HttpResponseValidator {
                handleResponseException { cause ->
                    val responseCode = when (cause) {
                        is HttpConnectTimeoutException -> ResponseCode.REMOTE_SERVICE_CONNECT_TIMEOUT_ERROR
                        is HttpRequestTimeoutException -> ResponseCode.REMOTE_SERVICE_REQUEST_TIMEOUT_ERROR
                        //is HttpSocketTimeoutException -> ResponseCode.REMOTE_SERVICE_SOCKET_TIMEOUT_ERROR
                        else -> ResponseCode.REMOTE_SERVICE_CONNECT_ERROR
                    }
                    throw RemoteServiceException(
                        serviceId = service.id, responseCode = responseCode,
                        message = "${service.id} HttpClient Response Error => ${cause.message}",
                        cause = cause
                    )
                }
            }
        }
    }

    fun shutdown() {
        logger.info("shutdown HttpClientManager...")

        services.forEach { service ->
            logger.info("close ${service.id} HttpClient...")
            service.client.close()
            logger.info("${service.id} HttpClient closed")
        }

        logger.info("shutdown HttpClientManager completed")
    }
}

val HttpRequest.api: String
    get() = "${method.value} - $url"

val HttpRequest.textBody: String?
    get() = when (content) {
        is TextContent -> (content as TextContent).text
        is FormDataContent -> (content as FormDataContent).formData.formUrlEncode()
        else -> null
    }

val HttpResponse.textBody: String
    get() = runBlocking {
        readText()
    }