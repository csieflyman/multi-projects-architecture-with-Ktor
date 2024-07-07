/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.user.repository.exposed

import fanpoll.infra.database.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(UserTable)

    var account by UserTable.account
    var enabled by UserTable.enabled
    var name by UserTable.name
    var email by UserTable.email
    var mobile by UserTable.mobile
    var lang by UserTable.lang
    var password by UserTable.password
    var roles by UserTable.roles
    var createdAt by UserTable.createdAt
    var updatedAt by UserTable.updatedAt
}