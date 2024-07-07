/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.database

import fanpoll.club.ClubKoinContext
import fanpoll.club.database.exposed.ClubDatabase
import fanpoll.infra.database.loadDatabaseModule
import io.ktor.server.application.Application

fun Application.loadClubDatabaseModule(databasesConfig: ClubDatabasesConfig) {
    databasesConfig.club.name = ClubDatabase.Club.name

    ClubKoinContext.koin.loadModules(
        listOf(loadDatabaseModule(listOf(databasesConfig.club)))
    )
}