/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration

import fanpoll.club.*
import fanpoll.club.user.CreateUserForm
import fanpoll.club.user.Gender
import fanpoll.club.user.UpdateUserForm
import fanpoll.club.user.UserDTO
import fanpoll.infra.ProjectManager
import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.provider.UserRunAsAuthProvider
import fanpoll.infra.auth.provider.UserRunAsToken
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.json.json
import fanpoll.infra.base.json.toJsonString
import fanpoll.infra.base.response.*
import fanpoll.infra.main
import io.kotest.core.spec.style.FunSpec
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import mu.KotlinLogging
import org.koin.test.KoinTest
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApiTest : KoinTest, FunSpec({

    val logger = KotlinLogging.logger {}

    val postgresImageName = System.getProperty("testcontainers.image.postgres", "postgres")
    // https://kotlinlang.org/docs/whatsnew1530.html#improvements-to-type-inference-for-recursive-generic-types
    // COMPATIBILITY => update intellij kotlin plugin to early access preview 1.6.x
    val postgresContainer = PostgreSQLContainer(DockerImageName.parse(postgresImageName))
        .withDatabaseName("test-db")
        .withUsername("tester")
        .withPassword("test")

    val redisImageName = System.getProperty("testcontainers.image.redis", "redis")
    val redisContainer = GenericContainer(DockerImageName.parse(redisImageName)).withExposedPorts(6379)

    beforeSpec {
        logger.info { "========== PostgreSQL Container Start ==========" }
        postgresContainer.start()
        logger.info { "========== Redis Container Start ==========" }
        redisContainer.start()
    }

    afterSpec {
        logger.info { "========== PostgreSQL Container Stop ==========" }
        postgresContainer.stop()
        logger.info { "========== Redis Container Stop ==========" }
        redisContainer.stop()
    }

    test("Api") {
        logger.info { "========== Club Api Test Begin ==========" }

        val ktorTestModule: Application.() -> Unit = {
            main {
                with(infra.database!!.hikari) {
                    jdbcUrl = postgresContainer.jdbcUrl
                    username = postgresContainer.username
                    password = postgresContainer.password
                }
                with(infra.redis!!) {
                    host = redisContainer.host
                    port = redisContainer.firstMappedPort
                }
            }
            clubMain()
        }

        withTestApplication(ktorTestModule) {

            val userForm = CreateUserForm(
                "clubUser1@test.com", "123456",
                true, ClubUserRole.Admin, "clubUser100",
                Gender.Male, 2000, "clubUser1@test.com", "0987654321", Lang.zh_TW
            )

            val userId = with(clubHandleSecuredRequest(
                HttpMethod.Post, "/users", ClubAuth.RootSource
            ) {
                setBody(userForm.toJsonString())
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                val userId = response.dataJsonObject()["id"]?.jsonPrimitive?.content
                assertNotNull(userId)
                UUID.fromString(userId)
            }

            with(clubHandleSecuredRequest(
                HttpMethod.Get, "/users", ClubAuth.Android,
                UserRunAsToken(ClubUserType.User.value, userId)
            ) {
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(1, response.dataJsonArray().size)
                val userDTO = response.dataList<UserDTO>().first()
                assertEquals(userForm.account, userDTO.account)
            }

            with(clubHandleSecuredRequest(
                HttpMethod.Put, "/users/$userId", ClubAuth.Android,
                UserRunAsToken(ClubUserType.User.value, userId)
            ) {
                setBody(
                    UpdateUserForm(userId, enabled = false).toJsonString()
                )
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
            }

            with(clubHandleSecuredRequest(
                HttpMethod.Get, "/users?q_filter=[enabled = false]", ClubAuth.Android,
                UserRunAsToken(ClubUserType.User.value, userId)
            ) {
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(1, response.dataJsonArray().size)
                val userDTO = response.dataList<UserDTO>().first()
                assertEquals(userForm.account, userDTO.account)
                assertEquals(false, userDTO.enabled)
            }
        }

        logger.info { "========== Club Api Test End ==========" }
    }
})

private val projectConfig = ProjectManager.loadConfig<ClubConfig>(ClubConst.projectId)

private fun TestApplicationEngine.clubHandleSecuredRequest(
    method: HttpMethod,
    uri: String,
    principalSource: PrincipalSource,
    userRunAsToken: UserRunAsToken? = null,
    setup: TestApplicationRequest.() -> Unit = {}
): TestApplicationCall = handleRequest {
    this.uri = ClubConst.urlRootPath + uri
    this.method = method

    addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())

    if (method != HttpMethod.Get || method != HttpMethod.Delete)
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    if (userRunAsToken == null) {
        addHeader(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(principalSource))
    } else {
        addHeader(AuthConst.API_KEY_HEADER_NAME, getRunAsKey(principalSource))
        addHeader(UserRunAsAuthProvider.RUN_AS_TOKEN_HEADER_NAME, userRunAsToken.value)
    }

    setup()
}

private fun getServiceApiKey(principalSource: PrincipalSource): String = projectConfig.auth.getServiceAuthConfigs()
    .first { it.principalSource == principalSource }.apiKey

private fun getRunAsKey(principalSource: PrincipalSource): String = projectConfig.auth.getRunAsConfigs()
    .first { it.principalSource == principalSource }.runAsKey

fun TestApplicationResponse.jsonObject(): JsonObject = content?.let { json.parseToJsonElement(it).jsonObject }
    ?: error("response content is empty or not a json object")

fun TestApplicationResponse.code(): ResponseCode = jsonObject()["code"]?.let { json.decodeFromJsonElement(it) }
    ?: InfraResponseCode.OK

fun TestApplicationResponse.message(): String? = jsonObject()["message"]?.jsonPrimitive?.content

fun TestApplicationResponse.dataJson(): JsonElement? = jsonObject()["data"]

fun TestApplicationResponse.dataJsonObject(): JsonObject =
    dataJson()?.jsonObject ?: error("response data is neither exist or a json object")

fun TestApplicationResponse.dataJsonArray(): JsonArray =
    dataJson()?.jsonArray ?: error("response data is neither exist or a json array")

inline fun <reified T> TestApplicationResponse.data(): T = dataJsonObject().let { json.decodeFromJsonElement(it) }

inline fun <reified T> TestApplicationResponse.dataList(): List<T> = dataJsonArray().map { json.decodeFromJsonElement(it) }

fun TestApplicationResponse.response(): ResponseDTO = json.decodeFromJsonElement(jsonObject())

fun TestApplicationResponse.dataResponse(): DataResponseDTO = json.decodeFromJsonElement(jsonObject())

fun TestApplicationResponse.pagingDataResponse(): PagingDataResponseDTO = json.decodeFromJsonElement(jsonObject())

fun TestApplicationResponse.errorResponse(): ErrorResponseDTO = json.decodeFromJsonElement(jsonObject())