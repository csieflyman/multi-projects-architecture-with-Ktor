/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.release.app.repository.exposed

import fanpoll.infra.database.exposed.InfraRepositoryComponent
import fanpoll.infra.database.exposed.sql.dbExecute
import fanpoll.infra.database.exposed.sql.insert
import fanpoll.infra.database.exposed.sql.singleOrNull
import fanpoll.infra.release.app.domain.AppRelease
import fanpoll.infra.release.app.domain.AppVersion
import fanpoll.infra.release.app.repository.AppReleaseRepository
import org.jetbrains.exposed.dao.id.CompositeID
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import java.time.Instant

class AppReleaseExposedRepository : InfraRepositoryComponent(), AppReleaseRepository {

    override suspend fun create(appRelease: AppRelease) {
        dbExecute(infraDatabase) {
            AppReleaseTable.insert(appRelease)
        }
    }

    override suspend fun update(appRelease: AppRelease) {
        val appVersion = appRelease.appVersion
        dbExecute(infraDatabase) {
            AppReleaseTable.update({
                AppReleaseTable.id eq CompositeID {
                    it[AppReleaseTable.appId] = appVersion.appId
                    it[AppReleaseTable.os] = appVersion.os
                    it[AppReleaseTable.verName] = appVersion.name
                }
            }) { updateStatement ->
                appRelease.enabled?.let { updateStatement[enabled] = it }
                appRelease.releasedAt?.let { updateStatement[releasedAt] = it }
                appRelease.forceUpdate?.let { updateStatement[forceUpdate] = it }
            }
        }
    }

    override suspend fun findByAppVersion(appVersion: AppVersion): AppRelease? {
        return dbExecute(infraDatabase) {
            AppReleaseTable.selectAll().where {
                AppReleaseTable.id eq CompositeID {
                    it[AppReleaseTable.appId] = appVersion.appId
                    it[AppReleaseTable.os] = appVersion.os
                    it[AppReleaseTable.verName] = appVersion.name
                }
            }.singleOrNull(AppRelease::class)
        }
    }

    override suspend fun checkForceUpdate(appVersion: AppVersion): Map<Int, Boolean> {
        return dbExecute(infraDatabase) {
            AppReleaseTable.select(AppReleaseTable.id, AppReleaseTable.forceUpdate).where {
                (AppReleaseTable.appId workaroundEq appVersion.appId) and
                        (AppReleaseTable.os workaroundEq appVersion.os) and
                        (AppReleaseTable.verNum greater appVersion.number) and
                        (AppReleaseTable.enabled eq true) and
                        (AppReleaseTable.releasedAt lessEq Instant.now())
            }.orderBy(AppReleaseTable.releasedAt, SortOrder.DESC)
                .toList().associate { it[AppReleaseTable.id].value[AppReleaseTable.verNum] to it[AppReleaseTable.forceUpdate] }
        }
    }

    /**
     * Workaround solution for Exposed 0.53 version
     * Occur at: IdTable.kt:174
     * Caused by: java.lang.ClassCastException: class java.lang.String cannot be cast to class org.jetbrains.exposed.dao.id.CompositeID
     */
    private infix fun <T : Comparable<T>, E : EntityID<T>?, V : T?> ExpressionWithColumnType<E>.workaroundEq(t: V): Op<Boolean> {
        @Suppress("UNCHECKED_CAST")
        val table = (columnType as EntityIDColumnType<*>).idColumn.table as IdTable<T>
        val entityID = EntityID(t!!, table)
        return EqOp(this, wrap(entityID))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T, S : T?> ExpressionWithColumnType<in S>.wrap(value: T): QueryParameter<T> =
        QueryParameter(value, columnType as IColumnType<T & Any>)
}