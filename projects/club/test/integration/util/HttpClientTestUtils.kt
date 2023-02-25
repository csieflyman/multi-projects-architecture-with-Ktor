/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.util

import fanpoll.club.ClubConfig
import fanpoll.club.ClubConst
import fanpoll.infra.ProjectManager
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.base.httpclient.bodyAsTextBlocking
import fanpoll.infra.base.json.json
import fanpoll.infra.base.response.*
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.*

val projectConfig = ProjectManager.loadConfig<ClubConfig>(ClubConst.projectId)

fun getServiceApiKey(principalSource: PrincipalSource): String = projectConfig.auth.getServiceAuthConfigs()
    .first { it.principalSource == principalSource }.apiKey

fun getRunAsKey(principalSource: PrincipalSource): String = projectConfig.auth.getRunAsConfigs()
    .first { it.principalSource == principalSource }.runAsKey

fun mergeRootPath(path: String) = ClubConst.urlRootPath + path

fun HttpResponse.jsonObject(): JsonObject = json.parseToJsonElement(bodyAsTextBlocking()).jsonObject

fun HttpResponse.code(): ResponseCode = jsonObject()["code"]?.let { json.decodeFromJsonElement(it) }
    ?: InfraResponseCode.OK

fun HttpResponse.message(): String? = jsonObject()["message"]?.jsonPrimitive?.content

fun HttpResponse.dataJson(): JsonElement? = jsonObject()["data"]

fun HttpResponse.dataJsonObject(): JsonObject =
    dataJson()?.jsonObject ?: error("response data is neither exist or a json object")

fun HttpResponse.dataJsonArray(): JsonArray =
    dataJson()?.jsonArray ?: error("response data is neither exist or a json array")

inline fun <reified T> HttpResponse.data(): T = dataJsonObject().let { json.decodeFromJsonElement(it) }

inline fun <reified T> HttpResponse.dataList(): List<T> = dataJsonArray().map { json.decodeFromJsonElement(it) }

fun HttpResponse.response(): ResponseDTO = json.decodeFromJsonElement(jsonObject())

fun HttpResponse.dataResponse(): DataResponseDTO = json.decodeFromJsonElement(jsonObject())

fun HttpResponse.pagingDataResponse(): PagingDataResponseDTO = json.decodeFromJsonElement(jsonObject())

fun HttpResponse.errorResponse(): ErrorResponseDTO = json.decodeFromJsonElement(jsonObject())
