/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.cache

interface Cache<K : Any, V : Any> {

    suspend fun get(key: K): V?

    suspend fun set(key: K, value: V, expirationMs: Long? = null)

    suspend fun remove(key: K)
}