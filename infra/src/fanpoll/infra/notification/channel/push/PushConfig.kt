/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.push

import fanpoll.infra.base.config.ValidateableConfig
import fanpoll.infra.notification.NotificationLogConfig
import fanpoll.infra.notification.channel.push.senders.FCMConfig

data class PushConfig(
    val logging: NotificationLogConfig? = null,
    val mock: Boolean? = false,
    val fcm: FCMConfig? = null
) : ValidateableConfig {

    override fun validate() {
        require(mock == true || fcm != null) {
            "at least one push service provider should be configured"
        }
    }

    class Builder {

        private var logging: NotificationLogConfig? = null
        var mock: Boolean? = false
        private var fcmConfig: FCMConfig? = null

        fun logging(configure: NotificationLogConfig.Builder.() -> Unit) {
            logging = NotificationLogConfig.Builder().apply(configure).build()
        }

        fun fcm(block: FCMConfig.Builder.() -> Unit) {
            fcmConfig = FCMConfig.Builder().apply(block).build()
        }

        fun build(): PushConfig {
            return PushConfig(logging, mock, fcmConfig).apply { validate() }
        }
    }
}