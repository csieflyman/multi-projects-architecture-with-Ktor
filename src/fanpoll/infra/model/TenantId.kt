/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package fanpoll.infra.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class TenantId(val value: String) {

    override fun toString(): String = value

    @Serializer(forClass = TenantId::class)
    companion object : KSerializer<TenantId> {

        override fun deserialize(decoder: Decoder): TenantId {
            return TenantId(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: TenantId) {
            return encoder.encodeString(value.toString())
        }
    }
}
