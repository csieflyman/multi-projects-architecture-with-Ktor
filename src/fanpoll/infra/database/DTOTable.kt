/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import java.util.*

interface DTOTable<EID : Comparable<EID>> {

    val naturalKeys: List<Column<out Any>>?

    val surrogateKey: Column<EntityID<EID>>?
}

abstract class IdDTOTable<EID : Comparable<EID>>(name: String = "") : IdTable<EID>(name), DTOTable<EID>

abstract class LongIdDTOTable(name: String = "", columnName: String = "id") : IdDTOTable<Long>(name) {
    override val id: Column<EntityID<Long>> = long(columnName).autoIncrement().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}

abstract class UUIDDTOTable(name: String = "", columnName: String = "id", autoGenerate: Boolean = true) : IdDTOTable<UUID>(name) {
    override val id: Column<EntityID<UUID>> = uuid(columnName).let { if (autoGenerate) it.autoGenerate() else it }.entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}