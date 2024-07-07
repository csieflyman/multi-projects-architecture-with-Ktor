/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.club.domain

import fanpoll.club.user.domain.User
import fanpoll.infra.base.entity.Entity
import java.time.Instant
import java.util.*

class Club(@JvmField val id: String) : Entity<String> {
    override fun getId(): String = id

    var name: String? = null
    var enabled: Boolean? = null
    var creatorId: UUID? = null
    var creator: User? = null
    var createdAt: Instant? = null
    var updatedAt: Instant? = null
}

enum class ClubRole {
    Admin, Member
}