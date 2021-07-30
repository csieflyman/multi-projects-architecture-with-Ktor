/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.exception

import fanpoll.infra.base.entity.Entity
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.tenant.TenantEntity
import fanpoll.infra.base.tenant.TenantId
import io.konform.validation.Invalid
import java.time.Instant

abstract class BaseException(
    val code: ResponseCode,
    message: String? = null,
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
        message: String? = "",
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
    val name: String, val api: String,
    val rspCode: String? = null,
    val reqBody: String? = null,
    val rspBody: String? = null
) : BaseException(
    code,
    "${message ?: ""} => name = [$name] api = [$api], rspCode = [$rspCode], reqBody = [$reqBody], rspBody = [$rspBody]",
    cause, dataMap, tenantId
)