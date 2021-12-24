/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.util

import io.kotest.core.spec.style.FunSpec
import mu.KotlinLogging
import org.testcontainers.containers.GenericContainer

abstract class ContainerTestBase(
    containers: List<GenericContainer<*>>,
    init: FunSpec.() -> Unit = {},
    body: FunSpec.() -> Unit = {}
) : FunSpec({

    val logger = KotlinLogging.logger {}

    beforeSpec {
        containers.forEach {
            logger.info { "========== ${it.image.get()} Start ==========" }
            it.start()
        }
    }

    init()

    afterSpec {
        containers.reversed().forEach {
            logger.info { "========== ${it.image.get()} Stop ==========" }
            it.stop()
        }
    }

    body()
})