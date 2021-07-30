/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.request

import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.base.json.InstantSerializer
import fanpoll.infra.base.json.UUIDSerializer
import fanpoll.infra.base.tenant.TenantId
import fanpoll.infra.logging.LogLevel
import fanpoll.infra.logging.LogMessage
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

@Serializable
data class RequestLog(
    val reqId: String,
    @Serializable(with = InstantSerializer::class) val reqAt: Instant,
    val api: String,
    val headers: String? = null,
    val querystring: String? = null,
    val reqBody: String?,
    val project: String,
    val function: String,
    val tag: String? = null,
    val source: PrincipalSource,
    val tenantId: TenantId? = null,
    val principal: String,
    val runAs: Boolean,
    val clientId: String? = null,
    val clientVersion: String? = null,
    val ip: String? = null,
    @Serializable(with = InstantSerializer::class) val rspAt: Instant,
    val rspTime: Long,
    val rspStatus: Int,
    val rspBody: String?
) : LogMessage() {

    @Serializable(with = UUIDSerializer::class)
    override val id: UUID = UUID.randomUUID()

    @Serializable(with = InstantSerializer::class)
    override val occurAt: Instant = reqAt

    override val logType: String = LOG_TYPE

    override val logLevel: LogLevel = Log_Level

    companion object {
        const val LOG_TYPE = "request"
        private val Log_Level = LogLevel.DEBUG
    }
}