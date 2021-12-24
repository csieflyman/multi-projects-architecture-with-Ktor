/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.util

import fanpoll.club.clubMain
import fanpoll.infra.main
import io.kotest.core.spec.style.FunSpec
import io.ktor.application.Application
import mu.KotlinLogging

abstract class KtorTestBase(
    init: FunSpec.() -> Unit = {},
    body: FunSpec.() -> Unit = {}
) : ContainerTestBase(
    listOf(SinglePostgreSQLContainer.instance, SingleRedisContainer.instance),
    {
        val logger = KotlinLogging.logger {}

        beforeSpec {
            logger.info { "========== Ktor Server Start ==========" }
            val ktorEngine = SingleKtorTestApplicationEngine.instance
            ktorEngine.start()

            val installKtorTestModules: Application.() -> Unit = {
                main {
                    SinglePostgreSQLContainer.configureConnectionProperties(infra.database!!.hikari)
                    SingleRedisContainer.configureConnectionProperties(infra.redis!!)
                }
                clubMain()
            }
            ktorEngine.application.installKtorTestModules()
        }

        init()

        afterSpec {
            logger.info { "========== Ktor Server Stop ==========" }
            SingleKtorTestApplicationEngine.instance.stop(0L, 0L)
        }
    },
    body
)