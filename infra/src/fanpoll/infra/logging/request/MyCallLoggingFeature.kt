/*
 * Copyright (c) 2022. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.request

import fanpoll.infra.auth.ATTRIBUTE_KEY_CLIENT_VERSION
import fanpoll.infra.auth.HEADER_CLIENT_VERSION
import fanpoll.infra.auth.principal.MyPrincipal
import fanpoll.infra.logging.LoggingConfig
import fanpoll.infra.logging.RequestAttribute.PARENT_REQ_ID
import fanpoll.infra.logging.RequestAttribute.REQ_AT
import fanpoll.infra.logging.RequestAttribute.REQ_ID
import fanpoll.infra.logging.RequestAttribute.TRACE_ID
import fanpoll.infra.logging.writers.LogWriter
import io.ktor.application.*
import io.ktor.auth.principal
import io.ktor.features.CallLogging
import io.ktor.features.callId
import io.ktor.features.toLogString
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.response.ApplicationSendPipeline
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.koin.ktor.ext.get
import org.slf4j.MDC
import java.time.Instant
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Logs application lifecycle and call events.
 */
class MyCallLoggingFeature private constructor(
    private val monitor: ApplicationEvents,
    private val mdcEntries: List<MDCEntry>,
    private val config: RequestLogConfig,
    private val logWriter: LogWriter
) {

    private val logger = KotlinLogging.logger {}

    internal class MDCEntry(val name: String, val provider: (ApplicationCall) -> String?)

    /**
     * Configuration for [CallLogging] feature
     */
    public class Configuration {
        internal val mdcEntries = mutableListOf<MDCEntry>()

        /**
         * Put a diagnostic context value to [MDC] with the specified [name] and computed using [provider] function.
         * A value will be available in MDC only during [ApplicationCall] lifetime and will be removed after call
         * processing.
         */
        public fun mdc(name: String, provider: (ApplicationCall) -> String?) {
            mdcEntries.add(MDCEntry(name, provider))
        }
    }

    //private val starting: (Application) -> Unit = { logger.info("Application starting: $it") }
    //private val started: (Application) -> Unit = { logger.info("Application started: $it") }
    private val stopPreparing: (ApplicationEnvironment) -> Unit = { logger.info("Application stopPreparing...") }
    private val stopping: (Application) -> Unit = { logger.info("Application stopping...") }
    private var stopped: (Application) -> Unit = {}

    init {
        stopped = {
            logger.info("Application stopped")
            //monitor.unsubscribe(ApplicationStarting, starting)
            //monitor.unsubscribe(ApplicationStarted, started)
            monitor.unsubscribe(ApplicationStopPreparing, stopPreparing)
            monitor.unsubscribe(ApplicationStopping, stopping)
            monitor.unsubscribe(ApplicationStopped, stopped)
        }

        //monitor.subscribe(ApplicationStarting, starting)
        //monitor.subscribe(ApplicationStarted, started)
        monitor.subscribe(ApplicationStopPreparing, stopPreparing)
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
     * Installable feature for [MyCallLoggingFeature].
     */
    companion object Feature : ApplicationFeature<Application, Configuration, MyCallLoggingFeature> {

        val ATTRIBUTE_KEY_RSP_BODY = AttributeKey<String>("rspBody")

        private val afterRenderResponsePhase: PipelinePhase = PipelinePhase("AfterRenderResponse")

        override val key: AttributeKey<MyCallLoggingFeature> = AttributeKey("MyCallLogging")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): MyCallLoggingFeature {
            val loggingPhase = PipelinePhase("Logging")
            val configuration = Configuration().apply(configure)

            val loggingConfig = pipeline.get<LoggingConfig>()
            val requestConfig = loggingConfig.request

            val logWriter = pipeline.get<LogWriter>()
            val feature = MyCallLoggingFeature(
                pipeline.environment.monitor,
                configuration.mdcEntries.toList(),
                requestConfig, logWriter
            )

            pipeline.insertPhaseBefore(ApplicationCallPipeline.Monitoring, loggingPhase)

            if (feature.mdcEntries.isNotEmpty()) {
                pipeline.intercept(loggingPhase) {
                    beforeProceed(call)
                    withMDC(call) {
                        proceed()
                        feature.logSuccess(call)
                    }
                }
            } else {
                pipeline.intercept(loggingPhase) {
                    beforeProceed(call)
                    proceed()
                    feature.logSuccess(call)
                }
            }

            if (requestConfig.includeResponseBody) {
                pipeline.sendPipeline.insertPhaseAfter(ApplicationSendPipeline.Render, afterRenderResponsePhase)
                pipeline.sendPipeline.intercept(afterRenderResponsePhase) { message ->
                    if (message is TextContent) {
                        call.attributes.put(ATTRIBUTE_KEY_RSP_BODY, message.text)
                    }
                }
            }

            return feature
        }

        private fun beforeProceed(call: ApplicationCall) {
            call.attributes.put(TRACE_ID, UUID.fromString(call.callId))
            call.attributes.put(REQ_ID, call.request.header(REQ_ID.name)?.let { UUID.fromString(it) } ?: UUID.randomUUID())
            if (call.request.headers.contains(PARENT_REQ_ID.name))
                call.attributes.put(PARENT_REQ_ID, call.request.header(PARENT_REQ_ID.name)!!.let { UUID.fromString(it) })

            call.attributes.put(REQ_AT, Instant.now())

            if (call.request.headers.contains(HEADER_CLIENT_VERSION))
                call.attributes.put(ATTRIBUTE_KEY_CLIENT_VERSION, call.request.header(HEADER_CLIENT_VERSION)!!)
        }
    }

    private fun logSuccess(call: ApplicationCall) {
        if (config.enabled) {
            if (filter(call)) {
                logWriter.write(RequestLog(config, call))
            }
        }
    }

    private fun filter(call: ApplicationCall): Boolean = when {
        call.request.httpMethod == HttpMethod.Get && !config.includeGetMethod -> false
        config.excludePaths.any { call.request.path().startsWith(it) } -> false
        call.principal<MyPrincipal>() == null -> false // ASSUMPTION => only log authenticated request to avoid ddos attack
        else -> true
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

private fun defaultFormat(call: ApplicationCall): String = when (val status = call.response.status() ?: "Unhandled") {
    HttpStatusCode.Found -> "$status: ${call.request.toLogString()} -> ${call.response.headers[HttpHeaders.Location]}"
    else -> "$status: ${call.request.toLogString()}"
}