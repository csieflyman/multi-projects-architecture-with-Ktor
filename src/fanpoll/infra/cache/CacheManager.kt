/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.cache

import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.ResponseCode
import fanpoll.infra.cache.redis.RedisCache
import fanpoll.infra.redis.ktorio.RedisClient
import mu.KotlinLogging

object CacheManager {

    private val logger = KotlinLogging.logger {}

    private lateinit var redisClient: RedisClient

    // ========== Project Cache  ==========

    private val projectCacheMap: MutableMap<String, Cache<*, *>> = mutableMapOf()

    fun registerCache(cacheId: String, cache: Cache<*, *>) {
        if (cache is RedisCache)
            cache.setClientIfNotInitialized(redisClient)
        projectCacheMap[cacheId] = cache
    }

    // ========== Builtin Cache  ==========

//    lateinit var dtoCache: Map<KClass<out Any>, Cache<out Any, out Any>>
//        private set

//    inline fun <K : Any, reified V : DTO<V, K, *>> getDTOCache(): Cache<K, V> {
//        return (dtoCache[V::class] ?: error("cache type ${V::class.qualifiedName} is not exist"))
//                as? Cache<K, V> ?: error("key or value of cache type ${V::class.qualifiedName} is wrong")
//    }

    fun initBuiltinRedisCaches(redisClient: RedisClient) {
        logger.info("========== init CacheManager... ==========")
        try {
            CacheManager.redisClient = redisClient
        } catch (e: Throwable) {
            throw InternalServerErrorException(ResponseCode.CACHE_ERROR, "could not init CacheManager", e)
        }
        logger.info("========== init CacheManager completed ==========")
    }

    fun shutdown() {
        logger.info("shutdown CacheManager...")
        try {
            //dtoCache.values.forEach { it.shutdown() }
            projectCacheMap.values.forEach { it.shutdown() }
        } catch (e: Throwable) {
            throw InternalServerErrorException(ResponseCode.CACHE_ERROR, "could not shutdown CacheManager", e)
        }
        logger.info("shutdown CacheManager completed")
    }
}