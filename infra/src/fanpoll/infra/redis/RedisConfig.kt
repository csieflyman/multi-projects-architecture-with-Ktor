/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.redis

import fanpoll.infra.base.async.CoroutineActorConfig
import fanpoll.infra.config.ValidateableConfig

data class RedisConfig(
    val host: String, val port: Int = 6379, val password: String?, val rootKeyPrefix: String,
    val client: CoroutineActorConfig,
    val subscribe: SubscribeConfig?
) {
    override fun toString(): String {
        return "url = redis://${if (password != null) "[needPW]@" else ""}$host:$port" +
                " ; rootKeyPrefix = $rootKeyPrefix ; subscribe = ${subscribe != null}"
    }
}

data class SubscribeConfig(
    val patterns: List<String>?,
    val channels: List<String>?,
    val keyspaceNotification: KeyspaceNotificationConfig?
) : ValidateableConfig {

    override fun validate() {
        require(patterns != null || channels != null) {
            "[Redis Subscriber] either patterns or channels should be configured"
        }
    }
}

data class KeyspaceNotificationConfig(
    val processor: CoroutineActorConfig
)