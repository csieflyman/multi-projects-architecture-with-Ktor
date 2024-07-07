/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.request

import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.principal.ClientAttributeKey
import fanpoll.infra.auth.principal.MyPrincipal
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.extension.bodyString
import fanpoll.infra.base.extension.publicRemoteHost
import fanpoll.infra.base.extension.toMap
import fanpoll.infra.base.json.kotlinx.DurationMicroSerializer
import fanpoll.infra.base.json.kotlinx.InstantSerializer
import fanpoll.infra.base.json.kotlinx.UUIDSerializer
import fanpoll.infra.logging.*
import fanpoll.infra.logging.RequestAttributeKey.TAGS
import fanpoll.infra.session.UserSession
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.util.*

class RequestLog(
    val config: RequestLogConfig,
    val call: ApplicationCall
) : LogEntity() {

    override val id: UUID = UUID.randomUUID()
    override val traceId: String? = call.attributes.getOrNull(RequestAttributeKey.TRACE_ID)
    override val occurAt: Instant = call.attributes[RequestAttributeKey.AT]
    override val type: String = LOG_TYPE
    override val level: LogLevel = Log_Level

    private val principal = call.principal<MyPrincipal>()
    override val project = principal?.source?.projectId ?: "infra"
    val function = call.request.logFunction()
    val source = principal?.source ?: PrincipalSource.System
    val principalId = principal?.id

    val user = if (principal != null) call.sessions.get<UserSession>()?.let { UserLog(it) } else null
    val request = ApplicationRequestLog(call, config.includeHeaders, config.includeQueryString, config.excludeRequestBodyPaths)
    val response = ApplicationResponseLog(call, request)

    val tags = call.attributes.getOrNull(TAGS)

    companion object {
        const val LOG_TYPE = "request"
        private val Log_Level = LogLevel.DEBUG
    }
}

@Serializable
class ApplicationRequestLog(
    val id: String,
    val traceId: String?,
    @Serializable(with = InstantSerializer::class) val at: Instant,
    val method: String?,
    val path: String,
    val headers: Map<String, List<String>>?,
    val querystring: String?,
    val body: String?,
    val ip: String?,
    val clientId: String?,
    val clientVersion: String?
) {
    constructor(
        call: ApplicationCall,
        includeHeaders: Boolean,
        includeQueryString: Boolean,
        excludeRequestBodyPaths: List<String>? = null
    ) : this(
        call.attributes[RequestAttributeKey.ID],
        call.attributes.getOrNull(RequestAttributeKey.TRACE_ID),
        call.attributes[RequestAttributeKey.AT],
        call.request.httpMethod.value,
        call.request.path(),
        if (includeHeaders) call.request.headers.toMap().toMutableMap()
            .apply {
                remove(AuthConst.API_KEY_HEADER_NAME)
                // remove(AuthConst.SESSION_ID_HEADER_NAME)
            }
        else null,
        if (includeQueryString) call.request.logQueryString() else null,
        if (excludeRequestBodyPaths != null && excludeRequestBodyPaths.any { call.request.path().endsWith(it) }) null
        else call.bodyString(),
        call.request.publicRemoteHost,
        call.sessions.get<UserSession>()?.clientId ?: call.attributes.getOrNull(ClientAttributeKey.CLIENT_ID),
        call.attributes.getOrNull(ClientAttributeKey.CLIENT_VERSION)
    )
}

@Serializable
class ApplicationResponseLog(
    @Serializable(with = InstantSerializer::class) val at: Instant,
    val status: Int,
    val body: String?,
    @Serializable(with = DurationMicroSerializer::class) val duration: Duration
) {
    constructor(call: ApplicationCall, request: ApplicationRequestLog) : this(
        Instant.now(),
        call.response.status()?.value ?: 500,
        call.attributes.getOrNull(ResponseAttributeKey.BODY),
        Duration.between(request.at, Instant.now())
    )
}

@Serializable
class UserLog(
    val type: UserType,
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val runAs: Boolean = false
) {
    constructor(session: UserSession) : this(session.userType, session.userId, session.runAs)
}