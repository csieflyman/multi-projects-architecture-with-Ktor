/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.httpclient

import fanpoll.infra.base.json.json
import fanpoll.infra.base.response.ResponseCode
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.features.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import java.net.http.HttpConnectTimeoutException

object HttpClientCreator {

    fun create(
        name: String,
        config: CIOHttpClientConfig,
        defaultRequest: (HttpRequestBuilder.() -> Unit)? = null
    ): HttpClient {

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

            defaultRequest?.also { defaultRequest(it) }

            expectSuccess = false
            HttpResponseValidator {
                handleResponseException { cause ->
                    val responseCode = when (cause) {
                        is HttpConnectTimeoutException -> ResponseCode.REMOTE_SERVICE_CONNECT_TIMEOUT_ERROR
                        is HttpRequestTimeoutException -> ResponseCode.REMOTE_SERVICE_REQUEST_TIMEOUT_ERROR
                        //is HttpSocketTimeoutException -> ResponseCode.REMOTE_SERVICE_SOCKET_TIMEOUT_ERROR
                        else -> ResponseCode.REMOTE_SERVICE_CONNECT_ERROR
                    }
                    throw HttpClientException(
                        responseCode, "HttpClient $name Response Error => ${cause.message}", cause,
                        serviceId = name
                    )
                }
            }
        }
    }
}