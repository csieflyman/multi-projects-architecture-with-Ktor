/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.user.domain

import fanpoll.infra.base.entity.Entity
import fanpoll.infra.i18n.Lang
import fanpoll.ops.OpsUserRole
import java.time.Instant
import java.util.*

class User(@JvmField val id: UUID) : Entity<UUID> {
    override fun getId(): UUID = id

    var account: String? = null
    var enabled: Boolean? = null
    var name: String? = null
    var email: String? = null
    var mobile: String? = null
    var lang: Lang? = null
    var password: String? = null
    var roles: Set<OpsUserRole> = emptySet()
    var createdAt: Instant? = null
    var updatedAt: Instant? = null
}