/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.error

import fanpoll.infra.auth.principal.MyPrincipal
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.base.exception.BaseException
import fanpoll.infra.base.exception.RemoteServiceException
import fanpoll.infra.base.json.kotlinx.DurationMicroSerializer
import fanpoll.infra.base.json.kotlinx.InstantSerializer
import fanpoll.infra.logging.LogEntity
import fanpoll.infra.logging.LogLevel
import fanpoll.infra.logging.RequestAttributeKey
import fanpoll.infra.logging.RequestAttributeKey.TAGS
import fanpoll.infra.logging.logFunction
import fanpoll.infra.logging.request.ApplicationRequestLog
import fanpoll.infra.logging.request.ApplicationResponseLog
import fanpoll.infra.logging.request.UserLog
import fanpoll.infra.session.UserSession
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.util.*

class ErrorLog private constructor(
    val exception: BaseException,
    val call: ApplicationCall? = null,
    val function: String,
    val tags: Map<String, String>? = null
) : LogEntity() {

    override val id: UUID = UUID.randomUUID()
    override val traceId: String? = call?.attributes?.getOrNull(RequestAttributeKey.TRACE_ID)
    override val occurAt = exception.occurAt
    override val type: String = LOG_TYPE
    override val level: LogLevel = Log_Level

    private val principal = call?.principal<MyPrincipal>()
    override val project = principal?.source?.projectId ?: "infra"
    val source = principal?.source ?: PrincipalSource.System
    val principalId = principal?.id

    val user = if (principal != null) call?.sessions?.get<UserSession>()?.let { UserLog(it) } else null
    val request = call?.let { ApplicationRequestLog(it, includeHeaders = true, includeQueryString = true) }
    val response = request?.let { ApplicationResponseLog(call!!, request) }
    val serviceRequest = (exception as? RemoteServiceException)?.let { ServiceRequestLog(it) }

    // LIMITATION => ktor StatusPages Plugin exception run at ApplicationCallPipeline before
    //  CallLogging Plugin run at ApplicationSendPipeline.Render

    companion object {

        const val LOG_TYPE = "error"
        private val Log_Level = LogLevel.ERROR

        fun request(e: BaseException, call: ApplicationCall): ErrorLog =
            ErrorLog(e, call, call.request.logFunction(), call.attributes.getOrNull(TAGS))

        fun internal(e: BaseException, function: String, tags: Map<String, String>? = null): ErrorLog =
            ErrorLog(e, null, function, tags)
    }
}

@Serializable
class ServiceRequestLog(
    val name: String,
    val api: String,
    val reqId: String?,
    @Serializable(with = InstantSerializer::class) val reqAt: Instant?,
    val reqBody: String?,
    val rspCode: String?,
    @Serializable(with = InstantSerializer::class) val rspAt: Instant?,
    val rspBody: String?,
    @Serializable(with = DurationMicroSerializer::class) val duration: Duration?
) {
    constructor(e: RemoteServiceException) : this(e.name, e.api, e.reqId, e.reqAt, e.reqBody, e.rspCode, e.rspAt, e.rspBody, e.duration)
}

