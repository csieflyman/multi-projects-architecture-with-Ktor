/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.principal

import fanpoll.infra.base.entity.Identifiable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass

@Serializable(UserRole.Companion::class)
interface UserRole : Identifiable<String> {

    val type: UserType
    val name: String

    override fun getId(): String = "${type.projectId}_${type.name}_$name"

    companion object : KSerializer<UserRole> {

        private val userRoleEnumMap: MutableMap<String, UserRole> = mutableMapOf()

        fun registerUserRole(userRoleEnumKClass: KClass<out UserRole>) {
            require(userRoleEnumKClass.java.isEnum)
            userRoleEnumKClass.java.enumConstants.forEach { userRoleEnumMap[(it as UserRole).getId()] = it }
        }

        fun getRoleById(roleId: String): UserRole = userRoleEnumMap[roleId]!!

        fun getRolesByType(type: UserType): Set<UserRole> = userRoleEnumMap.values.filter { it.type == type }.toSet()

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.auth.principal.UserType", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): UserRole {
            val id = decoder.decodeString()
            return userRoleEnumMap[id]!!
        }

        override fun serialize(encoder: Encoder, value: UserRole) {
            encoder.encodeString(value.getId())
        }
    }
}