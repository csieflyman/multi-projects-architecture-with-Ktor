/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.release.app.repository.exposed

import fanpoll.infra.database.exposed.sql.NaturalKeyTable
import fanpoll.infra.database.exposed.sql.createdAtColumn
import fanpoll.infra.database.exposed.sql.updatedAtColumn
import fanpoll.infra.release.app.domain.AppOS
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp

object AppReleaseTable : LongIdTable(name = "infra_app_release"), NaturalKeyTable {

    val appId = varchar("app_id", 30)
    val os = enumeration("os", AppOS::class)
    val verName = varchar("ver_name", 6)
    val verNum = integer("ver_num")
    val enabled = bool("enabled")
    val releasedAt = timestamp("released_at")
    val forceUpdate = bool("force_update")

    val createdAt = createdAtColumn()
    val updatedAt = updatedAtColumn()

    override fun getNaturalKeys(): List<Column<*>> = listOf(appId, os, verName)
}