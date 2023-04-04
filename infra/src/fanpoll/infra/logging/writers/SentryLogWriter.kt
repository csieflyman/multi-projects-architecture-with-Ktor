/*
 * Copyright (c) 2022. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.writers

import fanpoll.infra.AppInfoConfig
import fanpoll.infra.ServerConfig
import fanpoll.infra.base.util.DateTimeUtils
import fanpoll.infra.logging.LogEntity
import fanpoll.infra.logging.LogLevel
import fanpoll.infra.logging.error.ErrorLog
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message
import io.sentry.protocol.Request
import io.sentry.protocol.User

data class SentryConfig(val dsn: String, val debug: Boolean = false) {

    class Builder {

        private lateinit var dsn: String
        private var debug: Boolean = false

        fun build(): SentryConfig {
            return SentryConfig(dsn, debug)
        }
    }
}

class SentryLogWriter(
    private val sentryConfig: SentryConfig,
    private val appInfoConfig: AppInfoConfig,
    private val serverConfig: ServerConfig,
) : LogWriter {

    init {
        Sentry.init { options ->
            // https://docs.sentry.io/platforms/java/configuration/
            with(options) {
                dsn = sentryConfig.dsn
                setDebug(sentryConfig.debug)
                release = appInfoConfig.git.tag
                environment = serverConfig.env.name
                serverName = serverConfig.instanceId
                //connectionTimeoutMillis = 5000
                //readTimeoutMillis = 5000
                //tags
            }
        }
    }

    override fun write(logEntity: LogEntity) {
        val errorLog = logEntity as ErrorLog
        val event = toSentryEvent(errorLog)
        Sentry.captureEvent(event)
    }

    private fun toSentryEvent(errorLog: ErrorLog): SentryEvent = SentryEvent(errorLog.exception).apply {
        level = toSentryLevel(errorLog.level)
        message = Message().apply {
            message = errorLog.exception.message
        }

        transaction = "${errorLog.project}/${errorLog.function}"

        tags = toSentryTags(errorLog)

        errorLog.user?.let { user = toSentryUser(errorLog) }

        errorLog.request?.let { request = toSentryRequest(errorLog) }

        setExtras(toSentryExtras(errorLog))
    }

    private fun toSentryLevel(level: LogLevel): SentryLevel = when (level) {
        LogLevel.DEBUG -> SentryLevel.DEBUG
        LogLevel.INFO -> SentryLevel.INFO
        LogLevel.WARN -> SentryLevel.WARNING
        LogLevel.ERROR -> SentryLevel.ERROR
        LogLevel.FATAL -> SentryLevel.FATAL
    }

    private fun toSentryTags(errorLog: ErrorLog): Map<String, String> = mutableMapOf(
        "errorId" to errorLog.id.toString(),
        "level" to errorLog.level.name,
        "errorCode" to errorLog.exception.code.value,
        "project" to errorLog.project,
        "function" to errorLog.function,
        "source" to errorLog.source.name
    ).apply {
        with(errorLog) {
            tags?.let { putAll(it) }

            principalId?.let { put("principalId", it) }
            tenantId?.let { put("tenantId", it.value) }

            request?.let { req ->
                put("reqId", req.id)
                req.traceId?.let { put("traceId", it) }
                req.clientId?.let { put("clientId", it) }
                req.clientVersion?.let { put("clientVersion", it) }
            }

            serviceRequest?.let { req ->
                put("serviceName", req.name)
                req.reqId?.let { put("serviceReqId", req.reqId) }
            }
        }
    }

    private fun toSentryUser(errorLog: ErrorLog): User = User().apply {
        requireNotNull(errorLog.user)
        id = "${errorLog.user.type}/${errorLog.user.id}"
        ipAddress = errorLog.request?.ip
        with(errorLog.user) {
            data = mutableMapOf(
                "userType" to type.name,
                "userId" to id.toString(),
                "runAs" to runAs.toString()
            )
        }
    }

    private fun toSentryRequest(errorLog: ErrorLog): Request = Request().apply {
        with(errorLog) {
            requireNotNull(request)
            requireNotNull(response)

            url = request.path
            //cookies =
            method = request.method
            queryString = request.querystring
            headers = request.headers?.mapValues { it.value.joinToString(",") }
            data = request.body
            others = mutableMapOf(
                "reqAt" to DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(request.at),
                "ip" to (request.ip ?: ""),
                "clientId" to (request.clientId ?: ""),
                "clientVersion" to (request.clientVersion ?: ""),
                "rspAt" to DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(response.at),
                "rspStatus" to response.status.toString(),
                "rspBody" to (response.body ?: "")
            )
        }
    }

    private fun toSentryExtras(errorLog: ErrorLog): Map<String, Any> {
        val extras = mutableMapOf<String, Any>()
        errorLog.exception.dataMap?.let { extras.putAll(it) }
        errorLog.serviceRequest?.let {
            extras["serviceName"] = it.name
            extras["serviceApi"] = it.api

            if (it.reqId != null)
                extras["serviceReqId"] = it.reqId
            if (it.reqAt != null)
                extras["serviceReqAt"] = DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(it.reqAt)
            if (it.reqBody != null)
                extras["serviceReqBody"] = it.reqBody

            if (it.rspCode != null)
                extras["serviceRspCode"] = it.rspCode
            if (it.rspAt != null)
                extras["serviceRspAt"] = DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(it.rspAt)
            if (it.rspBody != null)
                extras["serviceRspBody"] = it.rspBody
            if (it.duration != null)
                extras["serviceDuration"] = it.duration
        }
        return extras
    }
}