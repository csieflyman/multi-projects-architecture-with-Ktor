/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.release.app.repository

import fanpoll.infra.release.app.domain.AppRelease
import fanpoll.infra.release.app.domain.AppVersion

interface AppReleaseRepository {
    suspend fun create(appRelease: AppRelease)
    suspend fun update(appRelease: AppRelease)
    suspend fun findByDbId(id: Long): AppRelease?
    suspend fun findByAppVersion(appVersion: AppVersion): AppRelease?
    suspend fun checkForceUpdate(appVersion: AppVersion): Map<Long, Boolean>
}