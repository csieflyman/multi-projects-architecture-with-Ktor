/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra

data class ServerConfig(
    val project: String,
    val env: EnvMode,
    val instance: String,
    val shutDownUrl: String
)

enum class EnvMode {
    dev, stage, prod
}