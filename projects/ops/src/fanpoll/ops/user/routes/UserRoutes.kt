/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.user.routes

import fanpoll.infra.auth.authorize
import fanpoll.infra.base.location.UUIDEntityIdLocation
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.respond
import fanpoll.infra.database.exposed.util.queryDB
import fanpoll.infra.openapi.route.dynamicQuery
import fanpoll.infra.openapi.route.post
import fanpoll.infra.openapi.route.put
import fanpoll.infra.session.UserSession
import fanpoll.ops.OpsAuth
import fanpoll.ops.OpsConst
import fanpoll.ops.OpsKoinContext
import fanpoll.ops.database.exposed.OpsDatabase
import fanpoll.ops.user.dtos.CreateUserForm
import fanpoll.ops.user.dtos.UpdateUserForm
import fanpoll.ops.user.dtos.UpdateUserPasswordForm
import fanpoll.ops.user.dtos.UserDTO
import fanpoll.ops.user.services.UserService
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

    val userService = OpsKoinContext.koin.get<UserService>()
    val opsDatabase = OpsKoinContext.koin.get<Database>(named(OpsDatabase.Ops.name))

    route("${OpsConst.urlRootPath}/users") {

        authorize(OpsAuth.Root) {

            post<CreateUserForm, UUID>(UserOpenApi.CreateUser) { form ->
                val id = userService.createUser(form)
                call.respond(DataResponseDTO.uuid(id))
            }

            put<UUIDEntityIdLocation, UpdateUserForm, Unit>(UserOpenApi.UpdateUser) { _, form ->
                userService.updateUser(form)
                call.respond(HttpStatusCode.OK)
            }

            dynamicQuery<UserDTO>(UserOpenApi.FindUsers) { dynamicQuery ->
                call.respond(dynamicQuery.queryDB<UserDTO>(opsDatabase))
            }
        }

        authorize(OpsAuth.User) {

            put<UpdateUserPasswordForm, Unit>("/myPassword", UserOpenApi.UpdateMyPassword) { form ->
                form.userId = call.sessions.get<UserSession>()!!.userId
                userService.updatePassword(form)
                call.respond(CodeResponseDTO.OK)
            }
        }
    }
}