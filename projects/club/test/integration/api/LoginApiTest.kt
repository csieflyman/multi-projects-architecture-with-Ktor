/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.api

import fanpoll.club.ClubAuth
import fanpoll.club.ClubUserRole
import fanpoll.club.ClubUserType
import fanpoll.club.features.CreateUserForm
import fanpoll.club.features.Gender
import fanpoll.club.features.UpdateUserForm
import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.login.AppLoginForm
import fanpoll.infra.auth.login.AppLoginResponse
import fanpoll.infra.auth.provider.UserRunAsAuthProvider
import fanpoll.infra.auth.provider.UserRunAsToken
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.response.InfraResponseCode
import integration.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.jsonPrimitive
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

val loginApiTest: suspend FunSpecContainerScope.(HttpClient) -> Unit = { client ->

    val logger = KotlinLogging.logger {}

    lateinit var userId: UUID
    lateinit var runAsToken: UserRunAsToken
    lateinit var sessionId: String

    val loginUser1Form = CreateUserForm(
        "login-user_1@test.com", "123456",
        true, ClubUserRole.Admin, "login-user_1",
        Gender.Male, 2000, "login-user_1@test.com", "0987654321", Lang.zh_TW
    )

    val loginUser1DeviceId1 = UUID.randomUUID()
    val loginForm = AppLoginForm(
        loginUser1Form.account, loginUser1Form.password, null,
        loginUser1DeviceId1, "pushToken", "Android 9.0"
    )

    test("user login success") {
        val createUserResponse = client.post(mergeRootPath("/users")) {
            contentType(ContentType.Application.Json)
            header(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(ClubAuth.RootSource))
            setBody(loginUser1Form)
        }
        assertEquals(HttpStatusCode.OK, createUserResponse.status)
        val userIdStr = createUserResponse.dataJsonObject()["id"]?.jsonPrimitive?.content
        assertNotNull(userIdStr)

        userId = UUID.fromString(userIdStr)
        runAsToken = UserRunAsToken(ClubUserType.User.value, userId)

        val loginResponse = client.post(mergeRootPath("/login")) {
            contentType(ContentType.Application.Json)
            header(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(ClubAuth.Android))
            setBody(loginForm)
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val appLoginResponse = loginResponse.data<AppLoginResponse>()
        assertNotNull(appLoginResponse.sid)

        sessionId = appLoginResponse.sid!!
    }

    test("account doesn't exist should login failed") {
        val response = client.post(mergeRootPath("/login")) {
            contentType(ContentType.Application.Json)
            header(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(ClubAuth.Android))
            setBody(loginForm.copy(account = "inExistAccount@test.com"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED, response.code())
    }

    test("incorrect password should login failed") {
        val response = client.post(mergeRootPath("/login")) {
            contentType(ContentType.Application.Json)
            header(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(ClubAuth.Android))
            setBody(loginForm.copy(password = "incorrect password"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED, response.code())
    }

    test("user logout without sessionId") {
        val response = client.post(mergeRootPath("/logout")) {
            header(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(ClubAuth.Android))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(InfraResponseCode.AUTH_SESSION_NOT_FOUND_OR_EXPIRED, response.code())
    }

    test("user logout with wrong sessionId") {
        val response = client.post(mergeRootPath("/logout")) {
            header(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(ClubAuth.Android))
            header(AuthConst.SESSION_ID_HEADER_NAME, "wrong sessionId")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(InfraResponseCode.AUTH_SESSION_NOT_FOUND_OR_EXPIRED, response.code())
    }

    test("user logout success") {
        val response = client.post(mergeRootPath("/logout")) {
            header(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(ClubAuth.Android))
            header(AuthConst.SESSION_ID_HEADER_NAME, sessionId)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    test("disabled account should login failed") {
        val updateUserResponse = client.put(mergeRootPath("/users/$userId")) {
            contentType(ContentType.Application.Json)
            header(AuthConst.API_KEY_HEADER_NAME, getRunAsKey(ClubAuth.Android))
            header(UserRunAsAuthProvider.RUN_AS_TOKEN_HEADER_NAME, runAsToken.value)
            setBody(UpdateUserForm(userId, enabled = false))
        }
        assertEquals(HttpStatusCode.OK, updateUserResponse.status)

        val loginResponse = client.post(mergeRootPath("/login")) {
            contentType(ContentType.Application.Json)
            header(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(ClubAuth.Android))
            setBody(loginForm)
        }
        assertEquals(HttpStatusCode.Forbidden, loginResponse.status)
        assertEquals(InfraResponseCode.AUTH_PRINCIPAL_DISABLED, loginResponse.code())
    }
}