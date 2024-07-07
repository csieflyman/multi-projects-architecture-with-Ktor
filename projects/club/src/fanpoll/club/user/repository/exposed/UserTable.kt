/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.user.repository.exposed

import fanpoll.club.database.exposed.clubUserRolesColumn
import fanpoll.club.user.domain.Gender
import fanpoll.infra.base.form.ValidationUtils
import fanpoll.infra.database.exposed.sql.createdAtColumn
import fanpoll.infra.database.exposed.sql.langColumn
import fanpoll.infra.database.exposed.sql.updatedAtColumn
import org.jetbrains.exposed.dao.id.UUIDTable

object UserTable : UUIDTable(name = "user") {

    val account = varchar("account", ValidationUtils.EMAIL_MAX_LENGTH) //unique
    val enabled = bool("enabled")
    val name = varchar("name", 30)
    val gender = enumeration("gender", Gender::class).nullable()
    val birthYear = integer("birth_year").nullable()
    val email = varchar("email", ValidationUtils.EMAIL_MAX_LENGTH).nullable()
    val mobile = varchar("mobile", ValidationUtils.MOBILE_NUMBER_LENGTH).nullable()
    val lang = langColumn("lang").nullable()
    val password = varchar("password", 1000)
    val roles = clubUserRolesColumn("roles")
    val createdAt = createdAtColumn()
    val updatedAt = updatedAtColumn()
}