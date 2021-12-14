/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.api

import fanpoll.club.clubMain
import fanpoll.infra.main
import integration.util.createPostgresContainer
import integration.util.createRedisContainer
import integration.util.withApplicationSuspend
import io.kotest.core.spec.style.FunSpec
import io.ktor.application.Application
import mu.KotlinLogging
import org.koin.test.KoinTest

class ApiTest : KoinTest, FunSpec({

    val logger = KotlinLogging.logger {}

    val postgresContainer = createPostgresContainer()
    val redisContainer = createRedisContainer()

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
        logger.info { "========== Redis Container Stop2 ==========" }
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