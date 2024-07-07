/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.push.token

import fanpoll.infra.base.entity.Entity
import fanpoll.infra.release.app.domain.AppOS
import java.time.Instant
import java.util.*

class DevicePushToken(val deviceId: String) : Entity<String> {
    var userId: UUID? = null
    var os: AppOS? = null
    var pushToken: String? = null
    var createdAt: Instant? = null
    var updatedAt: Instant? = null

    override fun getId(): String = deviceId
}