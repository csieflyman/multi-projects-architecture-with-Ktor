/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.ops.auth.AuthConfig
import fanpoll.ops.database.OpsDatabasesConfig

data class OpsConfig(
    val databases: OpsDatabasesConfig,
    val auth: AuthConfig
)