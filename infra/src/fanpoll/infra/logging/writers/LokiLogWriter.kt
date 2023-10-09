/*
 * Copyright (c) 2022. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.writers

import fanpoll.infra.ServerConfig
import fanpoll.infra.auth.login.logging.LoginLog
import fanpoll.infra.base.extension.toEpocNano
import fanpoll.infra.base.extension.toMicros
import fanpoll.infra.base.httpclient.CIOHttpClientConfig
import fanpoll.infra.base.httpclient.HttpClientCreator
import fanpoll.infra.base.httpclient.bodyAsTextBlocking
import fanpoll.infra.base.util.DateTimeUtils
import fanpoll.infra.logging.LogEntity
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.error.ServiceRequestLog
import fanpoll.infra.logging.request.ApplicationRequestLog
import fanpoll.infra.logging.request.ApplicationResponseLog
import fanpoll.infra.logging.request.RequestLog
import fanpoll.infra.logging.request.UserLog
import fanpoll.infra.notification.logging.NotificationMessageLog
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.util.InternalAPI
import io.ktor.util.encodeBase64
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

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

    private val json = io.ktor.client.plugins.json.defaultSerializer()

    override fun write(logEntity: LogEntity) {
        val response = runBlocking {
            client.post(lokiConfig.pushUrl) {
                setBody(json.write(buildJsonObject {
                    putJsonArray("streams") {
                        addJsonObject {
                            putJsonObject("stream") {
                                put("project", logEntity.project)
                                put("logType", logEntity.type)
                                put("env", serverConfig.env.name)
                            }
                            putJsonArray("values") {
                                addJsonArray {
                                    add(JsonPrimitive(logEntity.occurAt.toEpocNano()).toString())
                                    add(JsonPrimitive(toLokiLogText(logEntity)))
                                }
                            }
                        }
                    }
                }))
            }
        }
        logger.debug { "${response.status.value}-${response.bodyAsTextBlocking()}" }
    }

    @OptIn(InternalAPI::class)
    private fun constructBasicAuthValue(username: String, password: String): String {
        val authString = "$username:$password"
        val authBuf = authString.toByteArray(Charsets.UTF_8).encodeBase64()

        return "Basic $authBuf"
    }

    private fun toLokiLogText(logEntity: LogEntity): String {
        val logMap = mutableMapOf<String, String>().apply {
            with(logEntity) {
                put("id", id.toString())
                put("occurAt", DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(occurAt))
                put("level", level.name)
                put("logType", type)
                put("project", project)
                // traceID is a Loki built-in derived field and use traceID=(\w+) regex pattern to extract value
                if (logEntity.traceId != null)
                    put("traceID", logEntity.traceId!!.replace("-", ""))
            }
        }
        val subTypeLogMap = when (logEntity) {
            is RequestLog -> fromRequestLog(logEntity)
            is ErrorLog -> fromErrorLog(logEntity)
            is LoginLog -> fromLoginLog(logEntity)
            is NotificationMessageLog -> fromNotificationMessageLog(logEntity)
            else -> error("${logEntity.type} is not implemented yet")
        }
        logMap.putAll(subTypeLogMap)
        return logMap.entries.joinToString(" ")
    }

    private fun fromNotificationMessageLog(messageLog: NotificationMessageLog) = mutableMapOf<String, String>().apply {
        with(messageLog) {
            put("notificationId", notificationId.toString())
            put("eventId", eventId.toString())
            put("notificationType", notificationType.name)
            version?.let { put("version", it) }
            put("channel", channel.name)
            put("lang", lang.code)
            sendAt?.let { put("sendAt", DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(it)) }
            errorMsg?.let { put("errorMsg", it) }
            put("receivers", receivers.toString())
            content?.let { put("content", it) }
            put("success", success.toString())
            successList?.let { put("successList", it.toString()) }
            failureList?.let { put("failureList", it.toString()) }
            invalidRecipientIds?.let { put("invalidRecipientIds", it.toString()) }
            rspCode?.let { put("rspCode", it) }
            rspMsg?.let { put("rspMsg", it) }
            rspAt?.let { put("rspAt", DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(it)) }
            duration?.let { put("duration", it.toMicros().toString()) }
            rspBody?.let { put("rspBody", it) }
        }
    }

    private fun fromLoginLog(loginLog: LoginLog) = mutableMapOf<String, String>().apply {
        with(loginLog) {
            put("userId", userId.toString())
            put("resultCode", resultCode.name)
            put("source", source.name)
            tenantId?.let { put("tenantId", it.value) }
            clientId?.let { put("clientId", it) }
            clientVersion?.let { put("clientVersion", it) }
            ip?.let { put("ip", it) }
            sid?.let { put("sid", it) }
        }
    }

    private fun fromErrorLog(errorLog: ErrorLog) = mutableMapOf<String, String>().apply {
        with(errorLog) {
            put("function", function)
            put("source", source.name)
            principalId?.let { put("principalId", it) }
            tenantId?.let { put("tenantId", it.value) }
            tags?.let { putAll(it.mapKeys { key -> "tag.$key" }) }

            if (request != null) {
                fromApplicationRequestLog(request)
            }
            if (response != null) {
                putAll(fromApplicationResponseLog(response))
            }
            if (user != null) {
                fromUserLog(user)
            }
            if (serviceRequest != null) {
                fromServiceRequestLog(serviceRequest)
            }
        }
    }

    private fun fromRequestLog(requestLog: RequestLog) = mutableMapOf<String, String>().apply {
        with(requestLog) {
            put("function", function)
            put("source", source.name)
            principalId?.let { put("principalId", it) }
            tenantId?.let { put("tenantId", it.value) }
            tags?.let { putAll(it.mapKeys { key -> "tag.$key" }) }

            putAll(fromApplicationRequestLog(request))
            putAll(fromApplicationResponseLog(response))
            if (user != null) {
                fromUserLog(user)
            }
        }
    }

    private fun fromApplicationRequestLog(request: ApplicationRequestLog) = mutableMapOf<String, String>().apply {
        with(request) {
            put("req.id", id)
            put("req.api", "[$method] $path")
            headers?.let { put("req.headers", it.toString()) }
            querystring?.let { put("req.querystring", it) }
            body?.let { put("req.body", it) }
            ip?.let { put("req.ip", it) }
            clientId?.let { put("req.clientId", it) }
            clientVersion?.let { put("req.clientVersion", it) }
        }
    }

    private fun fromApplicationResponseLog(response: ApplicationResponseLog) = mutableMapOf<String, String>().apply {
        with(response) {
            put("rsp.at", DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(at))
            put("rsp.status", status.toString())
            body?.let { put("rsp.body", it) }
            put("duration", duration.toMicros().toString())
        }
    }

    private fun fromUserLog(user: UserLog) = mutableMapOf<String, String>().apply {
        with(user) {
            put("user.type", type.name)
            put("user.id", id.toString())
            put("user.runAs", runAs.toString())
        }
    }

    private fun fromServiceRequestLog(service: ServiceRequestLog) = mutableMapOf<String, String>().apply {
        with(service) {
            put("service.name", name)
            put("service.api", api)
            reqId?.let { put("service.req.id", it) }
            reqAt?.let { put("service.req.at", DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(it)) }
            reqBody?.let { put("service.req.body", it) }
            rspCode?.let { put("service.rsp.code", it) }
            rspAt?.let { put("service.rsp.at", DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(it)) }
            rspBody?.let { put("service.rsp.body", it) }
            duration?.let { put("service.duration", it.toMicros().toString()) }
        }
    }
}