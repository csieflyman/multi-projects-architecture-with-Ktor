/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.principal

import fanpoll.infra.base.json.json
import fanpoll.infra.base.util.IdentifiableObject
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

@Serializable(UserRole.Companion::class)
class UserRole(
    private val userTypeId: String,
    override val name: String,
    @Transient private val parent: UserRole? = null
) : PrincipalRole, IdentifiableObject<String>() {

    override val id: String = "${userTypeId}_$name"

    override fun toString(): String = name

    operator fun contains(other: PrincipalRole): Boolean {
        return if (this.javaClass == other.javaClass)
            this == other || this == (other as UserRole).parent
        else
            false
    }

    companion object : KSerializer<UserRole> {

        init {
            json.serializersModule.plus(SerializersModule {
                polymorphicDefault(UserRole::class) { serializer() }
            })
        }

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.auth.principal.UserRole", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): UserRole {
            val value = decoder.decodeString()
            return UserType.lookupRole(value)
        }

        override fun serialize(encoder: Encoder, value: UserRole) {
            encoder.encodeString(value.id)
        }
    }
}