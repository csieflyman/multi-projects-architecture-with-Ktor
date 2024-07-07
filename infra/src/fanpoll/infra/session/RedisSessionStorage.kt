/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */
package fanpoll.infra.session

import fanpoll.infra.base.datetime.DateTimeUtils
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.redis.RedisKeyspaceNotificationListener
import fanpoll.infra.redis.ktorio.RedisClient
import fanpoll.infra.redis.ktorio.commands.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import java.time.temporal.ChronoUnit
import java.util.*

class RedisSessionStorage(
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
        val sessionKey = buildSessionKey(session)
        val sessionIdsKey = buildSessionIdKey(session.userId)

        val expireDurationMs = session.expireTime?.let { ChronoUnit.MILLIS.between(session.loginTime, it) }

        redisClient.set(sessionKey, json.encodeToString(session), expireDurationMs)
        redisClient.hset(
            sessionIdsKey, sessionKey,
            session.expireTime?.let { DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(it) } ?: ""
        )
    }

    override suspend fun setSessionExpireTime(session: UserSession) {
        if (session.expireTime != null)
            redisClient.expireat(session.sid, Date.from(session.expireTime))
    }

    override suspend fun deleteSession(session: UserSession) {
        logger.debug { "logout session: ${session.sid}" }

        val sessionKey = buildSessionKey(session)
        val sessionIdsKey = buildSessionIdKey(session.userId)

        redisClient.del(sessionKey)
        redisClient.hdel(sessionIdsKey, sessionKey)
    }

    override suspend fun getSession(sid: String): UserSession? =
        redisClient.get(buildSessionKey(sid))?.let { json.decodeFromString(it) }

    override suspend fun getSessionAsText(sid: String): String? = redisClient.get(buildSessionKey(sid))

    override suspend fun hasSession(sid: String): Boolean = redisClient.exists(buildSessionKey(sid))

    private fun buildSessionKey(session: UserSession): String = buildSessionKey(session.sid)

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