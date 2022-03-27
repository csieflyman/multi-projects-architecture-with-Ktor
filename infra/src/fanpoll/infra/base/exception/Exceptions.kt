/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.exception

import fanpoll.infra.base.entity.Entity
import fanpoll.infra.base.extension.toMicros
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.tenant.TenantEntity
import fanpoll.infra.base.tenant.TenantId
import fanpoll.infra.base.util.DateTimeUtils
import io.konform.validation.Invalid
import java.time.Duration
import java.time.Instant

abstract class BaseException(
    val code: ResponseCode,
    message: String? = code.name,
    cause: Throwable? = null,
    val dataMap: Map<String, Any>? = null,
    var tenantId: TenantId? = null
) : RuntimeException(
    "[${code.name}] ${message ?: ""}" +
            (if (dataMap != null && dataMap.isNotEmpty()) "dataMap = $dataMap" else "") +
            (if (tenantId != null) "tenantId = $tenantId" else ""), cause
) {
    val occurAt: Instant = Instant.now()
}

class RequestException : BaseException {

    val invalidResult: Invalid<*>?

    constructor(
        code: ResponseCode,
        message: String? = null,
        cause: Throwable? = null,
        dataMap: Map<String, Any>? = null,
        tenantId: TenantId? = null
    ) : super(code, message, cause, dataMap, tenantId) {
        this.invalidResult = null
    }

    constructor(code: ResponseCode, invalidResult: Invalid<*>) : super(code, invalidResult.toString()) {
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
        if (entity is TenantEntity) {
            tenantId = entity.tenantId
        }
    }
}

class InternalServerException(
    code: ResponseCode,
    message: String? = null,
    cause: Throwable? = null,
    dataMap: Map<String, Any>? = null,
    tenantId: TenantId? = null
) : BaseException(code, message, cause, dataMap, tenantId)

open class RemoteServiceException(
    code: ResponseCode,
    message: String? = null,
    cause: Throwable? = null,
    dataMap: Map<String, Any>? = null,
    tenantId: TenantId? = null,
    val name: String,
    val api: String,
    val reqId: String?,
    val reqBody: String?,
    val reqAt: Instant?,
    val rspCode: String?,
    val rspBody: String?,
    val rspAt: Instant?,
    val duration: Duration?
) : BaseException(
    code,
    """${message ?: ""} => 
        name = [$name] 
        api = [$api], 
        reqId = [$reqId], 
        rspCode = [$rspCode], 
        reqAt = [${reqAt?.let { DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(it) }}], 
        rspAt = [${rspAt?.let { DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(it) }}], 
        duration = [${duration?.toMicros()}],
        reqBody = [$reqBody],
        rspBody = [$rspBody]
    """,
    cause, dataMap, tenantId
)