/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login.logging

import fanpoll.infra.auth.login.LoginResultCode
import fanpoll.infra.base.json.kotlinx.InstantSerializer
import fanpoll.infra.base.json.kotlinx.UUIDSerializer
import fanpoll.infra.logging.LogEntity
import fanpoll.infra.logging.LogLevel
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

@Serializable
data class LoginLog(
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    val resultCode: LoginResultCode,
    override val project: String,
    val sourceName: String,
    val clientId: String? = null,
    val clientVersion: String?,
    val ip: String? = null,
    override val traceId: String?,
    val sid: String? = null
) : LogEntity() {

    @Serializable(with = UUIDSerializer::class)
    override val id: UUID = UUID.randomUUID()

    @Serializable(with = InstantSerializer::class)
    override val occurAt: Instant = Instant.now()

    override val type: String = LOG_TYPE

    override val level: LogLevel = Log_Level

    companion object {
        const val LOG_TYPE = "login"
        private val Log_Level = LogLevel.INFO
    }
}