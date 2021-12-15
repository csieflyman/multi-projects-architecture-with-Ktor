/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.request

import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.json.InstantSerializer
import fanpoll.infra.base.json.UUIDSerializer
import fanpoll.infra.base.tenant.TenantId
import fanpoll.infra.database.util.ResultRowDTOMapper
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

@Serializable
data class RequestLogDTO(
    @JvmField @Serializable(with = UUIDSerializer::class) val id: UUID
) : EntityDTO<UUID> {

    var reqId: String? = null

    @Serializable(with = InstantSerializer::class)
    var reqAt: Instant? = null

    var api: String? = null
    var headers: String? = null
    var querystring: String? = null
    var reqBody: String? = null
    var project: String? = null
    var function: String? = null
    var tag: String? = null
    var source: PrincipalSource? = null
    var tenantId: TenantId? = null
    var principal: String? = null
    var runAs: Boolean? = null
    var clientId: String? = null
    var clientVersion: String? = null
    var ip: String? = null

    @Serializable(with = InstantSerializer::class)
    var rspAt: Instant? = null

    var rspTime: Long? = null
    var rspStatus: Int? = null
    var rspBody: String? = null

    override fun getId(): UUID = id

    companion object {
        val mapper: ResultRowDTOMapper<RequestLogDTO> = ResultRowDTOMapper(RequestLogDTO::class, RequestLogTable)
    }
}