/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

@file:OptIn(InternalSerializationApi::class)

package fanpoll.infra.session

import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.auth.principal.UserRole
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.json.kotlinx.InstantSerializer
import fanpoll.infra.base.json.kotlinx.UUIDSerializer
import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.base.json.kotlinx.pathOrNull
import fanpoll.infra.base.util.IdentifiableObject
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.sessions.generateSessionId
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import java.time.Instant
import java.util.*

@Serializable
class UserSession(
    override val account: String,
    override val source: PrincipalSource,
    @Serializable(with = UUIDSerializer::class) override val userId: UUID,
    override val userType: UserType,
    override val userRoles: Set<UserRole>,
    override val runAs: Boolean = false,
    override val clientId: String? = null,
    override val clientVersion: String? = null,
    @Serializable(with = InstantSerializer::class) val loginTime: Instant,
    @Serializable(with = InstantSerializer::class) var expireTime: Instant? = null,
    val data: JsonObject? = null
) : UserPrincipal, IdentifiableObject<String>() {

    @Transient
    override val id: String = userId.toString()

    val sid: String = "${source.projectId}:${source.name}:${userType.name}:$userId:${generateSessionId()}"

    override fun toString(): String = sid

    companion object {
        val logger = KotlinLogging.logger {}
    }

    // session data content schema may be changed. if we can't deserialize data then return null
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : Any> getData(path: String? = null): T? {
        return if (data == null) null
        else {
            try {
                when (path) {
                    null -> json.decodeFromJsonElement(T::class.serializer(), data)
                    else -> data.pathOrNull(path)?.let { json.decodeFromJsonElement(T::class.serializer(), it) }
                }
            } catch (e: Throwable) {
                logger.info { "$sid invalid session data => ${e.message}" }
                null
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : Any> getDataAsList(path: String): List<T>? {
        return if (data == null) null
        else {
            try {
                (data.pathOrNull(path) as? JsonArray)
                    ?.let { json.decodeFromJsonElement(ListSerializer(T::class.serializer()), it) }
            } catch (e: Throwable) {
                logger.info { "$sid invalid session data => ${e.message}" }
                null
            }
        }
    }
}