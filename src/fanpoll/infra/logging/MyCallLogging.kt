/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.auth.ATTRIBUTE_KEY_CLIENT_VERSION
import fanpoll.infra.auth.HEADER_CLIENT_VERSION
import fanpoll.infra.auth.MyPrincipal
import fanpoll.infra.auth.UserPrincipal
import fanpoll.infra.controller.publicRemoteHost
import fanpoll.infra.controller.receiveUTF8Text
import fanpoll.infra.controller.tenantId
import io.ktor.application.*
import io.ktor.auth.principal
import io.ktor.features.callId
import io.ktor.features.toLogString
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.queryString
import io.ktor.response.ApplicationSendPipeline
import io.ktor.util.AttributeKey
import io.ktor.util.InternalAPI
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.slf4j.MDC
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/**
 * Logs application lifecycle and call events.
 */
class MyCallLogging private constructor(
    private val monitor: ApplicationEvents,
    private val mdcEntries: List<MDCEntry>,
    private val loggingConfig: LoggingConfig,
) {

    private val logger = KotlinLogging.logger {}

    internal class MDCEntry(val name: String, val provider: (ApplicationCall) -> String?)

    /**
     * Configuration for [MyCallLogging] feature
     */
    class Configuration {
        internal val mdcEntries = mutableListOf<MDCEntry>()
        internal lateinit var loggingConfig: LoggingConfig

        /**
         * Put a diagnostic context value to [MDC] with the specified [name] and computed using [provider] function.
         * A value will be available in MDC only during [ApplicationCall] lifetime and will be removed after call
         * processing.
         */
        fun mdc(name: String, provider: (ApplicationCall) -> String?) {
            mdcEntries.add(MDCEntry(name, provider))
        }

        fun config(config: LoggingConfig) {
            this.loggingConfig = config
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
     * Installable feature for [MyCallLogging].
     */
    companion object Feature : ApplicationFeature<Application, Configuration, MyCallLogging> {

        val ATTRIBUTE_KEY_REQ_TIME = AttributeKey<Instant>("reqTime")
        val ATTRIBUTE_KEY_RSP_BODY = AttributeKey<String>("rspBody")
        val ATTRIBUTE_KEY_TAG = AttributeKey<String>("tag")

        private val afterRenderResponsePhase: PipelinePhase = PipelinePhase("AfterRenderResponse")

        override val key: AttributeKey<MyCallLogging> = AttributeKey("MyCallLogging")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): MyCallLogging {
            val loggingPhase = PipelinePhase("Logging")
            val configuration = Configuration().apply(configure)
            val feature = MyCallLogging(
                pipeline.environment.monitor,
                configuration.mdcEntries.toList(),
                configuration.loggingConfig,
            )

            pipeline.insertPhaseBefore(ApplicationCallPipeline.Monitoring, loggingPhase)

            if (feature.mdcEntries.isNotEmpty()) {
                pipeline.intercept(loggingPhase) {
                    withMDC {
                        putAttributes(call)
                        proceed()
                        feature.logSuccess(call)
                    }
                }
            } else {
                pipeline.intercept(loggingPhase) {
                    putAttributes(call)
                    proceed()
                    feature.logSuccess(call)
                }
            }

            if (feature.loggingConfig.requestLog.includeResponseBody) {
                pipeline.sendPipeline.insertPhaseAfter(ApplicationSendPipeline.Render, afterRenderResponsePhase)
                pipeline.sendPipeline.intercept(afterRenderResponsePhase) { message ->
                    if (message is TextContent) {
                        val responseBody = if (configuration.loggingConfig.responseBodySensitiveDataFilter != null)
                            configuration.loggingConfig.responseBodySensitiveDataFilter!!(call, message.text)
                        else message.text
                        if (responseBody != null)
                            call.attributes.put(ATTRIBUTE_KEY_RSP_BODY, responseBody)
                    }
                }
            }

            return feature
        }

        private fun putAttributes(call: ApplicationCall) {
            if (!call.attributes.contains(ATTRIBUTE_KEY_REQ_TIME))
                call.attributes.put(ATTRIBUTE_KEY_REQ_TIME, Instant.now())
            if (call.request.headers.contains(HEADER_CLIENT_VERSION))
                call.attributes.put(ATTRIBUTE_KEY_CLIENT_VERSION, call.request.header(HEADER_CLIENT_VERSION)!!)
        }
    }


    @Suppress("KDocMissingDocumentation")
    @InternalAPI
    object Internals {
        @InternalAPI
        suspend fun <C : PipelineContext<*, ApplicationCall>> C.withMDCBlock(block: suspend C.() -> Unit) {
            withMDC(block)
        }
    }

    private fun logSuccess(call: ApplicationCall) {
        if (loggingConfig.requestLogEnabled) {
            if (filter(call)) {
                LogManager.writeAsync(buildMessage(call))
            }
        }
    }

    private fun filter(call: ApplicationCall): Boolean = when {
        call.request.httpMethod == HttpMethod.Get && !loggingConfig.requestLog.includeGetMethod -> false
        call.request.httpMethod != HttpMethod.Get && !RequestLogConfig.includeHttpMethods.contains(call.request.httpMethod) -> false
        loggingConfig.requestLog.excludePaths.any { call.request.path().startsWith(it) } -> false
        call.principal<MyPrincipal>() == null -> false // ASSUMPTION => only log authenticated request to avoid ddos attack
        else -> true
    }

    private fun buildMessage(call: ApplicationCall): LogMessage {
        // ASSUMPTION => path segments size >= 3
        val pathSegments = call.request.path().split("/")
        //val project = pathSegments[1]
        val function = pathSegments[2]

        val principal = call.principal<MyPrincipal>()!!
        val userPrincipal = call.principal<UserPrincipal>()
        val projectId = principal.source.projectId
        val reqTime = call.attributes.getOrNull(ATTRIBUTE_KEY_REQ_TIME) ?: Instant.now()
        val rspTime = Instant.now()
        val reqMillis: Long = Duration.between(reqTime, rspTime).toMillis()
        return LogMessage(
            LogType.REQUEST, RequestLogDTO(
                call.callId!!,
                reqTime,
                call.request.toLogString(),
                call.request.queryString().let { if (it.isEmpty()) null else it },
                if (loggingConfig.requestBodySensitiveDataFilter != null)
                    loggingConfig.requestBodySensitiveDataFilter!!(call)
                else runBlocking { call.receiveUTF8Text() },
                projectId,
                function,
                call.attributes.getOrNull(ATTRIBUTE_KEY_TAG),
                principal.source,
                call.tenantId?.value,
                principal.id,
                userPrincipal?.runAs ?: false,
                userPrincipal?.clientId,
                call.attributes.getOrNull(ATTRIBUTE_KEY_CLIENT_VERSION),
                call.request.publicRemoteHost,
                rspTime,
                reqMillis,
                call.response.status()!!.value,
                call.attributes.getOrNull(ATTRIBUTE_KEY_RSP_BODY)
            )
        )
    }
}

/**
 * Invoke suspend [block] with a context having MDC configured.
 */
private suspend inline fun <C : PipelineContext<*, ApplicationCall>> C.withMDC(crossinline block: suspend C.() -> Unit) {
    val call = call
    val feature = call.application.featureOrNull(MyCallLogging) ?: return block()

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