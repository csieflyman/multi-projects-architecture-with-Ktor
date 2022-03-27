/*
 * Copyright (c) 2022. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.request

import io.ktor.application.*
import io.ktor.features.CallLogging
import io.ktor.request.ApplicationRequest
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.slf4j.MDC
import java.time.Instant
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Logs application lifecycle and call events.
 */
public class MyCallLoggingFeature private constructor(
    private val monitor: ApplicationEvents,
    private val filters: List<(ApplicationCall) -> Boolean>,
    private val mdcEntries: List<MDCEntry>,
    private val writeLog: (ApplicationCall) -> Unit
) {

    private val logger = KotlinLogging.logger {}

    internal class MDCEntry(val name: String, val provider: (ApplicationCall) -> String?)

    /**
     * Configuration for [CallLogging] feature
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

    private val starting: (Application) -> Unit = { logger.info("Application starting: $it") }
    private val started: (Application) -> Unit = { logger.info("Application started: $it") }
    private val stopping: (Application) -> Unit = { logger.info("Application stopping: $it") }
    private var stopped: (Application) -> Unit = {}

    init {
        stopped = {
            logger.info("Application stopped: $it")
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
     * Installable feature for [CallLogging].
     */
    public companion object Feature : ApplicationFeature<Application, Configuration, MyCallLoggingFeature> {
        override val key: AttributeKey<MyCallLoggingFeature> = AttributeKey("MyCallLogging")
        override fun install(pipeline: Application, configure: Configuration.() -> Unit): MyCallLoggingFeature {
            val loggingPhase = PipelinePhase("Logging")
            val configuration = Configuration().apply(configure)

            val feature = MyCallLoggingFeature(
                pipeline.environment.monitor,
                configuration.filters.toList(),
                configuration.mdcEntries.toList(),
                configuration.writeLog
            )

            pipeline.insertPhaseBefore(ApplicationCallPipeline.Monitoring, loggingPhase)

            if (feature.mdcEntries.isNotEmpty()) {
                pipeline.intercept(loggingPhase) {
                    withMDC(call) {
                        proceed()
                        feature.logSuccess(call)
                    }
                }
            } else {
                pipeline.intercept(loggingPhase) {
                    proceed()
                    feature.logSuccess(call)
                }
            }

            return feature
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
    val feature = call.application.featureOrNull(MyCallLoggingFeature) ?: return block()

    withContext(MDCSurvivalElement(feature.setupMdc(call))) {
        try {
            block()
        } finally {
            feature.cleanupMdc()
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
