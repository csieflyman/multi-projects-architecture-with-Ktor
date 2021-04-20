/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.cache.redis

import fanpoll.infra.cache.Cache
import fanpoll.infra.redis.ktorio.RedisClient
import fanpoll.infra.redis.ktorio.commands.del
import fanpoll.infra.redis.ktorio.commands.get
import fanpoll.infra.redis.ktorio.commands.set
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.Duration

// Redis Client Impl => https://github.com/ktorio/ktor-clients
class RedisCache<K : Any, V : Any>(
    private val keyPrefix: String,
    private val keyMapper: ((K) -> String?)? = null,
    private val rootKeyPrefix: String? = null,
    private val valueSerializer: RedisValueSerializer<V>? = null,
    private val shutdownBlock: (() -> Unit)? = null,
    client: RedisClient? = null
) : Cache<K, V> {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private lateinit var client: RedisClient

    init {
        if (client != null) {
            this.client = client
        }
    }

    fun setClientIfNotInitialized(client: RedisClient) {
        if (!this::client.isInitialized)
            this.client = client
    }

    override fun get(key: K): V? {
        logger.debug("[RedisCache:$keyPrefix] get: $key")
        return runBlocking {
            getValue(key)
        }
    }

    override suspend fun getAsync(key: K): V? {
        logger.debug("[RedisCache:$keyPrefix] getAsync: $key")
        return getValue(key)
    }

    override fun put(key: K, value: V, expirationMs: Long?) {
        return runBlocking {
            setValue(key, value, expirationMs)
        }
    }

    override suspend fun putAsync(key: K, value: V, expirationMs: Long?) {
        setValue(key, value, expirationMs)
    }

    override suspend fun invalidate(key: K) {
        logger.debug("[RedisCache:$keyPrefix] invalidate: $key")
        client.del(buildRedisKey(key))
    }

    override fun shutdown() {
        shutdownBlock?.invoke()
    }

    private suspend fun setValue(key: K, value: V, expirationMs: Long?) {
        val stringValue = valueSerializer?.serialize(value) ?: value as String
        logger.debug("[RedisCache:$keyPrefix] set: $key = $stringValue " +
                "(${expirationMs?.let { Duration.ofMillis(it).toSeconds().toString() + "s" } ?: "No Expiry"})")
        return client.set(buildRedisKey(key), stringValue, expirationMs)
    }

    private suspend fun getValue(key: K): V? {
        return client.get(buildRedisKey(key))?.let { valueSerializer?.deserialize(it) ?: it as V }
    }

    private fun buildRedisKey(key: K): String {
        val redisKey = when (key) {
            keyMapper != null -> keyMapper?.invoke(key)
            is String -> key
            else -> key.toString()
        }
        return "${rootKeyPrefix ?: client.rootKeyPrefix}:$keyPrefix:$redisKey"
    }
}

