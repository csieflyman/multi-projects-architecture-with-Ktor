/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.auth

import fanpoll.infra.auth.login.WebLoginForm
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.openapi.route.RouteApiOperation
import fanpoll.infra.openapi.schema.Tag

object AuthOpenApi {

    private val AuthTag = Tag("auth")

    val Login = RouteApiOperation("Login", listOf(AuthTag)) {

        addErrorResponses(
            InfraResponseCode.AUTH_PRINCIPAL_DISABLED,
            InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED
        )

        addRequestExample(WebLoginForm("tester@test.com", "test123"))
    }
    val Logout = RouteApiOperation("Logout", listOf(AuthTag))
}