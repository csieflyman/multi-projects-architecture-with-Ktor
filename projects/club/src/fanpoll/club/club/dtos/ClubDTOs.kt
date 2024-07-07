/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.club.dtos

import fanpoll.club.user.dtos.UserDTO
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.json.kotlinx.InstantSerializer
import fanpoll.infra.base.json.kotlinx.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

@Serializable
data class ClubDTO(@JvmField val id: String) : EntityDTO<String> {
    var name: String? = null
    var enabled: Boolean? = null

    @Serializable(with = UUIDSerializer::class)
    var creatorId: UUID? = null
    var creator: UserDTO? = null

    @Serializable(with = InstantSerializer::class)
    var createdAt: Instant? = null

    @Serializable(with = InstantSerializer::class)
    var updatedAt: Instant? = null

    override fun getId(): String = id
}