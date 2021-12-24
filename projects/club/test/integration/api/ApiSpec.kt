/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.api

import integration.util.KtorTestBase
import integration.util.SingleKtorTestApplicationEngine
import io.kotest.common.ExperimentalKotest
import io.kotest.core.NamedTag
import mu.KotlinLogging
import org.koin.test.KoinTest

@OptIn(ExperimentalKotest::class)
class ApiSpec : KoinTest, KtorTestBase(
    {
        val logger = KotlinLogging.logger {}

        beforeSpec {
            logger.info { "========== API Test Begin ==========" }
        }
        afterSpec {
            logger.info { "========== API Test End ==========" }
        }
    },
    {
        val logger = KotlinLogging.logger {}

        val ktorEngine = SingleKtorTestApplicationEngine.instance

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
    }
)