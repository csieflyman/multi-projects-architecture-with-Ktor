/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

data class OpsConfig(
    val auth: OpsAuthConfig,
    val notification: OpsNotificationConfig
)

data class OpsNotificationConfig(
    val infoEmail: String? = null,
    val alertEmail: String? = null
)