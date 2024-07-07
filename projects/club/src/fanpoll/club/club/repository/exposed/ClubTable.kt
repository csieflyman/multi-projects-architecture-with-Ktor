/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.club.repository.exposed

import fanpoll.infra.database.exposed.sql.createdAtColumn
import fanpoll.infra.database.exposed.sql.updatedAtColumn
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ClubTable : IdTable<String>(name = "club") {
    override val id: Column<EntityID<String>> = varchar("id", 30).entityId()
    override val primaryKey: PrimaryKey = PrimaryKey(id)

    val name = varchar("name", 30)
    val enabled = bool("enabled")
    val creatorId = uuid("creator_id")
    val createdAt = createdAtColumn()
    val updatedAt = updatedAtColumn()
}