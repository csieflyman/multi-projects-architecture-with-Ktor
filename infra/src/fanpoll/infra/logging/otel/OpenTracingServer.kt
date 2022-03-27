/*
 * Copyright (c) 2022. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.otel

import fanpoll.infra.base.extension.publicRemoteHost
import fanpoll.infra.logging.RequestAttributeKey
import fanpoll.infra.logging.logApi
import io.ktor.application.*
import io.ktor.http.Headers
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import kotlinx.coroutines.withContext
import mu.KotlinLogging

class OpenTracingServer(configuration: Configuration) {

    class Configuration {

        var filter: ((ApplicationCall) -> Boolean) = { false }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, OpenTracingServer> {

        override val key = AttributeKey<OpenTracingServer>("OpenTracingServer")

        private val logger = KotlinLogging.logger {}

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): OpenTracingServer {
            val configuration = Configuration().apply(configure)
            val feature = OpenTracingServer(configuration)

            val headerGetter = object : TextMapGetter<Headers> {

                override operator fun get(carrier: Headers?, key: String): String? = carrier?.get(key)

                override fun keys(carrier: Headers): Iterable<String> = carrier.names()
            }

            val tracingPhase = PipelinePhase("OpenTracingServer")
            pipeline.insertPhaseAfter(ApplicationCallPipeline.Monitoring, tracingPhase)

            pipeline.intercept(tracingPhase) {
                if (configuration.filter(call)) return@intercept

                val tracer = GlobalOpenTelemetry.getTracer("ktor.server")
                val extractedContext = GlobalOpenTelemetry.getPropagators().textMapPropagator
                    .extract(Context.current(), call.request.headers, headerGetter)
                val span = tracer.spanBuilder(call.request.logApi())
                    .setParent(extractedContext)
                    .setSpanKind(SpanKind.SERVER)
                    .setAttribute(SemanticAttributes.HTTP_METHOD, call.request.httpMethod.value)
                    .setAttribute(SemanticAttributes.HTTP_URL, call.request.uri)
                    .apply {
                        call.request.publicRemoteHost?.let { setAttribute(SemanticAttributes.HTTP_CLIENT_IP, it) }
                    }
                    .startSpan()
                call.attributes.put(RequestAttributeKey.TRACE_ID, span.spanContext.traceId)
                call.attributes.put(RequestAttributeKey.ID, span.spanContext.spanId)

                try {
                    withContext(span.asContextElement()) {
                        proceed()
                    }
                } catch (e: Throwable) {
                    span.setStatus(StatusCode.ERROR, "process request error")
                    span.recordException(e) // additionalAttributes
                    throw e
                } finally {
                    call.response.status()?.value?.let { span.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, it) }
                    span.end()
                }
            }
            return feature
        }
    }
}