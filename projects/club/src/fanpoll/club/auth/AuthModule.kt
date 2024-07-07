/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.auth

import fanpoll.club.ClubAuth
import fanpoll.club.ClubKoinContext
import fanpoll.club.ClubUserType
import fanpoll.club.auth.login.ClubLoginService
import fanpoll.club.auth.login.UserLoginExposedRepository
import fanpoll.club.auth.login.UserLoginRepository
import fanpoll.club.user.repository.UserRepository
import fanpoll.infra.auth.provider.RunAsUser
import fanpoll.infra.auth.provider.UserSessionAuthValidator
import fanpoll.infra.auth.provider.runAs
import fanpoll.infra.auth.provider.service
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.notification.channel.push.token.DevicePushTokenRepository
import fanpoll.infra.session.MySessionStorage
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.session
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.koin.ktor.ext.getKoin

fun Application.loadAuthModule(authConfig: AuthConfig) {
    val infraKoin = getKoin()
    ClubKoinContext.koin.loadModules(listOf(
        module(createdAtStart = true) {
            val userLoginRepository = UserLoginExposedRepository()
            single<UserLoginRepository> { userLoginRepository }
            val devicePushTokenRepository = infraKoin.get<DevicePushTokenRepository>()
            single { ClubLoginService(userLoginRepository, devicePushTokenRepository, infraKoin.get(), infraKoin.get()) }
        }
    ))

    val userRepository = ClubKoinContext.koin.get<UserRepository>()
    val sessionStorage = infraKoin.get<MySessionStorage>()
    authentication {
        service(ClubAuth.serviceAuthProviderName, authConfig.getServiceAuthConfigs())
        session(
            ClubAuth.userAuthProviderName,
            UserSessionAuthValidator(authConfig.getUserAuthConfigs(), sessionStorage).configureFunction
        )
        runAs(
            ClubAuth.userRunAsAuthProviderName, authConfig.getRunAsConfigs()
        ) { userType, userId ->
            when (userType) {
                ClubUserType.User -> {
                    runBlocking {
                        userRepository.getById(userId)
                    }.let { RunAsUser(userId, it.account!!, userType, it.roles!!) }
                }

                else -> throw InternalServerException(
                    InfraResponseCode.UNSUPPORTED_OPERATION_ERROR,
                    "UserType $userType doesn't support runas"
                )
            }
        }
    }
}