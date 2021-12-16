/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.api

import fanpoll.club.ClubAuth
import fanpoll.club.ClubUserRole
import fanpoll.club.ClubUserType
import fanpoll.club.features.*
import fanpoll.infra.auth.provider.UserRunAsToken
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.json.toJsonString
import fanpoll.infra.base.response.InfraResponseCode
import integration.util.*
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.setBody
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

val userApiTest: suspend TestApplicationEngine.(FunSpecContainerScope) -> Unit = { context ->

    val logger = KotlinLogging.logger {}

    lateinit var userId: UUID
    lateinit var runAsToken: UserRunAsToken

    val userAdmin1Form = CreateUserForm(
        "user-admin_1@test.com", "123456",
        true, ClubUserRole.Admin, "user-admin_1",
        Gender.Male, 2000, "user-admin_1@test.com", "0987654321", Lang.zh_TW
    )

    context.test("create admin user") {
        with(clubHandleSecuredRequest(
            HttpMethod.Post, "/users", ClubAuth.RootSource
        ) {
            setBody(userAdmin1Form.toJsonString())
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
            val userIdStr = response.dataJsonObject()["id"]?.jsonPrimitive?.content
            assertNotNull(userIdStr)

            userId = UUID.fromString(userIdStr)
            runAsToken = UserRunAsToken(ClubUserType.User.value, userId)
        }

        with(clubHandleSecuredRequest(
            HttpMethod.Get, "/users", ClubAuth.Android, runAsToken
        ) {
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(1, response.dataJsonArray().size)
            val userDTO = response.dataList<UserDTO>().first()
            assertEquals(userAdmin1Form.account, userDTO.account)
        }
    }

    context.test("create user with duplicated account") {
        with(clubHandleSecuredRequest(
            HttpMethod.Post, "/users", ClubAuth.RootSource
        ) {
            setBody(userAdmin1Form.toJsonString())
        }) {
            assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
            assertEquals(InfraResponseCode.ENTITY_ALREADY_EXISTS, response.code())
        }
    }

    context.test("update user data") {
        with(clubHandleSecuredRequest(
            HttpMethod.Put, "/users/$userId", ClubAuth.Android, runAsToken
        ) {
            setBody(UpdateUserForm(userId, enabled = false).toJsonString())
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
        }

        with(clubHandleSecuredRequest(
            HttpMethod.Get, "/users?q_filter=[enabled = false]", ClubAuth.Android, runAsToken
        ) {
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(1, response.dataJsonArray().size)
            val userDTO = response.dataList<UserDTO>().first()
            assertEquals(userAdmin1Form.account, userDTO.account)
            assertEquals(false, userDTO.enabled)
        }
    }

    context.test("update my password with incorrect old password") {
        with(clubHandleSecuredRequest(
            HttpMethod.Put, "/users/myPassword", ClubAuth.Android, runAsToken
        ) {
            setBody(UpdateUserPasswordForm("incorrectOldPassword", "newPassword").toJsonString())
        }) {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
            assertEquals(InfraResponseCode.AUTH_BAD_PASSWORD, response.code())
        }
    }

    context.test("update my password with correct old password") {
        with(clubHandleSecuredRequest(
            HttpMethod.Put, "/users/myPassword", ClubAuth.Android, runAsToken
        ) {
            setBody(UpdateUserPasswordForm(userAdmin1Form.password, "newPassword").toJsonString())
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }
}