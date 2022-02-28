/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.email

import fanpoll.infra.base.config.ValidateableConfig
import fanpoll.infra.notification.NotificationLogConfig
import fanpoll.infra.notification.channel.email.senders.AwsSESConfig
import fanpoll.infra.notification.channel.email.senders.SendGridConfig

data class EmailConfig(
    val noReplyAddress: String,
    val marketingAddress: String? = null,
    val logging: NotificationLogConfig? = null,
    val mock: Boolean? = false,
    val sendgrid: SendGridConfig? = null,
    val awsSES: AwsSESConfig? = null
) : ValidateableConfig {

    override fun validate() {
        require(mock == true || sendgrid != null || awsSES != null) {
            "at least one email service provider should be configured"
        }
    }

    class Builder {

        lateinit var noReplyAddress: String
        var marketingAddress: String? = null
        private var logging: NotificationLogConfig? = null
        var mock: Boolean? = false

        private var sendgrid: SendGridConfig? = null
        private var awsSES: AwsSESConfig? = null

        fun logging(configure: NotificationLogConfig.Builder.() -> Unit) {
            logging = NotificationLogConfig.Builder().apply(configure).build()
        }

        fun sendgrid(block: SendGridConfig.Builder.() -> Unit) {
            sendgrid = SendGridConfig.Builder().apply(block).build()
        }

        fun awsSES(block: AwsSESConfig.Builder.() -> Unit) {
            awsSES = AwsSESConfig.Builder().apply(block).build()
        }

        fun build(): EmailConfig {
            return EmailConfig(noReplyAddress, marketingAddress, logging, mock, sendgrid, awsSES).apply { validate() }
        }
    }
}