/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.club.routes

import fanpoll.infra.openapi.route.RouteApiOperation
import fanpoll.infra.openapi.schema.Tag

object ClubOpenApi {

    private val ClubTag = Tag("club")

    val CreateClub = RouteApiOperation("CreateClub", listOf(ClubTag))
    val UpdateClub = RouteApiOperation("UpdateClub", listOf(ClubTag))
    val FindClubs = RouteApiOperation("FindClubs", listOf(ClubTag))
}