/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.user.services

import fanpoll.infra.auth.login.util.UserPasswordUtils
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.ops.user.domain.User
import fanpoll.ops.user.dtos.CreateUserForm
import fanpoll.ops.user.dtos.UpdateUserForm
import fanpoll.ops.user.dtos.UpdateUserPasswordForm
import fanpoll.ops.user.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

class UserService(private val userRepository: UserRepository) {

    val logger = KotlinLogging.logger {}

    suspend fun createUser(form: CreateUserForm): UUID {
        form.password = UserPasswordUtils.hashPassword(form.password)
        return userRepository.create(form.toEntity())
    }

    suspend fun updateUser(form: UpdateUserForm) {
        userRepository.update(form.toEntity())
    }

    suspend fun getUserById(userId: UUID): User {
        return userRepository.getById(userId)
    }

    suspend fun updatePassword(form: UpdateUserPasswordForm) {
        val user = userRepository.getById(form.userId)
        if (UserPasswordUtils.verifyPassword(form.oldPassword, user.password!!)) {
            user.password = UserPasswordUtils.hashPassword(form.newPassword)
            userRepository.update(user)
        } else throw RequestException(InfraResponseCode.AUTH_BAD_PASSWORD)
    }
}