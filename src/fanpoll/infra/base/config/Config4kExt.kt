/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.config

import com.typesafe.config.Config
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.ResponseCode
import io.github.config4k.ClassContainer
import io.github.config4k.CustomType
import io.github.config4k.extract
import io.github.config4k.readers.SelectReader
import io.github.config4k.registerCustomType
import io.ktor.config.tryGetString
import kotlinx.serialization.SerialName
import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

object Config4kExt {

    fun registerCustomType() {
        registerCustomType(validateableConfigReader)
    }

    private val validateableConfigReader = object : CustomType {

        override fun parse(clazz: ClassContainer, config: Config, name: String): Any {
            return extractWithParameters(clazz, config, name).also { (it as ValidateableConfig).validate() }
        }

        override fun testParse(clazz: ClassContainer): Boolean {
            return clazz.mapperClass.isSubclassOf(ValidateableConfig::class)
        }

        override fun testToConfig(obj: Any): Boolean {
            throw InternalServerException(ResponseCode.UNSUPPORTED_OPERATION_ERROR)
        }

        override fun toConfig(obj: Any, name: String): Config {
            throw InternalServerException(ResponseCode.UNSUPPORTED_OPERATION_ERROR)
        }

    }

// COMPATIBILITY => Because we can't access config4k internal Reader,
// we copy code from io.github.config4k.readers.ArbitraryTypeReader and TypeReference

    private fun extractWithParameters(
        clazz: ClassContainer,
        config: Config,
        parentPath: String = ""
    ): Any {
        // support sealed class
        val mapperClass = if (clazz.mapperClass.isSealed)
            sealedClassContainer(clazz.mapperClass, config, parentPath) else clazz.mapperClass

        val constructor = mapperClass.primaryConstructor!!
        val map = constructor.parameters.associateWith { param ->
            val type = param.type.javaType
            val classContainer: ClassContainer = when (type) {
                is ParameterizedType -> ClassContainer(
                    (type.rawType as Class<*>).kotlin,
                    getGenericMap(type, clazz.typeArguments)
                )
                is Class<*> -> ClassContainer(type.kotlin)
                else -> requireNotNull(clazz.typeArguments[type.typeName]) { "couldn't find type argument for ${type.typeName}" }
            }
            SelectReader.getReader(classContainer)
                .invoke(if (parentPath.isEmpty()) config else config.extract(parentPath), param.name!!)
        }
        val parameters = omitValue(map, config, parentPath)
        if (clazz.mapperClass.visibility == KVisibility.PRIVATE) {
            constructor.isAccessible = true
        }
        return constructor.callBy(parameters)
    }

    // not support generic now
    private fun sealedClassContainer(sealedClass: KClass<*>, config: Config, paramName: String): KClass<*> {
        val serialNameClassMap = sealedClass.sealedSubclasses.map { subClass ->
            val annotation = subClass.annotations.first { it.annotationClass == SerialName::class } as? SerialName
                ?: error("Config deserialization: Sealed Class ${sealedClass.qualifiedName} @SerialName annotation is required")
            annotation.value to subClass
        }.toMap()

        val serialTypeValue = config.tryGetString("$paramName._type")
            ?: error("Config deserialization: Sealed Class ${sealedClass.qualifiedName} _type property is required")
        return serialNameClassMap[serialTypeValue]
            ?: error("Config deserialization: Sealed Class ${sealedClass.qualifiedName} _type value $serialTypeValue is not mapped")
    }

    // if config doesn't have corresponding value, the value is omitted
    private fun omitValue(
        map: Map<KParameter, Any?>,
        config: Config,
        parentPath: String
    ): Map<KParameter, Any?> =
        map.filterNot { (param, _) ->
            val path = if (parentPath.isEmpty()) param.name
            else "$parentPath.${param.name}"
            param.isOptional && !config.hasPathOrNull(path)
        }

    private fun getGenericMap(
        type: ParameterizedType,
        typeArguments: Map<String, ClassContainer> = emptyMap()
    ): Map<String, ClassContainer> {
        val typeParameters = (type.rawType as Class<*>).kotlin.typeParameters
        return type.actualTypeArguments.mapIndexed { index, r ->
            val typeParameterName = typeParameters[index].name
            val impl = if (r is WildcardType) r.upperBounds[0] else r
            typeParameterName to if (impl is TypeVariable<*>) {
                requireNotNull(typeArguments[impl.name]) { "no type argument for ${impl.name} found" }
            } else {
                val wild = ((if (impl is ParameterizedType) impl.rawType else impl) as Class<*>).kotlin
                if (impl is ParameterizedType) ClassContainer(wild, getGenericMap(impl, typeArguments))
                else ClassContainer(wild)
            }
        }.toMap()
    }
}