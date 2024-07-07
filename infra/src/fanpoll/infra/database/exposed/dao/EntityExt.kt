/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.dao

import fanpoll.infra.base.extension.copyPropsFrom
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

val logger = KotlinLogging.logger {}

fun <EC : EntityClass<ID, E>, E : Entity<ID>, ID : Comparable<ID>> EC.new(
    entity: fanpoll.infra.base.entity.Entity<ID>,
    init: (E.() -> Unit)? = null
): E {
    return new(entity.getId()) {
        this.copyPropsFrom(entity)
        init?.let { it() }
    }
}

fun <EC : EntityClass<ID, E>, E : Entity<ID>, ID : Comparable<ID>> EC.update(
    entity: fanpoll.infra.base.entity.Entity<ID>,
    body: (E.() -> Unit)? = null
) {
    get(entity.getId()).update(entity, body)
}

fun <E : Entity<ID>, ID : Comparable<ID>> E.update(
    entity: fanpoll.infra.base.entity.Entity<ID>,
    body: (E.() -> Unit)? = null
) {
    this.copyPropsFrom(entity)
    body?.let { it() }
}

fun <EC : EntityClass<ID, E>, E : Entity<ID>, ID : Comparable<ID>> EC.newOrUpdate(
    id: ID,
    entity: fanpoll.infra.base.entity.Entity<ID>,
    newInit: (E.() -> Unit)? = null,
    updateBody: (E.() -> Unit)? = null
): E {
    val dbEntity = findById(id)
    return if (dbEntity == null)
        new(entity, newInit)
    else {
        dbEntity.update(entity, updateBody)
        dbEntity
    }
}

fun <T : Any> Entity<*>.toObject(objectKClass: KClass<T>, block: (T.(Entity<*>) -> Unit)? = null): T {
    logger.debug { "entity class = " + this.javaClass.kotlin.qualifiedName }
    logger.debug { "objectKClass class = " + objectKClass.qualifiedName }
    val objectConstructor = objectKClass.primaryConstructor ?: objectKClass.constructors.first() // // Assume primaryConstructor
    val objectInstance = entityToObjectWithConstructor(this, objectConstructor)
    objectInstance.copyPropsFrom(this, excludes = objectConstructor.parameters.mapNotNull { it.name })
    if (block != null)
        objectInstance.block(this)
    return objectInstance
}

private fun <T : Any> entityToObjectWithConstructor(entity: Entity<*>, objectConstructor: KFunction<T>): T {
    val entityProps = entity.javaClass.kotlin.memberProperties
    val parameterValueMap = objectConstructor.parameters.associateWith { objectKParameter ->
        entityProps.first { it.name == objectKParameter.name }.get(entity).let { propValue ->
            if (propValue is EntityID<*>)
                propValue.value
            else propValue
        }
    }
    logger.debug { "parameterValueMap = $parameterValueMap" }
    return objectConstructor.callBy(parameterValueMap)
}