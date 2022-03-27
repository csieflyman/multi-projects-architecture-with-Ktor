/*
 * Copyright (c) 2022. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.writers

import fanpoll.infra.ServerConfig
import fanpoll.infra.base.extension.toEpocNano
import fanpoll.infra.base.httpclient.CIOHttpClientConfig
import fanpoll.infra.base.httpclient.HttpClientCreator
import fanpoll.infra.base.httpclient.textBody
import fanpoll.infra.base.util.DateTimeUtils
import fanpoll.infra.logging.LogEntity
import fanpoll.infra.logging.request.RequestLog
import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.util.InternalAPI
import io.ktor.util.encodeBase64
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import mu.KotlinLogging

data class LokiConfig(
    val username: String,
    val password: String,
    val pushUrl: String,
    var cio: CIOHttpClientConfig? = null
) {

    class Builder {

        lateinit var username: String
        lateinit var password: String
        lateinit var pushUrl: String
        private var cio: CIOHttpClientConfig? = null

        fun cio(block: CIOHttpClientConfig.Builder.() -> Unit) {
            cio = CIOHttpClientConfig.Builder().apply(block).build()
        }

        fun build(): LokiConfig {
            return LokiConfig(username, password, pushUrl, cio)
        }
    }
}

class LokiLogWriter(
    private val lokiConfig: LokiConfig,
    private val serverConfig: ServerConfig
) : LogWriter {

    private val logger = KotlinLogging.logger {}

    private val client: HttpClient = HttpClientCreator.create("LokiLogWriter", lokiConfig.cio).config {

        defaultRequest {
            header(HttpHeaders.Authorization, constructBasicAuthValue(lokiConfig.username, lokiConfig.password))
        }
    }

    private val json = io.ktor.client.features.json.defaultSerializer()

    override fun write(logEntity: LogEntity) {
        val requestLog = logEntity as RequestLog
        val response = runBlocking {
            client.post<HttpResponse>(lokiConfig.pushUrl) {
                body = json.write(buildJsonObject {
                    putJsonArray("streams") {
                        addJsonObject {
                            putJsonObject("stream") {
                                put("project", requestLog.project)
                                put("env", serverConfig.env.name)
                            }
                            putJsonArray("values") {
                                addJsonArray {
                                    add(JsonPrimitive(requestLog.request.at.toEpocNano()).toString())
                                    add(JsonPrimitive(toLokiLogText(requestLog)))
                                }
                            }
                        }
                    }
                })
            }
        }
        logger.debug { "${response.status.value}-${response.textBody()}" }
    }

    @OptIn(InternalAPI::class)
    private fun constructBasicAuthValue(username: String, password: String): String {
        val authString = "$username:$password"
        val authBuf = authString.toByteArray(Charsets.UTF_8).encodeBase64()

        return "Basic $authBuf"
    }

private fun toLokiLogText(requestLog: RequestLog): String = mutableMapOf(
    "level" to requestLog.logLevel.name,
    "logType" to requestLog.logType,
    "function" to requestLog.function,
    "source" to requestLog.source.name
).apply {
    with(requestLog) {
        put("req.id", request.id)
        put("req.at", DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(request.at))
        put("req.api", "${request.method} ${request.path}")
        if (request.headers != null)
            put("req.headers", request.headers.toString())
        if (request.querystring != null)
            put("req.querystring", request.querystring)
        if (request.body != null)
            put("req.body", request.body)
        if (request.ip != null)
            put("req.ip", request.ip)
        if (request.clientId != null)
            put("req.clientId", request.clientId)
        if (request.clientVersion != null)
            put("req.clientVersion", request.clientVersion)

            put("rsp.at", DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(response.at))
            put("rsp.status", response.status.toString())
            if (response.body != null)
                put("rsp.body", response.body)
            put("duration", response.duration.toString())

            if (user != null) {
                put("user.type", user.type.name)
                put("user.id", user.id.toString())
                put("user.runAs", user.runAs.toString())
            }

            principalId?.let { put("principalId", it) }
            tenantId?.let { put("tenantId", it.value) }

            tags?.let { putAll(it.mapKeys { key -> "tag.$key" }) }
        }
    }.entries.joinToString(" ")
}