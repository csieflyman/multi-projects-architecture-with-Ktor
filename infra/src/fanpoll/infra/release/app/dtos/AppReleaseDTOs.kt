/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.release.app.dtos

import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.json.kotlinx.TaiwanInstantSerializer
import fanpoll.infra.release.app.domain.AppOS
import fanpoll.infra.release.app.domain.AppVersion
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class AppReleaseDTO(val appId: String, val os: AppOS, val verName: String) : EntityDTO<AppVersion> {

    var verNum: Int? = null
    var enabled: Boolean? = null

    @Serializable(with = TaiwanInstantSerializer::class)
    var releasedAt: Instant? = null
    var forceUpdate: Boolean? = null

    override fun getId(): AppVersion = AppVersion(appId, os, verName)
}