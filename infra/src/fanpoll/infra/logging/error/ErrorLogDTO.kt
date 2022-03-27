/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.error

import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.json.DurationMicroSerializer
import fanpoll.infra.base.json.InstantSerializer
import fanpoll.infra.base.json.UUIDSerializer
import fanpoll.infra.base.response.ResponseCodeType
import fanpoll.infra.base.tenant.TenantId
import fanpoll.infra.database.util.ResultRowDTOMapper
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.util.*

@Serializable
data class ErrorLogDTO(
    @JvmField @Serializable(with = UUIDSerializer::class) val id: UUID
) : EntityDTO<UUID> {

    @Serializable(with = InstantSerializer::class)
    var occurAt: Instant? = null
    var errorCode: String? = null
    var errorCodeType: ResponseCodeType? = null
    var errorMsg: String? = null
    var stackTrace: String? = null
    var extras: String? = null

    var project: String? = null
    var function: String? = null
    var source: PrincipalSource? = null
    var tenantId: TenantId? = null
    var principalId: String? = null
    var tags: String? = null

    var userType: UserType? = null

    @Serializable(with = UUIDSerializer::class)
    var userId: UUID? = null
    var runAs: Boolean? = null

    var traceId: String? = null
    var reqId: String? = null

    @Serializable(with = InstantSerializer::class)
    var reqAt: Instant? = null
    var api: String? = null
    var headers: String? = null
    var querystring: String? = null
    var reqBody: String? = null
    var ip: String? = null
    var clientId: String? = null
    var clientVersion: String? = null

    @Serializable(with = InstantSerializer::class)
    var rspAt: Instant? = null
    var rspStatus: Int? = null
    var rspBody: String? = null

    @Serializable(with = DurationMicroSerializer::class)
    var duration: Duration? = null

    var serviceName: String? = null
    var serviceApi: String? = null
    var serviceReqId: String? = null

    @Serializable(with = InstantSerializer::class)
    var serviceReqAt: Instant? = null
    var serviceReqBody: String? = null
    var serviceRspCode: String? = null

    @Serializable(with = InstantSerializer::class)
    var serviceRspAt: Instant? = null
    var serviceRspBody: String? = null

    @Serializable(with = DurationMicroSerializer::class)
    var serviceDuration: Duration? = null

    override fun getId(): UUID = id

    companion object {
        val mapper: ResultRowDTOMapper<ErrorLogDTO> = ResultRowDTOMapper(ErrorLogDTO::class, ErrorLogTable)
    }
}