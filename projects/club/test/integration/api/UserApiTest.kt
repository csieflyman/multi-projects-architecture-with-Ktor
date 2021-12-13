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
import fanpoll.club.user.UserDTO
import fanpoll.infra.auth.provider.UserRunAsToken
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.json.toJsonString
import integration.util.clubHandleSecuredRequest
import integration.util.dataJsonArray
import integration.util.dataJsonObject
import integration.util.dataList
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.setBody
import kotlinx.serialization.json.jsonPrimitive
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

val userApiTest: TestApplicationEngine.() -> Unit = {
    val userForm = CreateUserForm(
        "clubUser1@test.com", "123456",
        true, ClubUserRole.Admin, "clubUser100",
        Gender.Male, 2000, "clubUser1@test.com", "0987654321", Lang.zh_TW
    )

    val userId = with(clubHandleSecuredRequest(
        HttpMethod.Post, "/users", ClubAuth.RootSource
    ) {
        setBody(userForm.toJsonString())
    }) {
        assertEquals(HttpStatusCode.OK, response.status())
        val userId = response.dataJsonObject()["id"]?.jsonPrimitive?.content
        assertNotNull(userId)
        UUID.fromString(userId)
    }

    with(clubHandleSecuredRequest(
        HttpMethod.Get, "/users", ClubAuth.Android,
        UserRunAsToken(ClubUserType.User.value, userId)
    ) {
    }) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals(1, response.dataJsonArray().size)
        val userDTO = response.dataList<UserDTO>().first()
        assertEquals(userForm.account, userDTO.account)
    }

    with(clubHandleSecuredRequest(
        HttpMethod.Put, "/users/$userId", ClubAuth.Android,
        UserRunAsToken(ClubUserType.User.value, userId)
    ) {
        setBody(
            UpdateUserForm(userId, enabled = false).toJsonString()
        )
    }) {
        assertEquals(HttpStatusCode.OK, response.status())
    }

    with(clubHandleSecuredRequest(
        HttpMethod.Get, "/users?q_filter=[enabled = false]", ClubAuth.Android,
        UserRunAsToken(ClubUserType.User.value, userId)
    ) {
    }) {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals(1, response.dataJsonArray().size)
        val userDTO = response.dataList<UserDTO>().first()
        assertEquals(userForm.account, userDTO.account)
        assertEquals(false, userDTO.enabled)
    }
}