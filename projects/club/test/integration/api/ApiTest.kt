/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.api

import fanpoll.club.clubMain
import fanpoll.infra.main
import integration.util.TestContainerUtils
import integration.util.withApplicationSuspend
import io.kotest.core.spec.style.FunSpec
import io.ktor.application.Application
import mu.KotlinLogging
import org.koin.test.KoinTest

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
        logger.info { "========== PostgresSQL Container Stop ==========" }
        postgresContainer.stop()
        logger.info { "========== Redis Container Stop ==========" }
        redisContainer.stop()
    }

    context("Api") {
        logger.info { "========== Api Test Begin ==========" }
        withApplicationSuspend(ktorTestModule) {
            test("user") {
                userApiTest()
            }
        }
        logger.info { "========== Api Test End ==========" }
    }
})