/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login.logging

import fanpoll.infra.auth.login.LoginResultCode
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
data class LoginLog(
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    val resultCode: LoginResultCode,
    @Serializable(with = InstantSerializer::class) override val occurAt: Instant,
    val project: String,
    val source: PrincipalSource,
    val tenantId: TenantId? = null,
    val clientId: String? = null,
    val clientVersion: String?,
    val ip: String? = null,
    val sid: String? = null
) : LogMessage() {

    @Serializable(with = UUIDSerializer::class)
    override val id: UUID = UUID.randomUUID()

    override val logType: String = LOG_TYPE

    override val logLevel: LogLevel = Log_Level

    companion object {
        const val LOG_TYPE = "login"
        private val Log_Level = LogLevel.INFO
    }
}