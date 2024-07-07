/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.release.app.routes

import fanpoll.infra.auth.authorize
import fanpoll.infra.auth.login.ClientVersionCheckResult
import fanpoll.infra.base.location.Location
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.respond
import fanpoll.infra.database.exposed.util.queryDB
import fanpoll.infra.openapi.route.dynamicQuery
import fanpoll.infra.openapi.route.getWithLocation
import fanpoll.infra.openapi.route.post
import fanpoll.infra.openapi.route.put
import fanpoll.infra.release.app.domain.AppOS
import fanpoll.infra.release.app.domain.AppVersion
import fanpoll.infra.release.app.dtos.AppReleaseDTO
import fanpoll.infra.release.app.dtos.CreateAppReleaseForm
import fanpoll.infra.release.app.dtos.UpdateAppReleaseForm
import fanpoll.infra.release.app.service.AppReleaseChecker
import fanpoll.infra.release.app.service.AppReleaseService
import fanpoll.ops.OpsAuth
import io.ktor.server.application.call
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Route.appReleaseRoute() {

    val appReleaseService by inject<AppReleaseService>()
    val appReleaseChecker by inject<AppReleaseChecker>()

    route("/app") {

        authorize(OpsAuth.AppTeam) {

            post<CreateAppReleaseForm, Unit>(AppReleaseOpenApi.CreateAppRelease) { form ->
                appReleaseService.create(form.toEntity())
                call.respond(CodeResponseDTO.OK)
            }

            put<UpdateAppReleaseForm, Unit>(AppReleaseOpenApi.UpdateAppRelease) { form ->
                appReleaseService.update(form.toEntity())
                call.respond(CodeResponseDTO.OK)
            }

            dynamicQuery<AppReleaseDTO>(AppReleaseOpenApi.FindAppReleases) { dynamicQuery ->
                call.respond(dynamicQuery.queryDB<AppReleaseDTO>())
            }

            getWithLocation<CheckAppReleaseLocation, CheckAppReleaseResponse>(AppReleaseOpenApi.CheckAppRelease) { location ->
                val result = appReleaseChecker.check(AppVersion(location.appId, location.os, location.verName))
                call.respond(DataResponseDTO(CheckAppReleaseResponse(result)))
            }
        }
    }
}

@OptIn(KtorExperimentalLocationsAPI::class)
@io.ktor.server.locations.Location("/check")
data class CheckAppReleaseLocation(val appId: String, val os: AppOS, val verName: String) : Location()

@Serializable
data class CheckAppReleaseResponse(val result: ClientVersionCheckResult)