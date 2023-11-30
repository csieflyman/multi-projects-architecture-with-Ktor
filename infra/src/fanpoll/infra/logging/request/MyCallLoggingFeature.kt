/*
 * Copyright (c) 2022. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.request

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext

/**
 * Logs application lifecycle and call events.
 */
public class MyCallLoggingPlugin private constructor(
    private val monitor: ApplicationEvents,
    private val filters: List<(ApplicationCall) -> Boolean>,
    private val mdcEntries: List<MDCEntry>,
    private val writeLog: (ApplicationCall) -> Unit
) {

    private val logger = KotlinLogging.logger {}

    internal class MDCEntry(val name: String, val provider: (ApplicationCall) -> String?)

    /**
     * Configuration for [CallLogging] plugin
     */
    public class Configuration {
        internal val filters = mutableListOf<(ApplicationCall) -> Boolean>()
        internal val mdcEntries = mutableListOf<MDCEntry>()

        internal lateinit var writeLog: (ApplicationCall) -> Unit

        /**
         * Log messages for calls matching a [predicate]
         */
        public fun filter(predicate: (ApplicationCall) -> Boolean) {
            filters.add(predicate)
        }

        /**
         * Put a diagnostic context value to [MDC] with the specified [name] and computed using [provider] function.
         * A value will be available in MDC only during [ApplicationCall] lifetime and will be removed after call
         * processing.
         */
        public fun mdc(name: String, provider: (ApplicationCall) -> String?) {
            mdcEntries.add(MDCEntry(name, provider))
        }
    }

    private val starting: (Application) -> Unit = { logger.info { "Application starting: $it" } }
    private val started: (Application) -> Unit = { logger.info { "Application started: $it" } }
    private val stopping: (Application) -> Unit = { logger.info { "Application stopping: $it" } }
    private var stopped: (Application) -> Unit = {}

    init {
        stopped = {
            logger.info { "Application stopped: $it" }
            monitor.unsubscribe(ApplicationStarting, starting)
            monitor.unsubscribe(ApplicationStarted, started)
            monitor.unsubscribe(ApplicationStopping, stopping)
            monitor.unsubscribe(ApplicationStopped, stopped)
        }

        monitor.subscribe(ApplicationStarting, starting)
        monitor.subscribe(ApplicationStarted, started)
        monitor.subscribe(ApplicationStopping, stopping)
        monitor.subscribe(ApplicationStopped, stopped)
    }

    internal fun setupMdc(call: ApplicationCall): Map<String, String> {
        val result = HashMap<String, String>()

        mdcEntries.forEach { entry ->
            entry.provider(call)?.let { mdcValue ->
                result[entry.name] = mdcValue
            }
        }

        return result
    }

    internal fun cleanupMdc() {
        mdcEntries.forEach {
            MDC.remove(it.name)
        }
    }

    /**
     * Installable plugin for [CallLogging].
     */
    public companion object Plugin : BaseApplicationPlugin<Application, Configuration, MyCallLoggingPlugin> {
        override val key: AttributeKey<MyCallLoggingPlugin> = AttributeKey("MyCallLogging")
        override fun install(pipeline: Application, configure: Configuration.() -> Unit): MyCallLoggingPlugin {
            val loggingPhase = PipelinePhase("Logging")
            val configuration = Configuration().apply(configure)

            val plugin = MyCallLoggingPlugin(
                pipeline.environment.monitor,
                configuration.filters.toList(),
                configuration.mdcEntries.toList(),
                configuration.writeLog
            )

            pipeline.insertPhaseBefore(ApplicationCallPipeline.Monitoring, loggingPhase)

            if (plugin.mdcEntries.isNotEmpty()) {
                pipeline.intercept(loggingPhase) {
                    withMDC(call) {
                        proceed()
                        plugin.logSuccess(call)
                    }
                }
            } else {
                pipeline.intercept(loggingPhase) {
                    proceed()
                    plugin.logSuccess(call)
                }
            }

            return plugin
        }
    }

    private fun logSuccess(call: ApplicationCall) {
        if (filters.isEmpty() || filters.any { it(call) }) {
            writeLog(call)
        }
    }
}

/**
 * Invoke suspend [block] with a context having MDC configured.
 */
private suspend inline fun withMDC(call: ApplicationCall, crossinline block: suspend () -> Unit) {
    val plugin = call.application.pluginOrNull(MyCallLoggingPlugin) ?: return block()

    withContext(MDCSurvivalElement(plugin.setupMdc(call))) {
        try {
            block()
        } finally {
            plugin.cleanupMdc()
        }
    }
}

/**
 * Generates a string representing this [ApplicationRequest] suitable for logging
 */
public fun ApplicationRequest.toLogString(): String = "${httpMethod.value} - ${path()}"

private class MDCSurvivalElement(mdc: Map<String, String>) : ThreadContextElement<Map<String, String>> {
    override val key: CoroutineContext.Key<*> get() = Key

    private val snapshot = copyMDC() + mdc

    override fun restoreThreadContext(context: CoroutineContext, oldState: Map<String, String>) {
        putMDC(oldState)
    }

    override fun updateThreadContext(context: CoroutineContext): Map<String, String> {
        val mdcCopy = copyMDC()
        putMDC(snapshot)
        return mdcCopy
    }

    private fun copyMDC() = MDC.getCopyOfContextMap()?.toMap() ?: emptyMap()

    private fun putMDC(oldState: Map<String, String>) {
        MDC.clear()
        oldState.entries.forEach { (k, v) ->
            MDC.put(k, v)
        }
    }

    private object Key : CoroutineContext.Key<MDCSurvivalElement>
}
