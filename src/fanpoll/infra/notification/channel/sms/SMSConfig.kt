/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.sms

import fanpoll.infra.base.config.ValidateableConfig
import fanpoll.infra.notification.NotificationLogConfig
import fanpoll.infra.notification.channel.sms.senders.MitakeConfig

data class SMSConfig(
    val logging: NotificationLogConfig? = null,
    val mock: Boolean? = false,
    val mitake: MitakeConfig? = null
) : ValidateableConfig {

    override fun validate() {
        require(mock == true || mitake != null) {
            "at least one sms service provider should be configured"
        }
    }

    class Builder {

        private var logging: NotificationLogConfig? = null
        var mock: Boolean? = false
        var mitake: MitakeConfig? = null

        fun logging(configure: NotificationLogConfig.Builder.() -> Unit) {
            logging = NotificationLogConfig.Builder().apply(configure).build()
        }

        fun build(): SMSConfig {
            return SMSConfig(logging, mock, mitake).apply { validate() }
        }
    }
}