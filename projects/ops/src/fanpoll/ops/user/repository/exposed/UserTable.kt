/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.user.repository.exposed

import fanpoll.infra.base.form.ValidationUtils
import fanpoll.infra.database.exposed.sql.createdAtColumn
import fanpoll.infra.database.exposed.sql.langColumn
import fanpoll.infra.database.exposed.sql.updatedAtColumn
import fanpoll.infra.database.exposed.util.ResultRowMapper
import fanpoll.infra.database.exposed.util.ResultRowMappers
import fanpoll.ops.database.exposed.opsUserRolesColumn
import fanpoll.ops.user.domain.User
import fanpoll.ops.user.dtos.UserDTO
import org.jetbrains.exposed.dao.id.UUIDTable

object UserTable : UUIDTable(name = "user") {

    val account = varchar("account", ValidationUtils.EMAIL_MAX_LENGTH) //unique
    val enabled = bool("enabled")
    val name = varchar("name", 30)
    val email = varchar("email", ValidationUtils.EMAIL_MAX_LENGTH)
    val mobile = varchar("mobile", ValidationUtils.MOBILE_NUMBER_LENGTH).nullable()
    val lang = langColumn("lang").nullable()
    val password = varchar("password", 1000)
    val roles = opsUserRolesColumn("roles")
    val createdAt = createdAtColumn()
    val updatedAt = updatedAtColumn()

    init {
        ResultRowMappers.register(ResultRowMapper(User::class, UserTable), ResultRowMapper(UserDTO::class, UserTable))
    }
}