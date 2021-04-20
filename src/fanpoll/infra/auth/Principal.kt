/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth

import fanpoll.infra.utils.IdentifiableObject
import io.ktor.auth.Principal
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

const val PRINCIPAL_MDC: String = "principal"

abstract class MyPrincipal : Principal, IdentifiableObject<String>() {

    abstract val source: PrincipalSource
}

@Serializable
class PrincipalSource(
    val projectId: String, val name: String,
    val login: Boolean, val userDeviceType: UserDeviceType? = null
) : IdentifiableObject<String>() {

    override val id: String = "${projectId}_${name}"

    val checkClientVersion = login && userDeviceType?.isApp() == true

    fun getConfig(): PrincipalSourceAuthConfig<*> = getConfig(id)

    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(forClass = PrincipalSource::class)
    companion object : KSerializer<PrincipalSource> {

        val System = PrincipalSource("system", "system", false)

        private val registeredSources = setOf(System).map { it.id to it }.toMap().toMutableMap()

        private val configs: MutableMap<String, PrincipalSourceAuthConfig<*>> = mutableMapOf()

        fun register(sources: Map<PrincipalSource, PrincipalSourceAuthConfig<*>>) {
            sources.forEach {
                val id = it.key.id
                require(!registeredSources.contains(id)) { "principalSource $id had been registered" }
                registeredSources[id] = it.key
                configs[id] = it.value
            }
        }

        fun lookup(projectId: String, name: String): PrincipalSource = lookup("${projectId}_${name}")

        fun lookup(id: String): PrincipalSource = registeredSources[id] ?: error("principalSource $id is not defined")

        fun getConfig(id: String): PrincipalSourceAuthConfig<*> = configs[id]!!

        fun getRunAsConfigs(): List<UserAuthConfig> = configs.values.filter { it is UserAuthConfig && it.runAsKey != null }
            .map { it as UserAuthConfig }.toList()

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.auth.PrincipalSource", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): PrincipalSource {
            val value = decoder.decodeString()
            return lookup(value)
        }

        override fun serialize(encoder: Encoder, value: PrincipalSource) {
            encoder.encodeString(value.id)
        }
    }
}

interface PrincipalRole {
    val id: String
    val name: String
}

enum class UserDeviceType {

    Browser, Android, iOS;

    fun isApp(): Boolean = this == Android || this == iOS
}