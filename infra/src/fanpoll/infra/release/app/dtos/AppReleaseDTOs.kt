/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.release.app.dtos

import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.json.kotlinx.TaiwanInstantSerializer
import fanpoll.infra.release.app.domain.AppOS
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant

@Serializable
data class AppReleaseDTO(@Transient val id: Long = 0) : EntityDTO<Long> {

    var appId: String? = null
    var os: AppOS? = null
    var verName: String? = null
    var verNum: Int? = null
    var enabled: Boolean? = null

    @Serializable(with = TaiwanInstantSerializer::class)
    var releasedAt: Instant? = null
    var forceUpdate: Boolean? = null

    override fun getId(): Long = id
}