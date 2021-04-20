/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.redis

import fanpoll.infra.utils.ConfigUtils
import fanpoll.infra.utils.CoroutineConfig
import fanpoll.infra.utils.MyConfig

data class RedisConfig(
    val host: String, val port: Int? = 6379, val password: String?, val rootKeyPrefix: String,
    val client: CoroutineConfig,
    val pubSub: PubSubConfig?
) {

    override fun toString(): String {
        return "url = redis://${if (password != null) "[needPW]@" else ""}$host:$port" +
                " ; rootKeyPrefix = $rootKeyPrefix ; pubSub = ${pubSub != null}"
    }
}

data class PubSubConfig(
    val patterns: List<String>?,
    val channels: List<String>?,
    val keyspaceNotification: KeyspaceNotificationConfig?
) : MyConfig {
    override fun validate() {
        ConfigUtils.require(patterns != null || channels != null) {
            "[Redis PubSub] either patterns or channels should be configured"
        }
    }
}

data class KeyspaceNotificationConfig(
    val subscribeSessionKeyExpired: Boolean,
    val coroutine: CoroutineConfig
)