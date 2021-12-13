/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.util

import fanpoll.club.ClubConfig
import fanpoll.club.ClubConst
import fanpoll.infra.ProjectManager
import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.provider.UserRunAsAuthProvider
import fanpoll.infra.auth.provider.UserRunAsToken
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest

val projectConfig = ProjectManager.loadConfig<ClubConfig>(ClubConst.projectId)

fun TestApplicationEngine.clubHandleSecuredRequest(
    method: HttpMethod,
    uri: String,
    principalSource: PrincipalSource,
    userRunAsToken: UserRunAsToken? = null,
    setup: TestApplicationRequest.() -> Unit = {}
): TestApplicationCall = handleRequest {
    this.uri = ClubConst.urlRootPath + uri
    this.method = method

    addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())

    if (method != HttpMethod.Get || method != HttpMethod.Delete)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    if (userRunAsToken == null) {
        addHeader(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(principalSource))
    } else {
        addHeader(AuthConst.API_KEY_HEADER_NAME, getRunAsKey(principalSource))
        addHeader(UserRunAsAuthProvider.RUN_AS_TOKEN_HEADER_NAME, userRunAsToken.value)
    }

    setup()
}

private fun getServiceApiKey(principalSource: PrincipalSource): String = projectConfig.auth.getServiceAuthConfigs()
    .first { it.principalSource == principalSource }.apiKey

private fun getRunAsKey(principalSource: PrincipalSource): String = projectConfig.auth.getRunAsConfigs()
    .first { it.principalSource == principalSource }.runAsKey