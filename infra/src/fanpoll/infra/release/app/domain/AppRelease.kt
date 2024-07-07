/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.release.app.domain

import fanpoll.infra.base.entity.Entity
import java.time.Instant

class AppRelease(val appId: String, val os: AppOS, val verName: String) : Entity<AppVersion> {

    val appVersion: AppVersion = AppVersion(appId, os, verName)
    val verNum: Int = appVersion.number
    var enabled: Boolean? = null
    var releasedAt: Instant? = null
    var forceUpdate: Boolean? = null

    override fun getId(): AppVersion = appVersion
}