/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.principal

import fanpoll.infra.auth.provider.RunAsUser
import fanpoll.infra.base.json.json
import fanpoll.infra.base.util.IdentifiableObject
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import java.util.*

@Serializable(UserType.Companion::class)
open class UserType(val projectId: String, val name: String) : IdentifiableObject<String>() {

    override val id: String = "${projectId}_${name}"

    override fun toString(): String = name

    open val roles: Set<UserRole>? = null

    open fun findRunAsUserById(userId: UUID): RunAsUser =
        throw UnsupportedOperationException("userType $name runas is not supported")

    companion object : KSerializer<UserType> {

        init {
            json.serializersModule.plus(SerializersModule {
                polymorphicDefaultSerializer(UserType::class) { serializer() }
            })
        }

        private val registeredUserTypes = mutableMapOf<String, UserType>()

        fun register(vararg types: UserType) {
            register(types.toSet())
        }

        fun register(types: Collection<UserType>) {
            types.forEach {
                require(!registeredUserTypes.contains(it.id)) { "userType ${it.id} had been registered" }
                registeredUserTypes[it.id] = it
            }
        }

        fun values(): List<UserType> = registeredUserTypes.values.toList()

        fun lookup(projectId: String, name: String): UserType = lookup("${projectId}_${name}")

        fun lookup(typeId: String): UserType = registeredUserTypes[typeId] ?: error("userType $typeId is not defined")

        fun lookupRole(roleId: String): UserRole = registeredUserTypes.values
            .filter { it.roles != null }.flatMap { it.roles!! }.first { it.id == roleId }

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.auth.principal.UserType", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): UserType {
            val value = decoder.decodeString()
            return lookup(value)
        }

        override fun serialize(encoder: Encoder, value: UserType) {
            encoder.encodeString(value.id)
        }
    }
}