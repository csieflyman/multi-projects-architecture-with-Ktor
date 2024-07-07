/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database

import fanpoll.infra.database.exposed.InfraDatabase
import io.ktor.server.application.Application
import org.koin.core.context.loadKoinModules

fun Application.loadInfraDatabaseModule(databasesConfig: InfraDatabasesConfig) {
    databasesConfig.infra.name = InfraDatabase.Infra.name
    databasesConfig.infra.isDefault = true

    loadKoinModules(loadDatabaseModule(listOf(databasesConfig.infra)))
}