/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.api

import fanpoll.club.clubMain
import fanpoll.infra.main
import integration.util.TestContainerUtils
import integration.util.withTestApplicationInKotestContext
import io.kotest.common.ExperimentalKotest
import io.kotest.core.NamedTag
import io.kotest.core.spec.style.FunSpec
import io.ktor.application.Application
import mu.KotlinLogging
import org.koin.test.KoinTest

@OptIn(ExperimentalKotest::class)
class ApiTest : KoinTest, FunSpec({

    val logger = KotlinLogging.logger {}

    val postgresContainer = TestContainerUtils.createPostgresContainer()
    val redisContainer = TestContainerUtils.createRedisContainer()

    val ktorTestModule: Application.() -> Unit = {
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
    }

    afterSpec {
        logger.info { "========== Redis Container Stop ==========" }
        redisContainer.stop()
        logger.info { "========== PostgresSQL Container Stop ==========" }
        postgresContainer.stop()
    }

    context("api").config(tags = setOf(NamedTag("api"))) {
        logger.info { "========== API Test Begin ==========" }
        withTestApplicationInKotestContext(ktorTestModule) { context ->
            logger.info { "========== User API Test Begin ==========" }
            userApiTest(context)
            logger.info { "========== User API Test End ==========" }

            logger.info { "========== Login API Test ==========" }
            loginApiTest(context)
            logger.info { "========== Login API Test End ==========" }

            logger.info { "========== Notification API Test ==========" }
            notificationApiTest(context)
            logger.info { "========== Notification API Test End ==========" }
        }
        logger.info { "========== API Test End ==========" }
    }
})