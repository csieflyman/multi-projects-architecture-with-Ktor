/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import fanpoll.infra.config.ValidateableConfig
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
}