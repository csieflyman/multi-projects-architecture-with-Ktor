/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.user.routes

import fanpoll.infra.openapi.route.RouteApiOperation
import fanpoll.infra.openapi.schema.Tag

object UserOpenApi {

    private val UserTag = Tag("user")

    val CreateUser = RouteApiOperation("CreateUser", listOf(UserTag))
    val UpdateUser = RouteApiOperation("UpdateUser", listOf(UserTag))
    val FindUsers = RouteApiOperation("FindUsers", listOf(UserTag))
    val UpdateMyPassword = RouteApiOperation("UpdateMyPassword", listOf(UserTag))

    val UserJoinClub = RouteApiOperation("UserJoinClub", listOf(UserTag))
}