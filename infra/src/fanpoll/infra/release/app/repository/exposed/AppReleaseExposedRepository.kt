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
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
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
                (AppReleaseTable.appId eq appVersion.appId) and
                        (AppReleaseTable.os eq appVersion.os) and
                        (AppReleaseTable.verName eq appVersion.name)
            }) { updateStatement ->
                appRelease.enabled?.let { updateStatement[enabled] = it }
                appRelease.releasedAt?.let { updateStatement[releasedAt] = it }
                appRelease.forceUpdate?.let { updateStatement[forceUpdate] = it }
            }
        }
    }

    override suspend fun findByDbId(id: Long): AppRelease? {
        return dbExecute(infraDatabase) {
            AppReleaseTable.selectAll().where { (AppReleaseTable.id eq id) }.singleOrNull(AppRelease::class)
        }
    }

    override suspend fun findByAppVersion(appVersion: AppVersion): AppRelease? {
        return dbExecute(infraDatabase) {
            AppReleaseTable.selectAll().where {
                (AppReleaseTable.appId eq appVersion.appId) and
                        (AppReleaseTable.os eq appVersion.os) and
                        (AppReleaseTable.verName eq appVersion.name)
            }.singleOrNull(AppRelease::class)
        }
    }

    override suspend fun checkForceUpdate(appVersion: AppVersion): Map<Long, Boolean> {
        return dbExecute(infraDatabase) {
            AppReleaseTable.select(AppReleaseTable.id, AppReleaseTable.forceUpdate).where {
                (AppReleaseTable.appId eq appVersion.appId) and
                        (AppReleaseTable.os eq appVersion.os) and
                        (AppReleaseTable.verNum greater appVersion.number) and
                        (AppReleaseTable.enabled eq true) and
                        (AppReleaseTable.releasedAt lessEq Instant.now())
            }.orderBy(AppReleaseTable.releasedAt, SortOrder.DESC)
                .toList().associate { it[AppReleaseTable.id].value to it[AppReleaseTable.forceUpdate] }
        }
    }
}