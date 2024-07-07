/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.database.exposed

import fanpoll.club.ClubUserRole
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType

fun Table.clubUserRolesColumn(name: String): Column<Set<ClubUserRole>> = registerColumn(name, ClubUserRolesColumnType())

class ClubUserRolesColumnType : ColumnType<Set<ClubUserRole>>() {
    private val varcharColumnType = VarCharColumnType(100)
    override fun sqlType(): String = varcharColumnType.sqlType()
    override fun valueFromDB(value: Any): Set<ClubUserRole> = value as? Set<ClubUserRole> ?: (value as String)
        .removeSurrounding("[", "]").split(",")
        .map { name -> ClubUserRole.entries.first { it.name == name.trim() } }.toSet()

    override fun notNullValueToDB(value: Set<ClubUserRole>): Any = value.toString()
}