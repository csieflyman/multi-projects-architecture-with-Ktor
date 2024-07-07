/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.user.domain

import fanpoll.club.club.domain.ClubRole
import java.time.Instant
import java.util.*

data class UserJoinedClub(val userId: UUID, val clubId: String) {
    var role: ClubRole? = null
    var createdAt: Instant? = null
    var updatedAt: Instant? = null
}