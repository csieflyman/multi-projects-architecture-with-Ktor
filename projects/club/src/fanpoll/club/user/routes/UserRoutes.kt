/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.user.routes

import fanpoll.club.ClubAuth
import fanpoll.club.ClubConst
import fanpoll.club.ClubKoinContext
import fanpoll.club.database.exposed.ClubDatabase
import fanpoll.club.user.dtos.*
import fanpoll.club.user.services.UserJoinClubService
import fanpoll.club.user.services.UserService
import fanpoll.infra.auth.authorize
import fanpoll.infra.base.location.UUIDEntityIdLocation
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.respond
import fanpoll.infra.database.exposed.util.queryDB
import fanpoll.infra.openapi.route.dynamicQuery
import fanpoll.infra.openapi.route.post
import fanpoll.infra.openapi.route.postWithLocation
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
import java.util.*


fun Routing.userRoutes() {

    val userService = ClubKoinContext.koin.get<UserService>()
    val userJoinClubService = ClubKoinContext.koin.get<UserJoinClubService>()
    val clubDatabase = ClubKoinContext.koin.get<Database>(named(ClubDatabase.Club.name))

    route("${ClubConst.urlRootPath}/users") {

        authorize(ClubAuth.Root) {

            post<CreateUserForm, UUID>(UserOpenApi.CreateUser) { form ->
                val id = userService.createUser(form)
                call.respond(DataResponseDTO.uuid(id))
            }
        }

        authorize(ClubAuth.Admin) {

            put<UUIDEntityIdLocation, UpdateUserForm, Unit>(UserOpenApi.UpdateUser) { _, form ->
                userService.updateUser(form)
                call.respond(HttpStatusCode.OK)
            }

            dynamicQuery<UserDTO>(UserOpenApi.FindUsers) { dynamicQuery ->
                call.respond(dynamicQuery.queryDB<UserDTO>(clubDatabase))
            }
        }

        authorize(ClubAuth.User) {

            put<UpdateUserPasswordForm, Unit>("/myPassword", UserOpenApi.UpdateMyPassword) { form ->
                form.userId = call.sessions.get<UserSession>()!!.userId
                userService.updatePassword(form)
                call.respond(CodeResponseDTO.OK)
            }

            postWithLocation<UserJoinClubLocation, UserJoinClubForm, Unit>(UserOpenApi.UserJoinClub) { _, form ->
                userJoinClubService.joinClub(form.toUserJoinedClub())
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}