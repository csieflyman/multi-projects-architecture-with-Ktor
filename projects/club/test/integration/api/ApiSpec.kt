/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.api

import fanpoll.club.clubMain
import fanpoll.infra.base.json.json
import fanpoll.infra.main
import integration.util.SinglePostgreSQLContainer
import integration.util.SingleRedisContainer
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
import mu.KotlinLogging
import org.koin.test.KoinTest

@OptIn(ExperimentalKotest::class)
class ApiSpec : KoinTest, FunSpec({

    val logger = KotlinLogging.logger {}

    lateinit var testApplication: TestApplication
    lateinit var client: HttpClient

    beforeSpec {
        testApplication = TestApplication {
            application {
                main {
                    listOf(SinglePostgreSQLContainer, SingleRedisContainer).forEach {
                        it.configure(this)
                    }
                }
                clubMain()
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