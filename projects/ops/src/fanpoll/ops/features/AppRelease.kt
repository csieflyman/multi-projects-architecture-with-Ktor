/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops.features

import fanpoll.infra.app.*
import fanpoll.infra.auth.ClientVersionCheckResult
import fanpoll.infra.auth.authorize
import fanpoll.infra.base.location.Location
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.respond
import fanpoll.infra.database.util.queryDB
import fanpoll.infra.openapi.dynamicQuery
import fanpoll.infra.openapi.getWithLocation
import fanpoll.infra.openapi.post
import fanpoll.infra.openapi.put
import fanpoll.ops.OpsAuth
import fanpoll.ops.OpsConst
import fanpoll.ops.OpsOpenApi
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.routing.Routing
import io.ktor.routing.route
import org.koin.ktor.ext.inject

fun Routing.opsAppRelease() {

    val appReleaseService by inject<AppReleaseService>()

    route("${OpsConst.urlRootPath}/app/releases") {

        authorize(OpsAuth.AppTeam) {

            post<CreateAppReleaseForm, Unit>(OpsOpenApi.CreateAppRelease) { dto ->
                appReleaseService.create(dto)
                call.respond(CodeResponseDTO.OK)
            }

            put<UpdateAppReleaseForm, Unit>(OpsOpenApi.UpdateAppRelease) { dto ->
                appReleaseService.update(dto)
                call.respond(CodeResponseDTO.OK)
            }

            dynamicQuery<AppReleaseDTO>(OpsOpenApi.FindAppReleases) { dynamicQuery ->
                call.respond(dynamicQuery.queryDB<AppReleaseDTO>())
            }

            getWithLocation<CheckAppReleaseLocation, CheckAppReleaseResponse>(OpsOpenApi.CheckAppRelease) { location ->
                val result = appReleaseService.check(AppVersion(location.appId, location.verName))
                call.respond(DataResponseDTO(CheckAppReleaseResponse(result)))
            }
        }
    }
}

@OptIn(KtorExperimentalLocationsAPI::class)
@io.ktor.locations.Location("/check")
data class CheckAppReleaseLocation(val appId: String, val verName: String) : Location()

data class CheckAppReleaseResponse(val result: ClientVersionCheckResult)