/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import mu.KotlinLogging

data class SMSConfig(
    val logSuccess: Boolean
)

//data class MitakeConfig(
//
//)

object MitakeService : NotificationChannelService {

    private val logger = KotlinLogging.logger {}

    private lateinit var config: SMSConfig

    fun init(config: SMSConfig) {
        MitakeService.config = config
    }

    override fun shutdown() {
        TODO("Not yet implemented")
    }
}