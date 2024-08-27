/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.cache.redis

import fanpoll.infra.cache.Cache
import fanpoll.infra.redis.ktorio.RedisClient
import fanpoll.infra.redis.ktorio.commands.del
import fanpoll.infra.redis.ktorio.commands.get
import fanpoll.infra.redis.ktorio.commands.set
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration

// Redis Client Impl => https://github.com/ktorio/ktor-clients
class RedisCache<K : Any, V : Any>(
    private val client: RedisClient,
    private val keyPrefix: String,
    private val keyMapper: ((K) -> String?)? = null,
    private val valueSerializer: RedisValueSerializer<V>? = null
) : Cache<K, V> {

    private val logger = KotlinLogging.logger {}

    private val cacheRootPrefix: String = client.rootKeyPrefix + ":cache:"

    override suspend fun get(key: K): V? {
        logger.debug { "[RedisCache:$keyPrefix] get: $key" }
        return getValue(key)
    }

    override suspend fun set(key: K, value: V, expirationMs: Long?) {
        setValue(key, value, expirationMs)
    }

    override suspend fun remove(key: K) {
        logger.debug { "[RedisCache:$keyPrefix] remove: $key" }
        client.del(buildRedisKey(key))
    }

    private suspend fun setValue(key: K, value: V, expirationMs: Long?) {
        val stringValue = valueSerializer?.serialize(value) ?: value as String
        logger.debug {
            "[RedisCache:$keyPrefix] set: $key = $stringValue " +
                    "(${expirationMs?.let { Duration.ofMillis(it).toSeconds().toString() + "s" } ?: "No Expiry"})"
        }
        return client.set(buildRedisKey(key), stringValue, expirationMs)
    }

    private suspend fun getValue(key: K): V? {
        return client.get(buildRedisKey(key))?.let { valueSerializer?.deserialize(it) ?: it as V }
    }

    private fun buildRedisKey(key: K): String {
        val redisKey = when {
            keyMapper != null -> keyMapper.invoke(key)
            key is String -> key
            else -> key.toString()
        }
        return "$cacheRootPrefix:$keyPrefix:$redisKey"
    }
}

