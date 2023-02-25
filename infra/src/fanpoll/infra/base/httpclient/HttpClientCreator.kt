/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.httpclient

import fanpoll.infra.base.json.json
import fanpoll.infra.base.response.InfraResponseCode
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import java.net.http.HttpConnectTimeoutException
import java.time.Instant
import java.util.*

object HttpClientCreator {

    fun create(
        name: String,
        config: CIOHttpClientConfig? = null
    ): HttpClient {

        return HttpClient(CIO) {

            @Suppress("NAME_SHADOWING")
            val config = config ?: CIOHttpClientConfig()

            engine {
                threadsCount = config.threadsCount
                maxConnectionsCount = config.maxConnectionsCount
                requestTimeout = config.requestTimeout

                endpoint {
                    connectTimeout = config.connectTimeout
                    connectAttempts = config.connectAttempts
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

            install(ContentNegotiation) {
                json(json)
            }

            defaultRequest {
                val reqId = UUID.randomUUID().toString()
                attributes.put(HttpClientAttributeKey.REQ_ID, reqId)
                attributes.put(HttpClientAttributeKey.REQ_AT, Instant.now())
                header(HttpHeaders.XRequestId, reqId)
            }

            expectSuccess = false
            HttpResponseValidator {
                handleResponseExceptionWithRequest { cause, request ->
                    val responseCode = when (cause) {
                        is HttpConnectTimeoutException -> InfraResponseCode.REMOTE_SERVICE_CONNECT_TIMEOUT_ERROR
                        is HttpRequestTimeoutException -> InfraResponseCode.REMOTE_SERVICE_REQUEST_TIMEOUT_ERROR
                        //is HttpSocketTimeoutException -> ResponseCode.REMOTE_SERVICE_SOCKET_TIMEOUT_ERROR
                        else -> InfraResponseCode.REMOTE_SERVICE_CONNECT_ERROR
                    }
                    throw HttpClientException(
                        request,
                        responseCode, "HttpClient $name Response Error => ${cause.message}", cause,
                        serviceId = name
                    )
                }
            }
        }
    }
}