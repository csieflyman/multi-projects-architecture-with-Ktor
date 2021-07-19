/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.cache.redis

import fanpoll.infra.base.json.json
import kotlinx.serialization.KSerializer

interface RedisValueSerializer<T> {
    /**
     * Serializes a complex arbitrary object into a [String].
     */
    fun serialize(obj: T): String

    /**
     * Deserializes a complex arbitrary object from a [String].
     */
    fun deserialize(text: String): T
}

object RedisByteArraySerializer : RedisValueSerializer<ByteArray> {

    private val charset = Charsets.UTF_8

    override fun serialize(obj: ByteArray): String {
        return obj.toString(charset)
    }

    override fun deserialize(text: String): ByteArray {
        return text.toByteArray(charset)
    }
}

class RedisJsonSerializer<T>(private val jsonSerializer: KSerializer<T>) : RedisValueSerializer<T> {

    override fun serialize(obj: T): String {
        return json.encodeToJsonElement(jsonSerializer, obj).toString()
    }

    override fun deserialize(text: String): T {
        return json.decodeFromJsonElement(jsonSerializer, json.parseToJsonElement(text))
    }
}