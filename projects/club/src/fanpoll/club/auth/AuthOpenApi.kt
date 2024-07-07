/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.auth

import fanpoll.club.auth.login.AppLoginForm
import fanpoll.infra.auth.login.AppLoginResponse
import fanpoll.infra.auth.login.ClientVersionCheckResult
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.openapi.route.RouteApiOperation
import fanpoll.infra.openapi.schema.Tag
import fanpoll.infra.openapi.schema.operation.definitions.ExampleObject
import fanpoll.infra.release.app.domain.AppOS

object AuthOpenApi {

    private val AuthTag = Tag("auth")

    val Login = RouteApiOperation("Login", listOf(AuthTag)) {

        addErrorResponses(
            InfraResponseCode.AUTH_PRINCIPAL_DISABLED,
            InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED
        )

        addRequestExample(
            AppLoginForm(
                "tester@test.com", "test123", null,
                AppOS.Android, "device1", "pushToken"
            )
        )

        addResponseExample(
            InfraResponseCode.OK,
            ExampleObject(
                ClientVersionCheckResult.Latest.name, ClientVersionCheckResult.Latest.name, "已是最新版本",
                DataResponseDTO(
                    AppLoginResponse(
                        "club:android:user:421feef3-c1b4-4525-a416-6a11cf6ed9ca:2d7674bb47ec1c58681ce56c49ba9e4d",
                        ClientVersionCheckResult.Latest
                    )
                )
            ),
            ExampleObject(
                ClientVersionCheckResult.ForceUpdate.name, ClientVersionCheckResult.ForceUpdate.name, "必須先更新版本才能繼續使用",
                DataResponseDTO(
                    AppLoginResponse(
                        "club:android:user:421feef3-c1b4-4525-a416-6a11cf6ed9ca:2d7674bb47ec1c58681ce56c49ba9e4d",
                        ClientVersionCheckResult.ForceUpdate
                    )
                )
            )
        )
    }
    val Logout = RouteApiOperation("Logout", listOf(AuthTag))
}