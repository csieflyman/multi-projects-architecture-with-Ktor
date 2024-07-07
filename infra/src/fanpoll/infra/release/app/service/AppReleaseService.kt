/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.release.app.service

import fanpoll.infra.base.datetime.DateTimeUtils
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.release.app.domain.AppRelease
import fanpoll.infra.release.app.repository.AppReleaseRepository
import java.time.Instant

class AppReleaseService(private val appReleaseRepository: AppReleaseRepository) {

    suspend fun create(appRelease: AppRelease) {
        if (appRelease.releasedAt == null)
            appRelease.releasedAt = Instant.now()
        else if (appRelease.releasedAt!! < Instant.now())
            throw RequestException(
                InfraResponseCode.ENTITY_PROP_VALUE_INVALID,
                "appVersion ${appRelease.appVersion} releasedAt can't be past time"
            )

        val current = appReleaseRepository.findByAppVersion(appRelease.appVersion)
        if (current != null)
            throw RequestException(InfraResponseCode.ENTITY_ALREADY_EXISTS, "appVersion ${appRelease.appVersion} already exists")

        appReleaseRepository.create(appRelease)
    }

    suspend fun update(appRelease: AppRelease) {
        if (appRelease.releasedAt != null && appRelease.releasedAt!! < Instant.now())
            throw RequestException(
                InfraResponseCode.ENTITY_PROP_VALUE_INVALID,
                "appVersion ${appRelease.appVersion} releasedAt can't be past time"
            )

        val old = appReleaseRepository.findByAppVersion(appRelease.appVersion) ?: throw RequestException(
            InfraResponseCode.ENTITY_NOT_FOUND,
            "appVersion ${appRelease.appVersion} does not exist"
        )

        if (appRelease.releasedAt != null && old.releasedAt != null && old.releasedAt!! < Instant.now() && old.enabled!!)
            throw RequestException(
                InfraResponseCode.ENTITY_STATUS_CONFLICT,
                "cannot update releasedAt because appVersion ${appRelease.appVersion} had been released at " +
                        DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER.format(old.releasedAt!!)
            )

        appReleaseRepository.update(appRelease)
    }
}