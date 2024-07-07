/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.principal

import fanpoll.infra.auth.provider.PrincipalSourceAuthConfig
import fanpoll.infra.base.util.IdentifiableObject
import io.ktor.util.AttributeKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(PrincipalSource.Companion::class)
class PrincipalSource(
    val projectId: String,
    val name: String,
    val clientSource: ClientSource,
    val checkClientVersion: Boolean
) : IdentifiableObject<String>() {

    override val id: String = "${projectId}_${name}"

    override fun toString(): String = id

    fun getAuthConfig(): PrincipalSourceAuthConfig = getAuthConfig(id)

    companion object : KSerializer<PrincipalSource> {

        val ATTRIBUTE_KEY = AttributeKey<PrincipalSource>("principalSource")

        val System = PrincipalSource("system", "system", ClientSource.Service, false)

        private val sources = setOf(System).associateBy { it.id }.toMutableMap()

        private val configs: MutableMap<String, PrincipalSourceAuthConfig> = mutableMapOf()

        fun register(config: PrincipalSourceAuthConfig) {
            require(!configs.containsKey(config.id)) { "principalSource ${config.id} had been registered" }
            configs[config.id] = config
            sources[config.id] = config.principalSource
        }

        fun lookup(projectId: String, name: String): PrincipalSource = lookup("${projectId}_${name}")

        fun lookup(id: String): PrincipalSource = sources[id] ?: error("principalSource $id is not defined")

        fun getAuthConfig(id: String): PrincipalSourceAuthConfig = configs[id] ?: error("principalSource $id config is not found")

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.auth.principal.PrincipalSource", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): PrincipalSource {
            val value = decoder.decodeString()
            return lookup(value)
        }

        override fun serialize(encoder: Encoder, value: PrincipalSource) {
            encoder.encodeString(value.id)
        }
    }
}