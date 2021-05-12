/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.CodeResponseDTO
import fanpoll.infra.DataResponseDTO
import fanpoll.infra.RequestException
import fanpoll.infra.ResponseCode
import fanpoll.infra.app.*
import fanpoll.infra.auth.authorize
import fanpoll.infra.database.queryDB
import fanpoll.infra.openapi.dynamicQuery
import fanpoll.infra.openapi.get
import fanpoll.infra.openapi.post
import fanpoll.infra.openapi.put
import fanpoll.ops.OpsOpenApiOperations.CheckAppRelease
import fanpoll.ops.OpsOpenApiOperations.CreateAppRelease
import fanpoll.ops.OpsOpenApiOperations.FindAppReleases
import fanpoll.ops.OpsOpenApiOperations.HealthCheck
import fanpoll.ops.OpsOpenApiOperations.UpdateAppRelease
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.route
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@OptIn(KtorExperimentalLocationsAPI::class)
fun Routing.ops() {

    route(OpsConst.urlRootPath) {

        authorize(OpsAuth.Root) {

        }

        authorize(OpsAuth.OpsTeam) {

            route("/sysstat") {

                get<Unit>("/healthCheck", HealthCheck) {
                    call.respond(CodeResponseDTO.OK)
                }
            }
        }

        authorize(OpsAuth.AppTeam) {

            route("/app/releases") {
                post<CreateAppReleaseForm, Unit>(CreateAppRelease) { dto ->
                    AppReleaseService.create(dto)
                    call.respond(CodeResponseDTO.OK)
                }

                put<UpdateAppReleaseForm, Unit>(UpdateAppRelease) { dto ->
                    AppReleaseService.update(dto)
                    call.respond(CodeResponseDTO.OK)
                }

                dynamicQuery<AppReleaseDTO>(FindAppReleases) { dynamicQuery ->
                    call.respond(dynamicQuery.queryDB<AppReleaseDTO>())
                }

                get<Unit>("/check", CheckAppRelease) {
                    val appId = call.request.queryParameters["appId"] ?: throw RequestException(
                        ResponseCode.REQUEST_BAD_QUERY, "appId parameter is missing"
                    )
                    val verName = call.request.queryParameters["verName"] ?: throw RequestException(
                        ResponseCode.REQUEST_BAD_QUERY, "parameter verName is missing"
                    )
                    call.respond(
                        DataResponseDTO(
                            JsonObject(
                                mapOf(
                                    "result" to JsonPrimitive(AppReleaseService.check(AppVersion(appId, verName)).name)
                                )
                            )
                        )
                    )
                }
            }
        }

    }
}