/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.async

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.util.IdentifiableObject
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.writers.LogWriter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.trySendBlocking

class CoroutineActor<T : IdentifiableObject<*>>(
    val name: String,
    capacity: Int,
    coroutineActorConfig: CoroutineActorConfig,
    defaultCoroutineDispatcher: CoroutineDispatcher,
    processBlock: suspend (T) -> Unit,
    produceBlock: (CoroutineScope.(SendChannel<T>) -> Unit)? = null,
    private val logWriter: LogWriter? = null
) {

    private val logger = KotlinLogging.logger {}

    private val dispatcher: CoroutineDispatcher
    private val channel: SendChannel<T>
    private val coroutineScope: CoroutineScope

    private val isUnlimited = capacity == Channel.UNLIMITED
    private val isProducer = produceBlock != null

    suspend fun send(message: T) {
        require(!isProducer)
        channel.send(message)
    }

    suspend fun sendToUnlimitedChannel(message: T, errorCode: ResponseCode) {
        require(!isProducer)
        require(isUnlimited)

        val result = channel.trySendBlocking(message)
        if (result.isFailure) {
            val e = result.exceptionOrNull()
            lateinit var errorMsg: String
            if (result.isClosed) {
                errorMsg = "$name is closed"
                logger.warn { "$errorMsg => $message" }
            } else {
                errorMsg = "$name unexpected error" // we never call channel.cancel()
                logger.error(e) { "$errorMsg => $message" }
            }
            if (errorCode != InfraResponseCode.LOG_ERROR) {
                logWriter?.write(
                    ErrorLog.internal(
                        InternalServerException(errorCode, errorMsg, e, mapOf("message" to message)),
                        name, mapOf("actorMessageId" to message.id.toString())
                    )
                )
                // TODO: persistence unDeliveredMessage and retry later
            }
        }
    }

    init {
        logger.info { "========== init coroutine actor $name ... ==========" }
        try {
            dispatcher = if (coroutineActorConfig.dispatcher != null)
                CoroutineUtils.createDispatcher(name, coroutineActorConfig.dispatcher)
            else defaultCoroutineDispatcher

            // Note: block should catch all exceptions in most cases
            val exceptionHandler = CoroutineExceptionHandler { ctx, e ->
                logger.error(e) { "coroutine actor uncaught exception => $ctx" }
            }
            val context = dispatcher + exceptionHandler
            coroutineScope = CoroutineScope(context)
            channel = CoroutineUtils.createActor(
                name, capacity, coroutineActorConfig.coroutines,
                coroutineScope, processBlock
            )

            produceBlock?.invoke(coroutineScope, channel)
        } catch (e: Throwable) {
            throw InternalServerException(InfraResponseCode.LOG_ERROR, "fail to init coroutine actor $name", e)
        }
        logger.info { "========== init coroutine actor $name completed ==========" }
    }

    // call channel.close() to wait task completed when shutdown server (don't call channel.cancel())
    fun close() {
        logger.info { "coroutine actor $name close ..." }
        CoroutineUtils.closeChannel(name, channel)
        coroutineScope.cancel(name)
        if (dispatcher is ExecutorCoroutineDispatcher) {
            CoroutineUtils.closeDispatcher(name, dispatcher)
        }
        logger.info { "coroutine actor $name closed" }
    }
}