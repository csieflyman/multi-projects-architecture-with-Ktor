/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.user.domain

import fanpoll.club.ClubUserRole
import fanpoll.infra.base.entity.Entity
import fanpoll.infra.i18n.Lang
import java.time.Instant
import java.util.*

class User(@JvmField val id: UUID) : Entity<UUID> {
    var account: String? = null
    var enabled: Boolean? = null
    var name: String? = null
    var gender: Gender? = null
    var birthYear: Int? = null
    var email: String? = null
    var mobile: String? = null
    var lang: Lang? = null
    var password: String? = null
    var roles: Set<ClubUserRole>? = null
    var createdAt: Instant? = null
    var updatedAt: Instant? = null

    override fun getId(): UUID = id
}

enum class Gender {
    Male, Female
}