/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.redis

import fanpoll.infra.base.async.CoroutineActor
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.redis.ktorio.commands.RedisPubSub
import fanpoll.infra.redis.ktorio.commands.redisKeyspaceNotificationChannel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch

class RedisKeyspaceNotificationListener(
    config: KeyspaceNotificationConfig,
    redisPubSub: RedisPubSub,
    logWriter: LogWriter
) {

    private val logger = KotlinLogging.logger {}

    private val actorName = "RedisKeyspaceNotificationListener"

    private val keyspaceNotificationBlocks: MutableList<suspend (RedisPubSub.KeyspaceNotification) -> Unit> = mutableListOf()

    private val actor: CoroutineActor<RedisPubSub.KeyspaceNotification> = CoroutineActor(
        actorName, Channel.UNLIMITED,
        config.processor, Dispatchers.IO,
        this::processKeyspaceNotification, { channel ->
            launch {
                val upstreamChannel = redisKeyspaceNotificationChannel(redisPubSub)
                for (message in upstreamChannel) {
                    try {
                        channel.send(message) // non-blocking if Channel.UNLIMITED
                    } catch (e: Throwable) { // ignore CancellationException because we don't call channel.cancel()
                        var errorMsg = ""
                        if (e is ClosedSendChannelException) {
                            errorMsg = "$actorName is closed"
                            logger.warn { "$errorMsg => $message" }
                        } else {
                            errorMsg = "$actorName unexpected error"
                            logger.error(e) { "$errorMsg => $message" }
                        }
                        logWriter.write(
                            ErrorLog.internal(
                                InternalServerException(InfraResponseCode.REDIS_ERROR, errorMsg, e, mapOf("message" to message)),
                                actorName, mapOf("redisPubSubMessageId" to message.id)
                            )
                        )
                        // TODO: persistence unDeliveredMessage and retry later
                    }
                }
            }
        }
    )

    private suspend fun processKeyspaceNotification(data: RedisPubSub.KeyspaceNotification) {
        keyspaceNotificationBlocks.forEach { it(data) }
    }

    fun subscribeKeyspaceNotification(block: suspend (RedisPubSub.KeyspaceNotification) -> Unit) {
        keyspaceNotificationBlocks.add(block)
    }

    fun shutdown() {
        actor.close()
    }
}