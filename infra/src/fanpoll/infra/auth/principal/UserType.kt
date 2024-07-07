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

@Serializable(UserType.Companion::class)
interface UserType : Identifiable<String> {

    val projectId: String
    val name: String

    override fun getId(): String = "${projectId}_$name"

    companion object : KSerializer<UserType> {

        private val userTypeEnumMap: MutableMap<String, UserType> = mutableMapOf()

        fun registerUserType(userTypeEnumKClass: KClass<out UserType>) {
            require(userTypeEnumKClass.java.isEnum)
            userTypeEnumKClass.java.enumConstants.forEach { userTypeEnumMap[(it as UserType).getId()] = it }
        }

        fun getTypeById(id: String): UserType = userTypeEnumMap[id]!!

        fun getTypesByProjectId(projectId: String): List<UserType> = userTypeEnumMap.values.filter { it.projectId == projectId }

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.auth.principal.UserType", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): UserType {
            val id = decoder.decodeString()
            return userTypeEnumMap[id]!!
        }

        override fun serialize(encoder: Encoder, value: UserType) {
            encoder.encodeString(value.getId())
        }
    }
}