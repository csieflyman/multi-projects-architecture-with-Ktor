/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.release.app.dtos

import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.extension.toObject
import fanpoll.infra.base.json.kotlinx.TaiwanInstantSerializer
import fanpoll.infra.release.app.domain.AppOS
import fanpoll.infra.release.app.domain.AppRelease
import fanpoll.infra.release.app.domain.AppVersion
import io.konform.validation.Validation
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class CreateAppReleaseForm(
    val appId: String,
    val os: AppOS,
    val verName: String,
    val enabled: Boolean,
    @Serializable(with = TaiwanInstantSerializer::class) var releasedAt: Instant? = null,
    val forceUpdate: Boolean,
) : EntityForm<CreateAppReleaseForm, AppRelease, AppVersion>() {

    override fun getEntityId(): AppVersion = AppVersion(appId, os, verName)

    override fun toEntity(): AppRelease = toObject(AppRelease::class)

    override fun validator(): Validation<CreateAppReleaseForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<CreateAppReleaseForm> = Validation {
            CreateAppReleaseForm::verName required { run(AppVersion.NAME_VALIDATOR) }
        }
    }
}

@Serializable
data class UpdateAppReleaseForm(
    val appId: String,
    val os: AppOS,
    val verName: String,
    val enabled: Boolean? = null,
    @Serializable(with = TaiwanInstantSerializer::class) val releasedAt: Instant? = null,
    val forceUpdate: Boolean? = null,
) : EntityForm<UpdateAppReleaseForm, AppRelease, AppVersion>() {

    override fun getEntityId(): AppVersion = AppVersion(appId, os, verName)

    override fun toEntity(): AppRelease = toObject(AppRelease::class)

    override fun validator(): Validation<UpdateAppReleaseForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<UpdateAppReleaseForm> = Validation {
            UpdateAppReleaseForm::verName required { run(AppVersion.NAME_VALIDATOR) }
        }
    }
}