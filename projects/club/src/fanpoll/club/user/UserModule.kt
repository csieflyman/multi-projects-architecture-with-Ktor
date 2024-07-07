/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.user

import fanpoll.club.ClubKoinContext
import fanpoll.club.user.domain.User
import fanpoll.club.user.domain.UserJoinedClub
import fanpoll.club.user.dtos.UserDTO
import fanpoll.club.user.repository.UserJoinedClubRepository
import fanpoll.club.user.repository.UserRepository
import fanpoll.club.user.repository.exposed.ExposedUserJoinedClubRepository
import fanpoll.club.user.repository.exposed.ExposedUserRepository
import fanpoll.club.user.repository.exposed.UserJoinedClubTable
import fanpoll.club.user.repository.exposed.UserTable
import fanpoll.club.user.services.UserJoinClubService
import fanpoll.club.user.services.UserService
import fanpoll.infra.database.exposed.util.ResultRowMapper
import fanpoll.infra.database.exposed.util.ResultRowMappers
import io.ktor.server.application.Application
import org.koin.dsl.module

fun Application.loadUserModule() {
    ClubKoinContext.koin.loadModules(listOf(
        module(createdAtStart = true) {
            val userRepository = ExposedUserRepository()
            val userService = UserService(userRepository)
            val userJoinedClubRepository = ExposedUserJoinedClubRepository()
            val userJoinClubService = UserJoinClubService(userJoinedClubRepository)

            single<UserRepository> { userRepository }
            single { userService }
            single<UserJoinedClubRepository> { userJoinedClubRepository }
            single { userJoinClubService }
        }
    ))
    registerResultRowMappers()
}

private fun registerResultRowMappers() {
    ResultRowMappers.register(
        ResultRowMapper(User::class, UserTable),
        ResultRowMapper(UserDTO::class, UserTable),
        ResultRowMapper(UserJoinedClub::class, UserJoinedClubTable)
    )
}