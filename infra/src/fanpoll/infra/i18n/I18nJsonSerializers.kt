/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.i18n

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import fanpoll.infra.base.json.jackson.Jackson

object I18nJsonSerializers {
    fun registerSerializers() {
        Jackson.mapper.registerModule(
            SimpleModule()
                .addSerializer(Lang::class.java, LangSerializer)
                .addDeserializer(Lang::class.java, LangDeSerializer)
        )
    }

    private object LangSerializer : JsonSerializer<Lang>() {

        override fun serialize(value: Lang?, gen: JsonGenerator, serializers: SerializerProvider?) {
            value?.let { gen.writeString(value.code) }
        }
    }

    private object LangDeSerializer : JsonDeserializer<Lang>() {

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Lang {
            return Lang(p.text)
        }
    }
}