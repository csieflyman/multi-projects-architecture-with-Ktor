/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(InternalSerializationApi::class)

package fanpoll.infra.login

import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.ResponseCode
import fanpoll.infra.auth.*
import fanpoll.infra.logging.ErrorLogDTO
import fanpoll.infra.logging.LogManager
import fanpoll.infra.logging.LogMessage
import fanpoll.infra.logging.LogType
import fanpoll.infra.model.TenantId
import fanpoll.infra.redis.RedisConfig
import fanpoll.infra.redis.RedisManager
import fanpoll.infra.redis.ktorio.RedisClient
import fanpoll.infra.redis.ktorio.commands.*
import fanpoll.infra.utils.*
import io.ktor.sessions.generateSessionId
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.util.*

object SessionService {

    private val logger = KotlinLogging.logger {}

    private lateinit var redisClient: RedisClient

    private lateinit var sessionKeyPrefix: String
    private lateinit var sessionIdsKeyPrefix: String

    fun init(redisClient: RedisClient, redisConfig: RedisConfig) {
        SessionService.redisClient = redisClient

        sessionKeyPrefix = redisConfig.rootKeyPrefix + ":session:"
        sessionIdsKeyPrefix = redisConfig.rootKeyPrefix + ":sessionIds:"

        if (redisConfig.pubSub?.keyspaceNotification != null &&
            redisConfig.pubSub.keyspaceNotification.subscribeSessionKeyExpired
        ) {
            subscribeSessionKeyExpired()
        }
    }

    suspend fun login(session: UserSession) {
        logger.debug("login session: ${session.id}")

        val sessionConfig = (session.id.source.getConfig() as UserAuthConfig).session!!
        setSession(session, session.value.loginTime, sessionConfig.expireDuration)
    }

    suspend fun extendExpireTimeIfNeed(session: UserSession) {
        val sessionConfig = (session.id.source.getConfig() as UserAuthConfig).session!!
        val now = Instant.now()
        val expireTime = session.value.expireTime
        if (sessionConfig.extendDuration != null && expireTime != null &&
            expireTime.isAfter(now) &&
            Duration.between(now, expireTime) <= sessionConfig.extendDuration
        ) {
            logger.debug("extent session: ${session.id}")
            setSession(session, now, sessionConfig.expireDuration)
        }
    }

    suspend fun logout(session: UserSession) {
        logger.debug("logout session: ${session.id}")

        val sessionKey = buildSessionKey(session)
        val sessionIdsKey = buildSessionIdKey(session.id.userId)

        redisClient.del(sessionKey)
        redisClient.hdel(sessionIdsKey, sessionKey)
    }

    private suspend fun setSession(session: UserSession, startTime: Instant, expireDuration: Duration?) {
        session.value.expireTime = expireDuration?.let { startTime.plus(it) }

        val sessionKey = buildSessionKey(session)
        val sessionIdsKey = buildSessionIdKey(session.id.userId)

        redisClient.set(
            sessionKey,
            json.encodeToString(UserSession.Value.serializer(), session.value),
            expireDuration?.toMillis()
        )
        redisClient.hset(
            sessionIdsKey, sessionKey,
            session.value.expireTime?.let { DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(it) } ?: ""
        )
    }

    suspend fun getSession(sid: String): UserSession? =
        redisClient.get(buildSessionKey(sid))?.let { json.decodeFromString(UserSession.Value.serializer(), it).session() }

    suspend fun getSessionAsByteArray(sid: String): ByteArray? = redisClient.get(buildSessionKey(sid))?.toByteArray()

    suspend fun hasSession(sid: String): Boolean = redisClient.exists(buildSessionKey(sid))

    private fun buildSessionKey(session: UserSession): String = buildSessionKey(session.id.value)

    private fun buildSessionKey(sid: String): String = sessionKeyPrefix + sid

    private fun buildSessionIdKey(userId: UUID): String = sessionIdsKeyPrefix + userId.toString()

    private fun subscribeSessionKeyExpired() {
        RedisManager.subscribeKeyspaceNotification { notification ->
            if (notification.isKeyEvent && notification.event == "expired" && notification.key.startsWith(sessionKeyPrefix)) {
                logger.debug { "session key expired: ${notification.key}" }
                try {
                    val segments = notification.key.split(":")
                    val userId = UUID.fromString(segments[5])
                    val sessionKey = notification.key
                    redisClient.hdel(buildSessionIdKey(userId), sessionKey)
                } catch (e: Throwable) {
                    LogManager.write(
                        LogMessage(
                            LogType.SERVER_ERROR, ErrorLogDTO.internal(
                                InternalServerErrorException(ResponseCode.REDIS_KEY_NOTIFICATION_ERROR, null, e),
                                "SessionService", "subscribeSessionKeyExpired"
                            )
                        )
                    )
                }
            }
        }
    }
}

class UserSession {

    val id: Id
    val value: Value

    constructor(id: Id, value: Value) {
        this.id = id
        this.value = value
    }

    constructor(principal: UserPrincipal, loginTime: Instant, expireTime: Instant?, data: JsonObject?) {
        id = with(principal) {
            Id(userType.projectId, source, userType, userId, generateSessionId())
        }
        value = Value(id.value, loginTime, expireTime, principal.clientId, principal.tenantId?.value, principal.roles, data)
    }

    override fun equals(other: Any?) = myEquals(other, { id })
    override fun hashCode() = myHashCode({ id })
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
        val tenantId: String? = null,
        val roles: Set<UserRole>? = null,
        val data: JsonObject? = null
    ) {

        fun session(): UserSession = UserSession(Id.parse(sid), this)

        fun principal(): UserPrincipal {
            val id = Id.parse(sid)
            return UserPrincipal(
                id.userType, id.userId, roles, id.source, clientId, tenantId?.let { TenantId(it) }, false,
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