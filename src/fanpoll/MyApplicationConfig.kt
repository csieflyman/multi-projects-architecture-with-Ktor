/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll

import com.typesafe.config.ConfigFactory
import fanpoll.club.ClubConfig
import fanpoll.infra.InfraConfig
import fanpoll.infra.base.config.Config4kExt
import fanpoll.ops.OpsConfig
import io.github.config4k.extract
import io.ktor.application.ApplicationEnvironment
import mu.KotlinLogging
import java.io.File

data class MyApplicationConfig(
    val server: ServerConfig,
    val infra: InfraConfig,
    val ops: OpsConfig,
    val club: ClubConfig
)

data class ServerConfig(
    val project: String,
    val env: EnvMode,
    val instance: String
)

enum class EnvMode {
    dev, stage, prod
}

// com.typesafe.config.Config is private val in ktor HoconApplicationConfig
object MyApplicationConfigLoader {

    private val logger = KotlinLogging.logger {}

    fun load(environment: ApplicationEnvironment): MyApplicationConfig {
        val myConfigFilePath = environment.config.propertyOrNull("ktor.application.myConfigFile")?.getString()
        logger.info { "ktor.application.myConfigFile = $myConfigFilePath" }
        try {
            logger.info { "load my application config file..." }
            val myConfig = myConfigFilePath?.let { ConfigFactory.parseFile(File(it)) } ?: ConfigFactory.load()
            //logger.debug(myConfig.getConfig("myapp").entrySet().toString())
            Config4kExt.registerCustomType()
            return myConfig.extract("myapp")
        } catch (e: Throwable) {
            logger.error("fail to load my application config file", e)
            throw e
        }
    }
}