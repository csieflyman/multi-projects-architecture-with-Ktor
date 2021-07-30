/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import fanpoll.infra.base.config.ValidateableConfig
import fanpoll.infra.notification.channel.email.EmailConfig
import fanpoll.infra.notification.channel.push.PushConfig
import fanpoll.infra.notification.channel.sms.SMSConfig

data class NotificationChannelConfig(
    val email: EmailConfig? = null,
    val push: PushConfig? = null,
    val sms: SMSConfig? = null
) : ValidateableConfig {

    override fun validate() {
        require(email != null || sms != null || push != null) {
            "at least one notification channel should be configured"
        }
    }

    class Builder {

        private var email: EmailConfig? = null
        private var push: PushConfig? = null
        private var sms: SMSConfig? = null

        fun email(block: EmailConfig.Builder.() -> Unit) {
            email = EmailConfig.Builder().apply(block).build()
        }

        fun push(block: PushConfig.Builder.() -> Unit) {
            push = PushConfig.Builder().apply(block).build()
        }

        fun sms(block: SMSConfig.Builder.() -> Unit) {
            sms = SMSConfig.Builder().apply(block).build()
        }

        fun build(): NotificationChannelConfig {
            return NotificationChannelConfig(email, push, sms).apply { validate() }
        }
    }
}