/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.utils

import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.ResponseCode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import mu.KotlinLogging
import java.util.concurrent.*

/**
reference:
(1) DB Connection Pool vs. Thread Pool Settings => https://venkatsadasivam.com/2008/12/28/connection-pool-vs-thread-pool/
(2) newFixedThreadPoolContext => https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/new-fixed-thread-pool-context.html
 */

object CoroutineUtils {

    private val logger = KotlinLogging.logger {}

    fun createDispatcher(name: String, config: DispatcherConfig): ExecutorCoroutineDispatcher {
        logger.info("init my coroutine $name dispatcher... $config")
        val factory = MyCoroutineThreadFactory(name)
        val executorService = if (config.fixedPoolSize != null)
            Executors.newFixedThreadPool(config.fixedPoolSize, factory)
        else
            ThreadPoolExecutor(
                config.minPoolSize!!, config.maxPoolSize!!,
                config.keepAliveTime!!, TimeUnit.SECONDS,
                LinkedBlockingQueue(), factory
            )
        return executorService.asCoroutineDispatcher()
    }

    private class MyCoroutineThreadFactory(private val dispatcherName: String) : ThreadFactory {

        private val backingThreadFactory: ThreadFactory = Executors.defaultThreadFactory()

        override fun newThread(r: Runnable): Thread {
            val t = backingThreadFactory.newThread(r)
            t.name = "MyCoroutine-$dispatcherName-${t.name}"
            return t
        }
    }

    fun closeDispatcher(dispatcherName: String, dispatcher: ExecutorCoroutineDispatcher) {
        logger.info("close $dispatcherName dispatcher...")
        try {
            dispatcher.close()
        } catch (e: Throwable) {
            throw InternalServerErrorException(
                ResponseCode.COROUTINE_ERROR,
                "could not close $dispatcherName dispatcher",
                e
            )
        }
        logger.info("dispatcher $dispatcherName closed")
    }

    fun closeChannel(channelName: String, channel: SendChannel<*>) {
        logger.info("close $channelName channel...")
        try {
            channel.close()
        } catch (e: Throwable) {
            throw InternalServerErrorException(ResponseCode.COROUTINE_ERROR, "could not close $channelName channel", e)
        }
        logger.info("channel $channelName closed")
    }

    fun <E> createActor(
        name: String, coroutines: Int,
        scope: CoroutineScope,
        block: suspend (E) -> Unit
    ): SendChannel<E> {
        require(coroutines > 0)

        val channel = Channel<E>(Channel.UNLIMITED)
        for (i in 1..coroutines) {
            scope.launch(CoroutineName("$name-$i")) {
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

        for (i in 1..coroutines) {
            scope.launch(CoroutineName("$name-$i")) {
                for (e in channel) {
                    logger.debug { coroutineContext }
                    block(e)
                }
            }
        }
    }
}

data class CoroutineConfig(val coroutines: Int, val dispatcher: DispatcherConfig?)

data class DispatcherConfig(
    val fixedPoolSize: Int? = null,
    val minPoolSize: Int? = null,
    val maxPoolSize: Int? = null,
    val keepAliveTime: Long? = null
) : MyConfig {
    override fun validate() {
        ConfigUtils.require(
            fixedPoolSize != null ||
                    (minPoolSize != null && maxPoolSize != null && keepAliveTime != null)
        ) {
            "either fixedPoolSize or minPoolSize, maxPoolSize, keepAliveTime should be configured"
        }
    }
}