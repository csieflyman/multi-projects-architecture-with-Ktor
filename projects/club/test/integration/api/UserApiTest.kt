/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.api

import fanpoll.club.ClubAuth
import fanpoll.club.ClubUserRole
import fanpoll.club.ClubUserType
import fanpoll.club.features.*
import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.provider.UserRunAsAuthProvider
import fanpoll.infra.auth.provider.UserRunAsToken
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.response.InfraResponseCode
import integration.util.*
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

val userApiTest: suspend FunSpecContainerScope.(HttpClient) -> Unit = { client ->

    val logger = KotlinLogging.logger {}

    lateinit var userId: UUID
    lateinit var runAsToken: UserRunAsToken

    val userAdmin1Form = CreateUserForm(
        "user-admin_1@test.com", "123456",
        true, ClubUserRole.Admin, "user-admin_1",
        Gender.Male, 2000, "user-admin_1@test.com", "0987654321", Lang.zh_TW
    )

    test("create admin user") {
        val createUserResponse = client.post(mergeRootPath("/users")) {
            contentType(ContentType.Application.Json)
            header(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(ClubAuth.RootSource))
            setBody(userAdmin1Form)
        }
        assertEquals(HttpStatusCode.OK, createUserResponse.status)
        val userIdStr = createUserResponse.dataJsonObject()["id"]?.jsonPrimitive?.content
        assertNotNull(userIdStr)

        userId = UUID.fromString(userIdStr)
        runAsToken = UserRunAsToken(ClubUserType.User.value, userId)

        val getAdminResponse = client.get(mergeRootPath("/users")) {
            header(AuthConst.API_KEY_HEADER_NAME, getRunAsKey(ClubAuth.Android))
            header(UserRunAsAuthProvider.RUN_AS_TOKEN_HEADER_NAME, runAsToken.value)
        }
        assertEquals(HttpStatusCode.OK, getAdminResponse.status)
        assertEquals(1, getAdminResponse.dataJsonArray().size)
        val userDTO = getAdminResponse.dataList<UserDTO>().first()
        assertEquals(userAdmin1Form.account, userDTO.account)
    }

    test("create user with duplicated account") {
        val createUserResponse = client.post(mergeRootPath("/users")) {
            contentType(ContentType.Application.Json)
            header(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(ClubAuth.RootSource))
            setBody(userAdmin1Form)
        }
        assertEquals(HttpStatusCode.Conflict, createUserResponse.status)
        assertEquals(InfraResponseCode.ENTITY_ALREADY_EXISTS, createUserResponse.code())
    }

    test("update user data") {
        val updateUserResponse = client.put(mergeRootPath("/users/$userId")) {
            contentType(ContentType.Application.Json)
            header(AuthConst.API_KEY_HEADER_NAME, getRunAsKey(ClubAuth.Android))
            header(UserRunAsAuthProvider.RUN_AS_TOKEN_HEADER_NAME, runAsToken.value)
            setBody(UpdateUserForm(userId, enabled = false))
        }
        assertEquals(HttpStatusCode.OK, updateUserResponse.status)

        val getUserResponse = client.get(mergeRootPath("/users?q_filter=[enabled = false]")) {
            header(AuthConst.API_KEY_HEADER_NAME, getRunAsKey(ClubAuth.Android))
            header(UserRunAsAuthProvider.RUN_AS_TOKEN_HEADER_NAME, runAsToken.value)
        }

        assertEquals(HttpStatusCode.OK, getUserResponse.status)
        assertEquals(1, getUserResponse.dataJsonArray().size)
        val userDTO = getUserResponse.dataList<UserDTO>().first()
        assertEquals(userAdmin1Form.account, userDTO.account)
        assertEquals(false, userDTO.enabled)
    }

    test("update my password with incorrect old password") {
        val response = client.put(mergeRootPath("/users/myPassword")) {
            contentType(ContentType.Application.Json)
            header(AuthConst.API_KEY_HEADER_NAME, getRunAsKey(ClubAuth.Android))
            header(UserRunAsAuthProvider.RUN_AS_TOKEN_HEADER_NAME, runAsToken.value)
            setBody(UpdateUserPasswordForm("incorrectOldPassword", "newPassword"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(InfraResponseCode.AUTH_BAD_PASSWORD, response.code())
    }

    test("update my password with correct old password") {
        val response = client.put(mergeRootPath("/users/myPassword")) {
            contentType(ContentType.Application.Json)
            header(AuthConst.API_KEY_HEADER_NAME, getRunAsKey(ClubAuth.Android))
            header(UserRunAsAuthProvider.RUN_AS_TOKEN_HEADER_NAME, runAsToken.value)
            setBody(UpdateUserPasswordForm(userAdmin1Form.password, "newPassword"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}