/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.async

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.*

object ThreadPoolUtils {

    private val logger = KotlinLogging.logger {}

    fun createThreadPoolExecutor(
        threadNamePrefix: String,
        config: ThreadPoolConfig,
        handler: Thread.UncaughtExceptionHandler? = null
    ): ExecutorService {
        logger.info("init thread pool $threadNamePrefix ... $config")
        val factory = DefaultThreadFactory(threadNamePrefix, handler)

        return if (config.isFixedThreadPool())
            Executors.newFixedThreadPool(config.fixedPoolSize!!, factory)
        else
            ThreadPoolExecutor(
                config.minPoolSize!!, config.maxPoolSize!!,
                config.keepAliveTime!!, TimeUnit.SECONDS,
                LinkedBlockingQueue(), factory
            )
    }

    private class DefaultThreadFactory(
        private val namePrefix: String,
        val handler: Thread.UncaughtExceptionHandler? = null
    ) : ThreadFactory {

        private val backingThreadFactory: ThreadFactory = Executors.defaultThreadFactory()

        override fun newThread(r: Runnable): Thread {
            val thread = backingThreadFactory.newThread(r)
            thread.name = "$namePrefix-${thread.name}"
            thread.isDaemon = true
            if (handler != null)
                thread.uncaughtExceptionHandler = handler
            else
                thread.setUncaughtExceptionHandler { t, e ->
                    logger.error("Thread Uncaught Error => ${t.name}", e)
                }
            return thread
        }
    }
}