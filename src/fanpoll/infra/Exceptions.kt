/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.infra

import fanpoll.infra.auth.MyPrincipal
import fanpoll.infra.controller.MyLocation
import fanpoll.infra.httpclient.api
import fanpoll.infra.httpclient.textBody
import fanpoll.infra.model.Entity
import fanpoll.infra.model.TenantEntity
import fanpoll.infra.utils.callerLogString
import io.konform.validation.Invalid
import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.features.BadRequestException
import io.ktor.features.MissingRequestParameterException
import io.ktor.features.ParameterConversionException
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.LocationRoutingException
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.json.JsonElement
import mu.KotlinLogging
import java.time.Instant

object ExceptionUtils {

    private val logger = KotlinLogging.logger {}

    @OptIn(KtorExperimentalAPI::class, KtorExperimentalLocationsAPI::class)
    fun wrapException(e: Throwable): BaseException = when (e) {
        is BaseException -> e
        is ParameterConversionException -> RequestException(ResponseCode.REQUEST_BAD_PATH_OR_QUERY, e.message, e)
        is MissingRequestParameterException -> RequestException(ResponseCode.REQUEST_BAD_PATH_OR_QUERY, e.message, e)
        is BadRequestException -> RequestException(ResponseCode.REQUEST_BAD_ALL, e.message, e)
        is LocationRoutingException -> RequestException(ResponseCode.REQUEST_BAD_PATH_OR_QUERY, e.message, e)
        is kotlinx.serialization.SerializationException -> RequestBodyException(e.message, e)
        else -> InternalServerErrorException(ResponseCode.UNEXPECTED_ERROR, cause = e)
    }

    fun getStackTrace(e: Throwable): String = org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e)

    fun writeLogToFile(e: Throwable, call: ApplicationCall? = null) {
        val ee = wrapException(e)
        val message = "${call?.callerLogString()} => "
        when (ee.code.codeType) {
            ResponseCodeType.SUCCESS -> logger.debug("$message${ee.message}")
            ResponseCodeType.USER_FAILED -> logger.info("$message${ee.message}")
            ResponseCodeType.CLIENT_ERROR -> logger.warn(message, ee)
            ResponseCodeType.SERVER_ERROR -> logger.error(message, ee)
        }
    }

    fun isWriteLog(call: ApplicationCall, e: BaseException): Boolean = e is InternalServerErrorException ||
            (call.principal<MyPrincipal>() != null && e.code.codeType.isError())
}

abstract class BaseException(
    val code: ResponseCode,
    message: String? = null,
    cause: Throwable? = null,
    val dataMap: Map<String, Any>? = null,
    var tenantId: String? = null
) :
    RuntimeException(
        "[${code.name}] ${message ?: ""}" +
                (if (dataMap != null && dataMap.isNotEmpty()) "dataMap = $dataMap" else "") +
                (if (tenantId != null) "tenantId = $tenantId" else ""), cause
    ) {

    val occurAt: Instant = Instant.now()
}

class RequestException : BaseException {

    var errorLocation: Invalid<MyLocation>? = null

    constructor(
        code: ResponseCode,
        message: String? = "",
        cause: Throwable? = null,
        dataMap: Map<String, Any>? = null,
        tenantId: String? = null
    ) : super(code, message, cause, dataMap, tenantId = tenantId)

    constructor(error: Invalid<MyLocation>, tenantId: String? = null) : super(
        ResponseCode.REQUEST_BAD_PATH_OR_QUERY,
        error.toString(),
        tenantId = tenantId
    ) {
        this.errorLocation = error
    }
}

class RequestBodyException : BaseException {

    private var errorBody: Invalid<*>? = null

    constructor(
        message: String? = "",
        cause: Throwable? = null,
        tenantId: String? = null
    ) : super(ResponseCode.REQUEST_BAD_BODY, message, cause, tenantId = tenantId)

    constructor (error: Invalid<*>, tenantId: String? = null) : super(
        ResponseCode.REQUEST_BAD_BODY,
        error.toString(),
        tenantId = tenantId
    ) {
        this.errorBody = error
    }
}

class EntityException(
    code: ResponseCode,
    message: String? = null,
    cause: Throwable? = null,
    dataMap: MutableMap<String, Any>? = null,
    val entityId: Any? = null,
    val entity: Entity<*>? = null
) :
    BaseException(
        code,
        "${message ?: ""} ${if (entityId != null) "entityId" else "${entity?.javaClass?.name}"} = ${entityId ?: entity}",
        cause,
        dataMap
    ) {
    init {
        require(!(entityId == null && entity == null)) { "Both entityId and entity can't be null" }
        if (entity is TenantEntity) {
            tenantId = entity.tenantId.value
        }
    }
}

class JsonDataException(message: String? = null, val data: JsonElement, val path: String) :
    BaseException(ResponseCode.ENTITY_JSON_INVALID, "${message ?: ""} path = $path, json data = $data")

class InternalServerErrorException(
    code: ResponseCode,
    message: String? = null,
    cause: Throwable? = null,
    dataMap: Map<String, Any>? = null,
    tenantId: String? = null
) :
    BaseException(code, message, cause, dataMap, tenantId)

class RemoteServiceException(
    code: ResponseCode,
    message: String? = null,
    cause: Throwable? = null,
    dataMap: Map<String, Any>? = null,
    tenantId: String? = null,
    val name: String, val api: String,
    val rspCode: String? = null,
    val reqBody: String? = null,
    val rspBody: String? = null
) :
    BaseException(
        code,
        "${message ?: ""} => name = [$name] api = [$api], rspCode = [$rspCode], reqBody = [$reqBody], rspBody = [$rspBody]",
        cause,
        dataMap,
        tenantId
    ) {

    constructor(
        response: HttpResponse, serviceId: String,
        responseCode: ResponseCode,
        message: String? = null
    ) : this(
        responseCode, message, null,
        name = serviceId,
        api = response.request.api,
        rspCode = "${response.status}",
        reqBody = response.request.textBody,
        rspBody = response.textBody
    )

    constructor(
        serviceId: String,
        responseCode: ResponseCode,
        message: String? = null,
        cause: Throwable? = null
    ) : this(
        responseCode, message, cause,
        name = serviceId,
        api = "unknown"
    )
}