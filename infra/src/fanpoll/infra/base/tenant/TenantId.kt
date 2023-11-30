/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package fanpoll.infra.base.tenant

import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(TenantId.Companion::class)
data class TenantId(val value: String) {

    override fun toString(): String = value

    @Serializer(forClass = TenantId::class)
    companion object : KSerializer<TenantId> {

        val ATTRIBUTE_KEY = AttributeKey<TenantId>("tenantId")

        override fun deserialize(decoder: Decoder): TenantId {
            return TenantId(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: TenantId) {
            return encoder.encodeString(value.toString())
        }
    }
}

val ApplicationCall.tenantId: TenantId?
    get() = attributes.getOrNull(TenantId.ATTRIBUTE_KEY)

