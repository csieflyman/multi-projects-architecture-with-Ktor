/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.api

import fanpoll.club.clubMain
import fanpoll.infra.main
import integration.util.TestContainerUtils
import io.kotest.common.ExperimentalKotest
import io.kotest.core.NamedTag
import io.kotest.core.spec.style.FunSpec
import io.ktor.application.Application
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import mu.KotlinLogging
import org.koin.test.KoinTest

@OptIn(ExperimentalKotest::class)
class ApiTest : KoinTest, FunSpec({

    val logger = KotlinLogging.logger {}

    val postgresContainer = TestContainerUtils.createPostgresContainer()
    val redisContainer = TestContainerUtils.createRedisContainer()

    val ktorEngine = TestApplicationEngine(createTestEnvironment()) {}
    val installKtorTestModules: Application.() -> Unit = {
        main {
            TestContainerUtils.replacePostgresConfig(postgresContainer, infra.database!!.hikari)
            TestContainerUtils.replaceRedisConfig(redisContainer, infra.redis!!)
        }
        clubMain()
    }

    beforeSpec {
        logger.info { "========== PostgresSQL Container Start ==========" }
        postgresContainer.start()

        logger.info { "========== Redis Container Start ==========" }
        redisContainer.start()

        logger.info { "========== Ktor Server Start ==========" }
        ktorEngine.start()
        installKtorTestModules(ktorEngine.application)

        logger.info { "========== API Test Begin ==========" }
    }

    afterSpec {
        logger.info { "========== API Test End ==========" }

        logger.info { "========== Ktor Server Stop ==========" }
        ktorEngine.stop(0L, 0L)

        logger.info { "========== Redis Container Stop ==========" }
        redisContainer.stop()

        logger.info { "========== PostgresSQL Container Stop ==========" }
        postgresContainer.stop()
    }

    tags(NamedTag("api"))

    context("api_user").config(tags = setOf(NamedTag("api_user"))) {
        logger.info { "========== User API Test Begin ==========" }
        ktorEngine.userApiTest(this)
        logger.info { "========== User API Test End ==========" }
    }

    context("api_login").config(tags = setOf(NamedTag("api_login"))) {
        logger.info { "========== Login API Test Begin ==========" }
        ktorEngine.loginApiTest(this)
        logger.info { "========== Login API Test End ==========" }
    }

    context("api_notification").config(tags = setOf(NamedTag("api_notification"))) {
        logger.info { "========== Notification API Test Begin ==========" }
        ktorEngine.notificationApiTest(this)
        logger.info { "========== Notification API Test End ==========" }
    }
})