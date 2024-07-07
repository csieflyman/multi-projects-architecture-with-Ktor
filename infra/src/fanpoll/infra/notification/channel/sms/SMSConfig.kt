/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.sms

import fanpoll.infra.config.ValidateableConfig
import fanpoll.infra.notification.NotificationLogConfig
import fanpoll.infra.notification.channel.sms.senders.MitakeConfig
import fanpoll.infra.notification.channel.sms.senders.TwilioSMSConfig

data class SMSConfig(
    var logging: NotificationLogConfig? = null,
    val mock: Boolean? = false,
    val twilio: TwilioSMSConfig? = null,
    val mitake: MitakeConfig? = null
) : ValidateableConfig {

    override fun validate() {
        require(mock == true || twilio != null || mitake != null) {
            "at least one sms service provider should be configured"
        }
    }
}