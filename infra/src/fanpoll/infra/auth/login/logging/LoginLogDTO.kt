/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login.logging

import fanpoll.infra.auth.login.LoginResultCode
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
data class LoginLogDTO(
    @JvmField @Serializable(with = UUIDSerializer::class) val id: UUID
) : EntityDTO<UUID> {

    @Serializable(with = UUIDSerializer::class)
    var userId: UUID? = null

    var resultCode: LoginResultCode? = null

    @Serializable(with = InstantSerializer::class)
    var occurAt: Instant? = null

    var project: String? = null
    var source: PrincipalSource? = null
    var tenantId: TenantId? = null
    var clientId: String? = null
    var clientVersion: String? = null
    var ip: String? = null
    var sid: String? = null

    override fun getId(): UUID = id

    companion object {
        val mapper: ResultRowDTOMapper<LoginLogDTO> = ResultRowDTOMapper(LoginLogDTO::class, LoginLogTable)
    }
}