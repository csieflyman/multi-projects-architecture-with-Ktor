/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.sql

import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.UserRole
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.i18n.Lang
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

fun Table.createdAtColumn(): Column<Instant> = timestamp("created_at")
    .defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentTimestamp)

fun Table.updatedAtColumn(): Column<Instant> = timestamp("updated_at")
    .defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentTimestamp)

fun Table.principalSourceColumn(name: String): Column<PrincipalSource> = registerColumn(name, PrincipalSourceColumnType())

class PrincipalSourceColumnType : ColumnType<PrincipalSource>() {
    private val varcharColumnType = VarCharColumnType(30)
    override fun sqlType(): String = varcharColumnType.sqlType()
    override fun valueFromDB(value: Any): PrincipalSource = PrincipalSource.lookup(value as String)
    override fun notNullValueToDB(value: PrincipalSource): Any = value.id
}

fun Table.userTypeColumn(name: String): Column<UserType> = registerColumn(name, UserTypeColumnType())

class UserTypeColumnType : ColumnType<UserType>() {
    private val varcharColumnType = VarCharColumnType(20)
    override fun sqlType(): String = varcharColumnType.sqlType()
    override fun valueFromDB(value: Any): UserType = UserType.getTypeById(value as String)
    override fun notNullValueToDB(value: UserType): Any = value.getId()
}

fun Table.userRolesColumn(name: String): Column<Set<UserRole>> = registerColumn(name, UserRolesColumnType())

class UserRolesColumnType : ColumnType<Set<UserRole>>() {
    private val varcharColumnType = VarCharColumnType(100)
    override fun sqlType(): String = varcharColumnType.sqlType()
    override fun valueFromDB(value: Any): Set<UserRole> = value as? Set<UserRole> ?: (value as String)
        .removeSurrounding("[", "]").split(",")
        .map { UserRole.getRoleById(it.trim()) }.toSet()

    override fun notNullValueToDB(value: Set<UserRole>): Any = value.toString()
}

fun Table.langColumn(name: String): Column<Lang> = registerColumn(name, LangColumnType())

class LangColumnType : ColumnType<Lang>() {
    private val varcharColumnType = VarCharColumnType(20)
    override fun sqlType(): String = varcharColumnType.sqlType()
    override fun valueFromDB(value: Any): Lang = value as? Lang ?: Lang(value as String)
    override fun notNullValueToDB(value: Lang): Any = value.code
}