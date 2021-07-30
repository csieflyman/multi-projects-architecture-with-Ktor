/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.async

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.ResponseCode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import mu.KotlinLogging

/**
reference:
(1) DB Connection Pool vs. Thread Pool Settings => https://venkatsadasivam.com/2008/12/28/connection-pool-vs-thread-pool/
(2) newFixedThreadPoolContext => https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/new-fixed-thread-pool-context.html
 */

object CoroutineUtils {

    private val logger = KotlinLogging.logger {}

    fun createDispatcher(name: String, config: ThreadPoolConfig): ExecutorCoroutineDispatcher {
        return ThreadPoolUtils.createThreadPoolExecutor(name, config).asCoroutineDispatcher()
    }

    fun closeDispatcher(dispatcherName: String, dispatcher: ExecutorCoroutineDispatcher) {
        logger.info("close $dispatcherName dispatcher...")
        try {
            dispatcher.close()
        } catch (e: Throwable) {
            throw InternalServerException(ResponseCode.COROUTINE_ERROR, "could not close $dispatcherName dispatcher", e)
        }
        logger.info("dispatcher $dispatcherName closed")
    }

    fun closeChannel(channelName: String, channel: SendChannel<*>) {
        logger.info("close $channelName channel...")
        try {
            channel.close()
        } catch (e: Throwable) {
            throw InternalServerException(ResponseCode.COROUTINE_ERROR, "could not close $channelName channel", e)
        }
        logger.info("channel $channelName closed")
    }

    fun <E> createActor(
        name: String, capacity: Int, coroutines: Int,
        scope: CoroutineScope,
        block: suspend (E) -> Unit
    ): SendChannel<E> {
        require(coroutines > 0)

        val channel = Channel<E>(capacity)
        repeat(coroutines) {
            scope.launch(CoroutineName("$name-(${it + 1})")) {
                for (e in channel) {
                    logger.debug { coroutineContext }
                    block(e)
                }
            }
        }
        return channel
    }

    fun <E> createConsumer(
        name: String, coroutines: Int,
        scope: CoroutineScope,
        block: suspend (E) -> Unit,
        channel: ReceiveChannel<E>
    ) {
        require(coroutines > 0)

        repeat(coroutines) {
            scope.launch(CoroutineName("$name-(${it + 1})")) {
                for (e in channel) {
                    logger.debug { coroutineContext }
                    block(e)
                }
            }
        }
    }
}