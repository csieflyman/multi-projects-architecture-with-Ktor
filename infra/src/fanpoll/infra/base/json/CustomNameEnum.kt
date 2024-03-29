/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.KClass

interface CustomNameEnumConverter {

    val toCustomName: ((Enum<*>) -> String)

    fun <E : Enum<E>> fromCustomName(): ((String, KClass<E>) -> E)
}

class CustomNameEnum<E : Enum<E>>(enumValue: E, converter: CustomNameEnumConverter) {

    val name: String = converter.toCustomName(enumValue)

    fun toJsonElement(): JsonElement = JsonPrimitive(name)

    constructor(
        value: String,
        enumClass: KClass<E>,
        converter: CustomNameEnumConverter
    ) : this(converter.fromCustomName<E>()(value, enumClass), converter)

    companion object {

        val defaultCustomNameStrategy = object : CustomNameEnumConverter {

            override val toCustomName: (Enum<*>) -> String
                get() = { value -> value.name.decapitalize() }

            override fun <E : Enum<E>> fromCustomName(): (String, KClass<E>) -> E = { value, enumClass ->
                val customValue = value.capitalize()
                enumClass.java.enumConstants.first { it.name == customValue }
            }
        }
    }
}

// QUESTION => is it work for @Serializable? we should use CustomNameEnum.toJsonElement() / invoke(jsonElement...) instead
class CustomNameEnumSerializer<E : Enum<E>>(
    private val enumClass: KClass<E>,
    private val converter: CustomNameEnumConverter
) : KSerializer<CustomNameEnum<E>> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("fanpoll.infra.base.json.CustomNameEnum", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): CustomNameEnum<E> =
        CustomNameEnum(decoder.decodeString(), enumClass, converter)

    override fun serialize(encoder: Encoder, value: CustomNameEnum<E>) = encoder.encodeString(value.name)
}