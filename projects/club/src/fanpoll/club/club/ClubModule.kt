/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.club

import fanpoll.club.ClubKoinContext
import fanpoll.club.club.dtos.ClubDTO
import fanpoll.club.club.repository.ClubRepository
import fanpoll.club.club.repository.exposed.ClubTable
import fanpoll.club.club.repository.exposed.ExposedClubRepository
import fanpoll.club.club.services.ClubService
import fanpoll.club.user.services.UserJoinClubService
import fanpoll.infra.database.exposed.util.ResultRowMapper
import fanpoll.infra.database.exposed.util.ResultRowMappers
import io.ktor.server.application.Application
import org.koin.dsl.module

fun Application.loadClubModule() {
    ClubKoinContext.koin.loadModules(listOf(
        module(createdAtStart = true) {
            val clubRepository = ExposedClubRepository()
            val clubService = ClubService(clubRepository, ClubKoinContext.koin.get<UserJoinClubService>())
            single<ClubRepository> { clubRepository }
            single { clubService }
        }
    ))
    registerResultRowMappers()
}

private fun registerResultRowMappers() {
    ResultRowMappers.register(
        ResultRowMapper(ClubDTO::class, ClubTable)
    )
}