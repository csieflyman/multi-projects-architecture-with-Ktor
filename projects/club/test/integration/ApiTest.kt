/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration

import fanpoll.club.*
import fanpoll.club.user.CreateUserForm
import fanpoll.club.user.Gender
import fanpoll.infra.ProjectManager
import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.json.toJsonString
import fanpoll.infra.main
import io.kotest.core.spec.style.FunSpec
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import mu.KotlinLogging
import org.koin.test.KoinTest
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals

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

            with(clubHandleSecuredRequest(HttpMethod.Post, "/users", ClubAuth.RootSource) {
                setBody(
                    CreateUserForm(
                        "clubUser1@test.com", "123456",
                        true, ClubUserRole.Member, "clubUser100",
                        Gender.Male, 2000, "clubUser1@test.com", "0987654321", Lang.zh_TW
                    ).toJsonString()
                )
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
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
    setup: TestApplicationRequest.() -> Unit = {}
): TestApplicationCall = handleRequest {
    this.uri = ClubConst.urlRootPath + uri
    this.method = method
    addHeader(HttpHeaders.Accept, ContentType.Application.Json.contentType)
    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.contentType)
    addHeader(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(principalSource))
    setup()
}

private fun getServiceApiKey(principalSource: PrincipalSource): String = projectConfig.auth.getServiceAuthConfigs()
    .first { it.principalSource == principalSource }.apiKey