/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.club.auth.AuthConfig
import fanpoll.club.database.ClubDatabasesConfig

data class ClubConfig(
    val databases: ClubDatabasesConfig,
    val auth: AuthConfig
)