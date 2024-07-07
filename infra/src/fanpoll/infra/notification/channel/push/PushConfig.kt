/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.push

import fanpoll.infra.config.ValidateableConfig
import fanpoll.infra.notification.NotificationLogConfig
import fanpoll.infra.notification.channel.push.senders.FCMConfig

data class PushConfig(
    var logging: NotificationLogConfig? = null,
    val mock: Boolean? = false,
    val fcm: FCMConfig? = null
) : ValidateableConfig {

    override fun validate() {
        require(mock == true || fcm != null) {
            "at least one push service provider should be configured"
        }
    }
}