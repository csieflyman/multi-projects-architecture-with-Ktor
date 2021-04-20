/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.*
import fanpoll.infra.app.*
import fanpoll.infra.auth.authorize
import fanpoll.infra.database.dynamicDBQuery
import fanpoll.infra.openapi.support.dynamicQuery
import fanpoll.infra.openapi.support.get
import fanpoll.infra.openapi.support.post
import fanpoll.infra.openapi.support.put
import fanpoll.ops.OpsOpenApiRoutes.CheckAppRelease
import fanpoll.ops.OpsOpenApiRoutes.CreateAppRelease
import fanpoll.ops.OpsOpenApiRoutes.FindAppReleases
import fanpoll.ops.OpsOpenApiRoutes.HealthCheck
import fanpoll.ops.OpsOpenApiRoutes.UpdateAppRelease
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.route
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@OptIn(KtorExperimentalLocationsAPI::class)
fun Routing.operations() {

    route(OpsConst.urlRootPath) {

        authorize(OpsAuth.Root) {

        }

        authorize(OpsAuth.OperationsTeam) {

            route("/sysstat") {

                get<Unit>("/healthCheck", HealthCheck) {
                    call.respond(HttpStatusResponse.OK)
                }
            }
        }

        authorize(OpsAuth.AppTeam) {

            route("/app/releases") {

                post<CreateAppReleaseForm, Unit>(CreateAppRelease) { dto ->
                    AppReleaseService.create(dto)
                    call.respond(HttpStatusResponse.OK)
                }

                put<UpdateAppReleaseForm, Unit>(UpdateAppRelease) { dto ->
                    AppReleaseService.update(dto)
                    call.respond(HttpStatusResponse.OK)
                }

                dynamicQuery<AppReleaseDTO>(FindAppReleases) {
                    call.respondMyResponse(call.request.dynamicDBQuery<AppReleaseDTO>())
                }

                get<Unit>("/check", CheckAppRelease) {
                    val appId = call.request.queryParameters["appId"] ?: throw RequestException(
                        ResponseCode.REQUEST_BAD_QUERY, "appId parameter is missing"
                    )
                    val verName = call.request.queryParameters["verName"] ?: throw RequestException(
                        ResponseCode.REQUEST_BAD_QUERY, "parameter verName is missing"
                    )
                    call.respond(
                        DataResponse(
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