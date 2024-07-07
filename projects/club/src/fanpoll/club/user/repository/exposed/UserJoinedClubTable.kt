/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.user.repository.exposed

import fanpoll.club.club.repository.exposed.ClubTable
import fanpoll.infra.database.exposed.sql.createdAtColumn
import fanpoll.infra.database.exposed.sql.updatedAtColumn
import org.jetbrains.exposed.sql.Table

object UserJoinedClubTable : Table(name = "user_joined_club") {
    val userId = reference("user_id", UserTable)
    val clubId = reference("club_id", ClubTable)
    val isAdmin = bool("is_admin")
    val createdAt = createdAtColumn()
    val updatedAt = updatedAtColumn()
    override val primaryKey: PrimaryKey = PrimaryKey(userId, clubId, name = "pk_user_club_id")
}