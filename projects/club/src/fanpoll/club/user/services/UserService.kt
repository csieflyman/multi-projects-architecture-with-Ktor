/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.user.services

import fanpoll.club.user.domain.User
import fanpoll.club.user.dtos.CreateUserForm
import fanpoll.club.user.dtos.UpdateUserForm
import fanpoll.club.user.dtos.UpdateUserPasswordForm
import fanpoll.club.user.repository.UserRepository
import fanpoll.infra.auth.login.util.UserPasswordUtils
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import java.util.*

class UserService(private val userRepository: UserRepository) {

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