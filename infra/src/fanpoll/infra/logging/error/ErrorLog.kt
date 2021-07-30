/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.error

import fanpoll.infra.auth.ATTRIBUTE_KEY_CLIENT_VERSION
import fanpoll.infra.auth.principal.MyPrincipal
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.base.exception.BaseException
import fanpoll.infra.base.exception.ExceptionUtils
import fanpoll.infra.base.exception.RemoteServiceException
import fanpoll.infra.base.extension.bodyString
import fanpoll.infra.base.extension.publicRemoteHost
import fanpoll.infra.base.json.InstantSerializer
import fanpoll.infra.base.json.UUIDSerializer
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.tenant.TenantId
import fanpoll.infra.base.tenant.tenantId
import fanpoll.infra.logging.LogLevel
import fanpoll.infra.logging.LogMessage
import fanpoll.infra.logging.request.MyCallLoggingFeature.Feature.ATTRIBUTE_KEY_REQ_AT
import fanpoll.infra.logging.request.MyCallLoggingFeature.Feature.ATTRIBUTE_KEY_TAG
import fanpoll.infra.logging.toHeadersLogString
import fanpoll.infra.logging.toQueryStringLogString
import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.features.callId
import io.ktor.features.toLogString
import io.ktor.request.path
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.util.*

@Serializable
data class ErrorLog(
    @Serializable(with = InstantSerializer::class) override val occurAt: Instant,
    val errorCode: ResponseCode,
    val errorMsg: String? = null,
    val data: String? = null,
    val reqId: String? = null,
    @Serializable(with = InstantSerializer::class) val reqAt: Instant,
    val api: String? = null,
    val headers: String? = null,
    val querystring: String? = null,
    val reqBody: String? = null,
    val project: String? = null,
    val function: String? = null,
    val tag: String? = null,
    val source: PrincipalSource,
    val tenantId: TenantId? = null,
    val principal: String,
    val runAs: Boolean,
    val clientId: String? = null,
    val clientVersion: String? = null,
    val ip: String? = null,
    @Serializable(with = InstantSerializer::class) val rspAt: Instant? = null,
    val rspTime: Long? = null,
    val serviceName: String? = null,
    val serviceApi: String? = null,
    val serviceRspCode: String? = null,
    val serviceReqBody: String? = null,
    val serviceRspBody: String? = null
) : LogMessage() {

    @Serializable(with = UUIDSerializer::class)
    override val id: UUID = UUID.randomUUID()

    override val logType: String = LOG_TYPE

    override val logLevel: LogLevel = Log_Level

    companion object {

        const val LOG_TYPE = "error"
        private val Log_Level = LogLevel.ERROR

        fun request(e: BaseException, call: ApplicationCall): ErrorLog {
            // ASSUMPTION => path segments size >= 3
            val pathSegments = call.request.path().split("/")
            //val project = pathSegments[1]
            val function = pathSegments[2]

            val principal = call.principal<MyPrincipal>()!!
            val userPrincipal = call.principal<UserPrincipal>()
            val reqAt = call.attributes.getOrNull(ATTRIBUTE_KEY_REQ_AT) ?: Instant.now()
            val rspAt = Instant.now()

            return ErrorLog(
                e.occurAt,
                e.code,
                ExceptionUtils.getStackTrace(e),
                e.dataMap?.toString(),
                call.callId,
                reqAt,
                call.request.toLogString(),
                call.request.toHeadersLogString(),
                call.request.toQueryStringLogString(),
                call.bodyString(),
                principal.source.projectId,
                function,
                call.attributes.getOrNull(ATTRIBUTE_KEY_TAG),
                principal.source,
                call.tenantId ?: e.tenantId,
                principal.id,
                userPrincipal?.runAs ?: false,
                userPrincipal?.clientId,
                call.attributes.getOrNull(ATTRIBUTE_KEY_CLIENT_VERSION),
                call.request.publicRemoteHost,
                rspAt,
                Duration.between(reqAt, rspAt ?: e.occurAt).toMillis(),
                // LIMITATION => ktor StatusPages feature exception run at ApplicationCallPipeline before CallLogging feature run at ApplicationSendPipeline.Render
                (e as? RemoteServiceException)?.name,
                (e as? RemoteServiceException)?.api,
                (e as? RemoteServiceException)?.rspCode,
                (e as? RemoteServiceException)?.reqBody,
                (e as? RemoteServiceException)?.rspBody
            )
        }

        fun internal(
            e: BaseException,
            internalServiceName: String,
            tag: String,
            project: String? = null,
            function: String? = null,
        ): ErrorLog {
            return ErrorLog(
                occurAt = e.occurAt,
                errorCode = e.code,
                errorMsg = ExceptionUtils.getStackTrace(e),
                data = e.dataMap?.toString(),
                reqAt = e.occurAt,
                tag = tag,
                project = project,
                function = function,
                source = PrincipalSource.System,
                principal = internalServiceName,
                runAs = false,
                serviceName = (e as? RemoteServiceException)?.name,
                serviceApi = (e as? RemoteServiceException)?.api,
                serviceRspCode = (e as? RemoteServiceException)?.rspCode,
                serviceReqBody = (e as? RemoteServiceException)?.reqBody,
                serviceRspBody = (e as? RemoteServiceException)?.rspBody
            )
        }
    }
}

