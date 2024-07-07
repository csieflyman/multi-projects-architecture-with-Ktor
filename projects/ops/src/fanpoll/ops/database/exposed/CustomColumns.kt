/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.database.exposed

import fanpoll.ops.OpsUserRole
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType

fun Table.opsUserRolesColumn(name: String): Column<Set<OpsUserRole>> = registerColumn(name, OpsUserRolesColumnType())

class OpsUserRolesColumnType : ColumnType<Set<OpsUserRole>>() {
    private val varcharColumnType = VarCharColumnType(100)
    override fun sqlType(): String = varcharColumnType.sqlType()
    override fun valueFromDB(value: Any): Set<OpsUserRole> = value as? Set<OpsUserRole> ?: (value as String)
        .removeSurrounding("[", "]").split(",")
        .map { name -> OpsUserRole.entries.first { it.name == name.trim() } }.toSet()

    override fun notNullValueToDB(value: Set<OpsUserRole>): Any = value.toString()
}