/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.api

import fanpoll.club.ClubAuth
import fanpoll.club.ClubUserRole
import fanpoll.club.ClubUserType
import fanpoll.club.user.CreateUserForm
import fanpoll.club.user.Gender
import fanpoll.club.user.UpdateUserForm
import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.login.AppLoginForm
import fanpoll.infra.auth.login.AppLoginResponse
import fanpoll.infra.auth.provider.UserRunAsToken
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.json.toJsonString
import fanpoll.infra.base.response.InfraResponseCode
import integration.util.clubHandleSecuredRequest
import integration.util.code
import integration.util.data
import integration.util.dataJsonObject
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.setBody
import kotlinx.serialization.json.jsonPrimitive
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

val loginApiTest: suspend TestApplicationEngine.(FunSpecContainerScope) -> Unit = { context ->

    lateinit var userId: UUID
    lateinit var runAsToken: UserRunAsToken
    lateinit var sessionId: String

    val userForm = CreateUserForm(
        "loginUser@test.com", "123456",
        true, ClubUserRole.Admin, "loginUser",
        Gender.Male, 2000, "loginUser@test.com", "0987654321", Lang.zh_TW
    )

    val deviceId1 = UUID.randomUUID()
    val loginForm = AppLoginForm(
        userForm.account, userForm.password, null,
        deviceId1, "pushToken", "Android 9.0"
    )

    context.test("user login success") {
        with(clubHandleSecuredRequest(
            HttpMethod.Post, "/users", ClubAuth.RootSource
        ) {
            setBody(userForm.toJsonString())
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
            val userIdStr = response.dataJsonObject()["id"]?.jsonPrimitive?.content
            assertNotNull(userIdStr)

            userId = UUID.fromString(userIdStr)
            runAsToken = UserRunAsToken(ClubUserType.User.value, userId)
        }

        with(clubHandleSecuredRequest(
            HttpMethod.Post, "/login", ClubAuth.Android
        ) {
            setBody(loginForm.toJsonString())
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
            val appLoginResponse = response.data<AppLoginResponse>()
            assertNotNull(appLoginResponse.sid)

            sessionId = appLoginResponse.sid!!
        }
    }

    context.test("account doesn't exist should login failed") {
        with(clubHandleSecuredRequest(
            HttpMethod.Post, "/login", ClubAuth.Android
        ) {
            setBody(loginForm.copy(account = "inExistAccount@test.com").toJsonString())
        }) {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
            assertEquals(InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED, response.code())
        }
    }

    context.test("incorrect password should login failed") {
        with(clubHandleSecuredRequest(
            HttpMethod.Post, "/login", ClubAuth.Android
        ) {
            setBody(loginForm.copy(password = "incorrect password").toJsonString())
        }) {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
            assertEquals(InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED, response.code())
        }
    }

    context.test("user logout without sessionId") {
        with(clubHandleSecuredRequest(
            HttpMethod.Post, "/login", ClubAuth.Android
        ) {
            setBody(loginForm.toJsonString())
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    context.test("user logout with invalid sessionId") {
        with(clubHandleSecuredRequest(
            HttpMethod.Post, "/login", ClubAuth.Android
        ) {
            addHeader(AuthConst.SESSION_ID_HEADER_NAME, "invalid sessionId")
            setBody(loginForm.toJsonString())
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    context.test("user logout success") {
        with(clubHandleSecuredRequest(
            HttpMethod.Post, "/login", ClubAuth.Android
        ) {
            addHeader(AuthConst.SESSION_ID_HEADER_NAME, sessionId)
            setBody(loginForm.toJsonString())
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    context.test("disabled account should login failed") {

        with(clubHandleSecuredRequest(
            HttpMethod.Put, "/users/$userId", ClubAuth.Android, runAsToken
        ) {
            setBody(UpdateUserForm(userId, enabled = false).toJsonString())
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
        }

        with(clubHandleSecuredRequest(
            HttpMethod.Post, "/login", ClubAuth.Android
        ) {
            setBody(loginForm.toJsonString())
        }) {
            assertEquals(HttpStatusCode.Forbidden, response.status())
            assertEquals(InfraResponseCode.AUTH_PRINCIPAL_DISABLED, response.code())
        }
    }
}