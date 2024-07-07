/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.club.repository.exposed

import fanpoll.infra.database.exposed.dao.StringEntity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ClubEntity(id: EntityID<String>) : StringEntity(id) {
    companion object : EntityClass<String, ClubEntity>(ClubTable)

    var name by ClubTable.name
    var enabled by ClubTable.enabled
    var creatorId by ClubTable.creatorId
    var createdAt by ClubTable.createdAt
    var updatedAt by ClubTable.updatedAt
}