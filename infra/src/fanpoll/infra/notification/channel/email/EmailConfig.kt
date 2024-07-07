/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.email

import fanpoll.infra.config.ValidateableConfig
import fanpoll.infra.notification.NotificationLogConfig
import fanpoll.infra.notification.channel.email.senders.SendGridConfig

data class EmailConfig(
    val noReplyAddress: String,
    val marketingAddress: String? = null,
    var logging: NotificationLogConfig? = null,
    val mock: Boolean? = false,
    val sendgrid: SendGridConfig? = null
) : ValidateableConfig {

    override fun validate() {
        require(mock == true || sendgrid != null) {
            "at least one email service provider should be configured"
        }
    }
}