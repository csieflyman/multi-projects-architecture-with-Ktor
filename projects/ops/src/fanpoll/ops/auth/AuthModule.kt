/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.auth

import fanpoll.infra.auth.provider.UserSessionAuthValidator
import fanpoll.infra.auth.provider.service
import fanpoll.infra.session.UserSession
import fanpoll.ops.OpsAuth
import fanpoll.ops.OpsKoinContext
import fanpoll.ops.auth.login.LoginService
import fanpoll.ops.auth.login.UserLoginExposedRepository
import fanpoll.ops.auth.login.UserLoginRepository
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.session
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin

fun Application.loadAuthModule(authConfig: AuthConfig) {
    val application = this
    authentication {
        service(OpsAuth.serviceAuthProviderName, authConfig.getServiceAuthConfigs())
        session<UserSession>(
            OpsAuth.userAuthProviderName,
            UserSessionAuthValidator(authConfig.getUserAuthConfigs(), application.get()).configureFunction
        )
    }

    val infraKoin = getKoin()
    OpsKoinContext.koin.loadModules(listOf(
        module(createdAtStart = true) {
            val userLoginRepository = UserLoginExposedRepository()
            single<UserLoginRepository> { userLoginRepository }
            single { LoginService(userLoginRepository, infraKoin.get(), infraKoin.get()) }
        }
    ))
}