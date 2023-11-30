/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
package fanpoll.infra.auth.login.session

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.json.json
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.util.DateTimeUtils
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.redis.RedisKeyspaceNotificationListener
import fanpoll.infra.redis.ktorio.RedisClient
import fanpoll.infra.redis.ktorio.commands.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.util.*

class RedisSessionStorage(
    private val sessionConfig: SessionConfig,
    private val redisClient: RedisClient,
    private val redisKeyspaceNotificationListener: RedisKeyspaceNotificationListener? = null,
    private val logWriter: LogWriter
) : MySessionStorage() {

    private val logger = KotlinLogging.logger {}

    private val sessionKeyPrefix: String = redisClient.rootKeyPrefix + ":session:"
    private val sessionIdsKeyPrefix: String = redisClient.rootKeyPrefix + ":sessionIds:"

    init {
        if (redisKeyspaceNotificationListener != null) {
            subscribeSessionKeyExpired()
        }
    }

    override suspend fun setSession(session: UserSession) {
        logger.debug { "login session: ${session.id}" }

        val sessionConfig = session.id.source.getAuthConfig().user!!.session ?: sessionConfig
        setSession(session, session.value.loginTime, sessionConfig.expireDuration)
    }

    override suspend fun extendExpireTime(session: UserSession) {
        val sessionConfig = session.id.source.getAuthConfig().user!!.session ?: sessionConfig
        val now = Instant.now()
        val expireTime = session.value.expireTime
        if (sessionConfig.extendDuration != null && expireTime != null &&
            expireTime.isAfter(now) &&
            Duration.between(now, expireTime) <= sessionConfig.extendDuration
        ) {
            logger.debug { "extent session: ${session.id}" }
            setSession(session, now, sessionConfig.expireDuration)
        }
    }

    override suspend fun deleteSession(session: UserSession) {
        logger.debug { "logout session: ${session.id}" }

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

    override suspend fun getSession(sid: String): UserSession? =
        redisClient.get(buildSessionKey(sid))?.let { json.decodeFromString(UserSession.Value.serializer(), it).session() }

    override suspend fun getSessionAsText(sid: String): String? = redisClient.get(buildSessionKey(sid))

    override suspend fun hasSession(sid: String): Boolean = redisClient.exists(buildSessionKey(sid))

    private fun buildSessionKey(session: UserSession): String = buildSessionKey(session.id.value)

    private fun buildSessionKey(sid: String): String = sessionKeyPrefix + sid

    private fun buildSessionIdKey(userId: UUID): String = sessionIdsKeyPrefix + userId.toString()

    private fun subscribeSessionKeyExpired() {
        redisKeyspaceNotificationListener!!.subscribeKeyspaceNotification { notification ->
            if (notification.isKeyEvent && notification.event == "expired" && notification.key.startsWith(sessionKeyPrefix)) {
                logger.debug { "session key expired: ${notification.key}" }
                try {
                    val segments = notification.key.split(":")
                    val userId = UUID.fromString(segments[5])
                    val sessionKey = notification.key
                    redisClient.hdel(buildSessionIdKey(userId), sessionKey)
                } catch (e: Throwable) {
                    val errorMsg = "subscribeSessionKeyExpired error"
                    logger.error(e) { "$errorMsg => $notification" }
                    logWriter.write(
                        ErrorLog.internal(
                            InternalServerException(
                                InfraResponseCode.REDIS_KEY_NOTIFICATION_ERROR, errorMsg, e,
                                mapOf("notification" to notification)
                            ),
                            "SessionService", mapOf("redisPubSubMessageId" to notification.id)
                        )
                    )
                }
            }
        }
    }
}