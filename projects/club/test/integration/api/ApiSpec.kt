/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.api

import fanpoll.club.ClubConfig
import fanpoll.club.ClubConst
import fanpoll.club.clubProject
import fanpoll.infra.ProjectManager
import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.config.ApplicationConfigLoader
import fanpoll.infra.config.MyApplicationConfig
import fanpoll.infra.main
import io.github.config4k.extract
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.common.ExperimentalKotest
import io.kotest.core.NamedTag
import io.kotest.core.spec.style.FunSpec
import io.ktor.client.HttpClient
import io.ktor.client.plugins.Charsets
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.TestApplication
import org.koin.test.KoinTest
import testcontainers.PostgresSQLContainerManager
import testcontainers.RedisContainerManager

@OptIn(ExperimentalKotest::class)
class ApiSpec : KoinTest, FunSpec({

    val logger = KotlinLogging.logger {}

    lateinit var testApplication: TestApplication
    lateinit var client: HttpClient

    lateinit var appConfig: MyApplicationConfig
    lateinit var projectConfig: ClubConfig

    fun initTestContainers() {
        appConfig = ApplicationConfigLoader.load()
        val infraDatabaseConfig = appConfig.infra.databases.infra
        val infraDBContainer = PostgresSQLContainerManager.create("infra", hikariConfig = infraDatabaseConfig.hikari)
        infraDatabaseConfig.hikari = infraDatabaseConfig.hikari.copy(jdbcUrl = infraDBContainer.jdbcUrl)

        projectConfig = ProjectManager.loadConfig(ClubConst.projectId).extract<ClubConfig>()
        val projectDatabaseConfig = projectConfig.databases.club
        val projectDBContainer = PostgresSQLContainerManager.create(ClubConst.projectId, hikariConfig = projectDatabaseConfig.hikari)
        projectDatabaseConfig.hikari = projectDatabaseConfig.hikari.copy(jdbcUrl = projectDBContainer.jdbcUrl)

        val redisConfig = appConfig.infra.redis
        val redisContainer = RedisContainerManager.create(ClubConst.projectId, redisConfig = redisConfig)
        appConfig.infra.redis = redisConfig.copy(port = redisContainer.firstMappedPort)
    }

    beforeSpec {
        initTestContainers()

        testApplication = TestApplication {
            application {
                main(appConfig)
                clubProject(projectConfig)
            }
        }

        client = testApplication.createClient {
            install(ContentNegotiation) {
                json(json)
            }

            install(Logging) {
                this.logger = Logger.DEFAULT
                level = LogLevel.NONE
            }

            Charsets {
                register(Charsets.UTF_8)
            }
        }

        testApplication.start()
    }

    afterSpec {
        testApplication.stop()
    }

    tags(NamedTag("api"))

    context("api_user").config(tags = setOf(NamedTag("api_user"))) {
        logger.info { "========== User API Test Begin ==========" }
        userApiTest(client)
        logger.info { "========== User API Test End ==========" }
    }

    context("api_login").config(tags = setOf(NamedTag("api_login"))) {
        logger.info { "========== Login API Test Begin ==========" }
        loginApiTest(client)
        logger.info { "========== Login API Test End ==========" }
    }

    context("api_notification").config(tags = setOf(NamedTag("api_notification"))) {
        logger.info { "========== Notification API Test Begin ==========" }
        notificationApiTest(client)
        logger.info { "========== Notification API Test End ==========" }
    }
})