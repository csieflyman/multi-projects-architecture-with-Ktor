/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.async

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel

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
        logger.info { "close $dispatcherName dispatcher..." }
        try {
            dispatcher.close()
        } catch (e: Throwable) {
            throw InternalServerException(InfraResponseCode.COROUTINE_ERROR, "could not close $dispatcherName dispatcher", e)
        }
        logger.info { "dispatcher $dispatcherName closed" }
    }

    fun closeChannel(channelName: String, channel: SendChannel<*>) {
        logger.info { "close $channelName channel..." }
        try {
            channel.close()
        } catch (e: Throwable) {
            throw InternalServerException(InfraResponseCode.COROUTINE_ERROR, "could not close $channelName channel", e)
        }
        logger.info { "channel $channelName closed" }
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

    fun <E> createActor(
        name: String, capacity: Int, coroutines: Int,
        scope: CoroutineScope,
        block: suspend (List<E>) -> Unit,
        sizeLimit: Int = 1,
        timeMsLimit: Int? = null
    ): SendChannel<E> {
        require(coroutines > 0)
        require(sizeLimit >= 1)

        val channel = Channel<E>(capacity)
        repeat(coroutines) {
            scope.launch(CoroutineName("$name-(${it + 1})")) {
                val unProcessItems = mutableListOf<E>()
                var nextProcessTimeMs = timeMsLimit?.plus(System.currentTimeMillis())
                for (e in channel) {
                    logger.debug { coroutineContext }
                    unProcessItems.add(e)
                    if (unProcessItems.size >= sizeLimit ||
                        (nextProcessTimeMs != null && System.currentTimeMillis() >= nextProcessTimeMs)
                    ) {
                        block(unProcessItems)
                        unProcessItems.clear()
                        if (timeMsLimit != null)
                            nextProcessTimeMs = System.currentTimeMillis() + timeMsLimit
                    }
                }
            }
        }
        return channel
    }
}