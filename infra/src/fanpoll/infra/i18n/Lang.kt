/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.i18n

import fanpoll.infra.base.extension.myEquals
import fanpoll.infra.base.extension.myHashCode
import io.ktor.util.AttributeKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

@Serializable(Lang.Companion::class)
class Lang(val locale: Locale) {

    constructor(code: String) : this(Locale.Builder().setLanguageTag(code).build())

    val code: String = locale.toLanguageTag()

    fun satisfies(accept: Lang): Boolean = Locale.lookup(listOf(Locale.LanguageRange(code)), listOf(accept.locale)) != null

    override fun toString(): String = code
    override fun equals(other: Any?): Boolean = myEquals(other, { code })

    override fun hashCode(): Int = myHashCode({ code })

    companion object : KSerializer<Lang> {

        val ATTRIBUTE_KEY = AttributeKey<Lang>("lang")

        val SystemDefault: Lang = Lang(Locale.getDefault())

        val zh_TW: Lang = Lang(Locale.TAIWAN)
        val en: Lang = Lang(Locale.ENGLISH)

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.base.i18n.Lang", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Lang) {
            encoder.encodeString(value.code)
        }

        override fun deserialize(decoder: Decoder): Lang {
            return Lang(decoder.decodeString())
        }
    }
}