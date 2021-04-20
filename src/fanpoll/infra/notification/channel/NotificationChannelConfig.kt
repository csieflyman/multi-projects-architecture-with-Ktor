/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import fanpoll.infra.utils.ConfigUtils
import fanpoll.infra.utils.CoroutineConfig
import fanpoll.infra.utils.MyConfig

// may be above two implementations of the same channel type in the future
data class NotificationChannelConfig(
    val email: EmailConfig? = null,
    val sms: SMSConfig? = null,
    val push: PushMessageConfig? = null,
    val coroutine: CoroutineConfig
) : MyConfig {

    override fun validate() {
        ConfigUtils.require(email == null || sms == null || push == null) {
            "at least one notification channel should be configured"
        }
    }
}