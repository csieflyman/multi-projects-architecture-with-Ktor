/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.BaseException
import fanpoll.infra.ExceptionUtils
import fanpoll.infra.RemoteServiceException
import fanpoll.infra.auth.ATTRIBUTE_KEY_CLIENT_VERSION
import fanpoll.infra.auth.MyPrincipal
import fanpoll.infra.auth.PrincipalSource
import fanpoll.infra.auth.UserPrincipal
import fanpoll.infra.controller.publicRemoteHost
import fanpoll.infra.controller.receiveUTF8Text
import fanpoll.infra.controller.tenantId
import fanpoll.infra.logging.MyCallLogging.Feature.ATTRIBUTE_KEY_REQ_TIME
import fanpoll.infra.logging.MyCallLogging.Feature.ATTRIBUTE_KEY_TAG
import fanpoll.infra.login.LoginResultCode
import fanpoll.infra.utils.InstantSerializer
import fanpoll.infra.utils.UUIDSerializer
import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.features.callId
import io.ktor.features.toLogString
import io.ktor.request.path
import io.ktor.request.queryString
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.util.*

@Serializable
data class RequestLogDTO(
    val reqId: String,
    @Serializable(with = InstantSerializer::class) val reqTime: Instant,
    val api: String,
    val querystring: String? = null,
    val reqBody: String?,
    val project: String,
    val function: String,
    val tag: String? = null,
    val source: PrincipalSource,
    val tenantId: String? = null,
    val principal: String,
    val runAs: Boolean,
    val clientId: String? = null,
    val clientVersion: String? = null,
    val ip: String? = null,
    @Serializable(with = InstantSerializer::class) val rspTime: Instant,
    val reqMillis: Long,
    val rspStatus: Int,
    val rspBody: String?
) {

    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID()
}

@Serializable
data class ErrorLogDTO(
    @Serializable(with = InstantSerializer::class) val occurAt: Instant,
    val errorCode: String,
    val errorMsg: String? = null,
    val reqId: String? = null,
    @Serializable(with = InstantSerializer::class) val reqTime: Instant, // reqTime is required field for sorting in ElasticaSearch
    val api: String,
    val querystring: String? = null,
    val reqBody: String? = null,
    val project: String? = null,
    val function: String? = null,
    val tag: String? = null,
    val source: PrincipalSource,
    val tenantId: String? = null,
    val principal: String,
    val runAs: Boolean,
    val clientId: String? = null,
    val clientVersion: String? = null,
    val ip: String? = null,
    @Serializable(with = InstantSerializer::class) val rspTime: Instant? = null,
    val reqMillis: Long? = null,
    val serviceName: String? = null,
    val serviceApi: String? = null,
    val serviceRspCode: String? = null,
    val serviceReqBody: String? = null,
    val serviceRspBody: String? = null
) {

    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID()

    companion object {

        fun request(e: BaseException, call: ApplicationCall, loggingConfig: LoggingConfig): ErrorLogDTO {
            // ASSUMPTION => path segments size >= 3
            val pathSegments = call.request.path().split("/")
            //val project = pathSegments[1]
            val principal = call.principal<MyPrincipal>()!!
            val userPrincipal = call.principal<UserPrincipal>()
            val projectId = principal.source.projectId
            val function = pathSegments[2]
            val reqTime = call.attributes.getOrNull(ATTRIBUTE_KEY_REQ_TIME) ?: Instant.now()
            val rspTime = Instant.now()

            return ErrorLogDTO(
                e.occurAt,
                e.code.value,
                ExceptionUtils.getStackTrace(e),
                call.callId,
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
                (call.tenantId ?: e.tenantId)?.toString(),
                principal.id,
                userPrincipal?.runAs ?: false,
                userPrincipal?.clientId,
                call.attributes.getOrNull(ATTRIBUTE_KEY_CLIENT_VERSION),
                call.request.publicRemoteHost,
                rspTime,
                Duration.between(reqTime, rspTime ?: e.occurAt).toMillis(),
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
            api: String,
            project: String? = null,
            function: String? = null,
        ): ErrorLogDTO {
            return ErrorLogDTO(
                occurAt = e.occurAt,
                errorCode = e.code.value,
                errorMsg = ExceptionUtils.getStackTrace(e),
                reqTime = e.occurAt,
                api = api,
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

@Serializable
data class LoginLogDTO(
    @Serializable(with = InstantSerializer::class) val reqTime: Instant, // reqTime is required field for sorting in ElasticaSearch
    val resultCode: LoginResultCode,
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    val project: String,
    val source: PrincipalSource,
    val tenantId: String? = null,
    val clientId: String? = null,
    val clientVersion: String?,
    val ip: String? = null,
    val sid: String? = null
) {

    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID()
}