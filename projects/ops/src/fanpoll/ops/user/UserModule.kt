/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.user

import fanpoll.infra.database.exposed.util.ResultRowMapper
import fanpoll.infra.database.exposed.util.ResultRowMappers
import fanpoll.ops.OpsKoinContext
import fanpoll.ops.user.domain.User
import fanpoll.ops.user.dtos.UserDTO
import fanpoll.ops.user.repository.UserRepository
import fanpoll.ops.user.repository.exposed.UserExposedRepository
import fanpoll.ops.user.repository.exposed.UserTable
import fanpoll.ops.user.services.UserService
import io.ktor.server.application.Application
import org.koin.dsl.module

fun Application.loadUserModule() {
    OpsKoinContext.koin.loadModules(listOf(
        module(createdAtStart = true) {
            val userRepository = UserExposedRepository()
            val userService = UserService(userRepository)
            single<UserRepository> { userRepository }
            single { userService }
        }
    ))
    registerResultRowMappers()
}

private fun registerResultRowMappers() {
    ResultRowMappers.register(
        ResultRowMapper(User::class, UserTable), ResultRowMapper(UserDTO::class, UserTable)
    )
}