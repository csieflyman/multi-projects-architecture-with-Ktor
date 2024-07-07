/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.user.dtos

import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.json.kotlinx.InstantSerializer
import fanpoll.infra.base.json.kotlinx.UUIDSerializer
import fanpoll.infra.i18n.Lang
import fanpoll.infra.openapi.schema.operation.support.OpenApiModel
import fanpoll.ops.OpsUserRole
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

@OpenApiModel(propertyNameOrder = ["id", "account", "enabled", "role", "name"])
@Serializable
data class UserDTO(@JvmField @Serializable(with = UUIDSerializer::class) val id: UUID) : EntityDTO<UUID> {

    var account: String? = null
    var enabled: Boolean? = null
    var name: String? = null
    var email: String? = null
    var mobile: String? = null
    var lang: Lang? = null
    var roles: Set<OpsUserRole> = emptySet()

    @Serializable(with = InstantSerializer::class)
    var createdAt: Instant? = null

    @Serializable(with = InstantSerializer::class)
    var updatedAt: Instant? = null

    override fun getId(): UUID = id
}

