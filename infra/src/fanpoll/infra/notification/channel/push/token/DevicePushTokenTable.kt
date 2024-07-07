/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.push.token

import fanpoll.infra.database.exposed.sql.createdAtColumn
import fanpoll.infra.database.exposed.sql.updatedAtColumn
import fanpoll.infra.release.app.domain.AppOS
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object DevicePushTokenTable : IdTable<String>(name = "infra_device_push_token") {

    override val id: Column<EntityID<String>> = varchar("device_id", 64).entityId()
    override val primaryKey: PrimaryKey = PrimaryKey(id)

    val userId = uuid("user_id")
    val os = enumeration("os", AppOS::class)
    val pushToken = varchar("push_token", 255)
    val createdAt = createdAtColumn()
    val updatedAt = updatedAtColumn()
}