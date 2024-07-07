/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.club.routes

import fanpoll.club.ClubAuth
import fanpoll.club.ClubConst
import fanpoll.club.ClubKoinContext
import fanpoll.club.club.dtos.ClubDTO
import fanpoll.club.club.dtos.CreateClubForm
import fanpoll.club.club.dtos.UpdateClubForm
import fanpoll.club.club.services.ClubService
import fanpoll.club.database.exposed.ClubDatabase
import fanpoll.club.user.dtos.UserDTO
import fanpoll.infra.auth.authorize
import fanpoll.infra.base.location.StringEntityIdLocation
import fanpoll.infra.base.response.respond
import fanpoll.infra.database.exposed.util.queryDB
import fanpoll.infra.openapi.route.dynamicQuery
import fanpoll.infra.openapi.route.post
import fanpoll.infra.openapi.route.put
import fanpoll.infra.session.UserSession
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named

fun Routing.clubRoutes() {

    val clubService = ClubKoinContext.koin.get<ClubService>()
    val clubDatabase = ClubKoinContext.koin.get<Database>(named(ClubDatabase.Club.name))

    route("${ClubConst.urlRootPath}/clubs") {

        authorize(ClubAuth.Admin) {

            post<CreateClubForm, Unit>(ClubOpenApi.CreateClub) { form ->
                form.creatorId = call.sessions.get<UserSession>()!!.userId
                clubService.createClub(form)
                call.respond(HttpStatusCode.OK)
            }
        }

        authorize(ClubAuth.User) {

            put<StringEntityIdLocation, UpdateClubForm, Unit>(ClubOpenApi.UpdateClub) { _, form ->
                form.currentUser = call.sessions.get<UserSession>()!!
                clubService.updateClub(form)
                call.respond(HttpStatusCode.OK)
            }

            dynamicQuery<UserDTO>(ClubOpenApi.FindClubs) { dynamicQuery ->
                call.respond(dynamicQuery.queryDB<ClubDTO>(clubDatabase))
            }
        }
    }
}