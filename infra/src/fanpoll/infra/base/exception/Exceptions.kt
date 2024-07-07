/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.exception

import fanpoll.infra.base.datetime.DateTimeUtils
import fanpoll.infra.base.datetime.toMicros
import fanpoll.infra.base.entity.Entity
import fanpoll.infra.base.response.ResponseCode
import io.konform.validation.Invalid
import java.time.Duration
import java.time.Instant

abstract class BaseException(
    val code: ResponseCode,
    message: String? = code.name,
    cause: Throwable? = null,
    val dataMap: Map<String, Any>? = null
) : RuntimeException(
    "[${code.name}] ${message ?: ""}" + (if (!dataMap.isNullOrEmpty()) "dataMap = $dataMap" else ""), cause
) {
    val occurAt: Instant = Instant.now()
}

class RequestException : BaseException {

    val invalidResult: Invalid?

    constructor(
        code: ResponseCode,
        message: String? = null,
        cause: Throwable? = null,
        dataMap: Map<String, Any>? = null
    ) : super(code, message, cause, dataMap) {
        this.invalidResult = null
    }

    constructor(code: ResponseCode, invalidResult: Invalid) : super(code, invalidResult.toString()) {
        this.invalidResult = invalidResult
    }
}

class EntityException(
    code: ResponseCode,
    message: String? = null,
    cause: Throwable? = null,
    dataMap: MutableMap<String, Any>? = null,
    val entityId: Any? = null,
    val entity: Entity<*>? = null
) : BaseException(
    code,
    "${message ?: ""} ${if (entityId != null) "entityId" else "${entity?.javaClass?.name}"} = ${entityId ?: entity}",
    cause, dataMap
) {
    init {
        require(!(entityId == null && entity == null)) { "Both entityId and entity can't be null" }
    }
}

class InternalServerException(
    code: ResponseCode,
    message: String? = null,
    cause: Throwable? = null,
    dataMap: Map<String, Any>? = null
) : BaseException(code, message, cause, dataMap)

open class RemoteServiceException(
    code: ResponseCode,
    message: String? = null,
    cause: Throwable? = null,
    dataMap: Map<String, Any>? = null,
    val name: String,
    val api: String,
    val reqId: String,
    val reqAt: Instant,
    val reqBody: String? = null,
    val rspCode: String? = null,
    val rspBody: String? = null,
    val rspAt: Instant? = null,
    val duration: Duration? = null
) : BaseException(
    code,
    """${message ?: ""} => 
        name = [$name] 
        api = [$api], 
        reqId = [$reqId], 
        rspCode = [$rspCode], 
        reqAt = [${DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(reqAt)}], 
        rspAt = [${rspAt?.let { DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(it) }}], 
        duration = [${duration?.toMicros()}],
        reqBody = [$reqBody],
        rspBody = [$rspBody]
    """,
    cause, dataMap
)