/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(InternalSerializationApi::class)

package fanpoll.infra.auth.login.session

import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.auth.principal.UserRole
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.extension.myEquals
import fanpoll.infra.base.extension.myHashCode
import fanpoll.infra.base.json.InstantSerializer
import fanpoll.infra.base.json.json
import fanpoll.infra.base.json.pathOrNull
import fanpoll.infra.base.tenant.TenantId
import fanpoll.infra.base.util.IdentifiableObject
import io.ktor.sessions.generateSessionId
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import mu.KotlinLogging
import java.time.Instant
import java.util.*

class UserSession : IdentifiableObject<UserSession.Id> {

    override val id: Id
    val value: Value

    constructor(id: Id, value: Value) {
        this.id = id
        this.value = value
    }

    constructor(principal: UserPrincipal, expireTime: Instant?, data: JsonObject?) {
        id = with(principal) {
            Id(userType.projectId, source, userType, userId, generateSessionId())
        }
        value = Value(id.value, principal.createAt, expireTime, principal.clientId, principal.tenantId, principal.roles, data)
    }

    override fun toString(): String = value.toString()

    data class Id(
        val projectId: String,
        val source: PrincipalSource,
        val userType: UserType,
        val userId: UUID,
        val nonce: String
    ) {

        val value: String = "${userType.projectId}:${source.name}:${userType.name}:$userId:$nonce"

        companion object {

            fun parse(text: String): Id {
                val s = text.split(":")
                return Id(
                    s[0], PrincipalSource.lookup(s[0], s[1]), UserType.lookup(s[0], s[2]), UUID.fromString(s[3]), s[4]
                )
            }
        }

        override fun equals(other: Any?) = myEquals(other, { value })
        override fun hashCode() = myHashCode({ value })
        override fun toString(): String = value
    }

    @Serializable
    data class Value(
        val sid: String,
        @Serializable(with = InstantSerializer::class) val loginTime: Instant,
        @Serializable(with = InstantSerializer::class) var expireTime: Instant? = null,
        val clientId: String? = null,
        val tenantId: TenantId? = null,
        val roles: Set<UserRole>? = null,
        val data: JsonObject? = null
    ) {

        fun session(): UserSession = UserSession(Id.parse(sid), this)

        fun principal(): UserPrincipal {
            val id = Id.parse(sid)
            return UserPrincipal(
                id.userType, id.userId, roles, id.source, clientId, tenantId, false,
                UserSession(id, this)
            )
        }

        override fun equals(other: Any?) = myEquals(other, { sid })
        override fun hashCode() = myHashCode({ sid })
        override fun toString(): String = sid
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }

    // session data content schema may be changed. if we can't deserialize data then return null

    inline fun <reified T : Any> getData(path: String? = null): T? {
        val sessionData = value.data
        return if (sessionData == null) null
        else {
            try {
                when (path) {
                    null -> json.decodeFromJsonElement(T::class.serializer(), sessionData)
                    else -> sessionData.pathOrNull(path)?.let { json.decodeFromJsonElement(T::class.serializer(), it) }
                }
            } catch (e: Throwable) {
                logger.info { "$id invalid session data => ${e.message}" }
                null
            }
        }
    }

    inline fun <reified T : Any> getDataAsList(path: String): List<T>? {
        val sessionData = value.data
        return if (sessionData == null) null
        else {
            try {
                (sessionData.pathOrNull(path) as? JsonArray)
                    ?.let { json.decodeFromJsonElement(ListSerializer(T::class.serializer()), it) }
            } catch (e: Throwable) {
                logger.info { "$id invalid session data => ${e.message}" }
                null
            }
        }
    }
}