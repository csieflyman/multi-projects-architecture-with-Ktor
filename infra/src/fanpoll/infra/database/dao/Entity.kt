/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.dao

import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.exception.EntityException
import fanpoll.infra.base.extension.copyPropsFrom
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.sql.Table
import fanpoll.infra.database.sql.dtoIdEq
import fanpoll.infra.database.sql.entityIdEq
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

/*
Exposed doesn't support composite primaryKey with DAO API, use auto increment id column instead.
https://github.com/JetBrains/Exposed/issues/353
 */

abstract class LongEntity(id: EntityID<Long>) : LongEntity(id), fanpoll.infra.base.entity.Entity<Long> {

    override fun getId(): Long {
        return id.value
    }

    override fun equals(other: Any?): Boolean = idEquals(other)

    override fun hashCode(): Int = idHashCode()
}

abstract class UUIDEntity(id: EntityID<UUID>) : UUIDEntity(id), fanpoll.infra.base.entity.Entity<UUID> {

    override fun getId(): UUID {
        return id.value
    }

    override fun equals(other: Any?): Boolean = idEquals(other)

    override fun hashCode(): Int = idHashCode()
}

fun <EC : EntityClass<ID, E>, E : Entity<ID>, ID : Comparable<ID>> EC.new(dto: EntityForm<*, *, ID>, init: (E.() -> Unit)? = null): E {
    //TODO => ResponseCode.ENTITY_DUPLICATED?
    return new {
        this.copyPropsFrom(dto)
        init?.let { it() }
    }
}

fun <EC : EntityClass<ID, E>, E : Entity<ID>, ID : Comparable<ID>> EC.update(dto: EntityForm<*, *, ID>, body: (E.() -> Unit)? = null) {
    val entity = dto.getEntityId()?.let { getByEntityId(it) } ?: getByDTOId(dto.getDtoId()!!)
    entity.update(dto, body)
}

fun <E : Entity<ID>, ID : Comparable<ID>> E.update(dto: EntityForm<*, *, ID>, body: (E.() -> Unit)? = null) {
    this.klass
    this.copyPropsFrom(dto)
    body?.let { it() }
}

fun <EC : EntityClass<ID, E>, E : Entity<ID>, ID : Comparable<ID>> EC.newOrUpdate(
    dto: EntityForm<*, *, ID>,
    newInit: (E.() -> Unit)? = null,
    updateBody: (E.() -> Unit)? = null
): E {
    val entity = findByDTOId(dto)
    return if (entity == null)
        new(dto, newInit)
    else {
        entity.update(dto, updateBody)
        entity
    }
}

fun <EC : EntityClass<ID, E>, E : Entity<ID>, ID : Comparable<ID>> EC.findByEntityId(id: ID): E? {
    return find { entityIdEq(table as Table<*>, id) }.singleOrNull()
}

fun <EC : EntityClass<ID, E>, E : Entity<ID>, ID : Comparable<ID>> EC.getByEntityId(id: ID): E {
    return findById(id) ?: throw EntityException(InfraResponseCode.ENTITY_NOT_FOUND, "can't find entity by entityId", entityId = id)
}

fun <EC : EntityClass<ID, E>, E : Entity<ID>, ID : Comparable<ID>> EC.findByDTOId(id: Any): E? {
    return find { dtoIdEq(table as Table<*>, id) }.singleOrNull()
}

fun <EC : EntityClass<ID, E>, E : Entity<ID>, ID : Comparable<ID>> EC.getByDTOId(id: Any): E {
    return findByDTOId(id) ?: throw EntityException(InfraResponseCode.ENTITY_NOT_FOUND, "can't find entity by dtoId", entityId = id)
}

fun <T : EntityDTO<*>> Entity<*>.toDTO(dtoClass: KClass<T>, defaultValueMap: Map<String, Any>? = null): T {
    val dtoPrimaryConstructor =
        dtoClass.primaryConstructor ?: error("DTO class ${dtoClass.qualifiedName} require primaryConstructor")
    val entityProperties = this.javaClass.kotlin.memberProperties
    return dtoPrimaryConstructor.callBy(
        dtoPrimaryConstructor.parameters.associateWith { dtoProperty ->
            if (dtoProperty.name == "entityId") {
                this.id.value
            } else {
                val defaultValue = defaultValueMap?.get(dtoProperty.name)
                val dtoKType = Form::class.createType()
                val iterableDTOKType = typeOf<Iterable<T>>()
                entityProperties.find { it.name == dtoProperty.name }?.get(this)?.let { entityPropertyValue ->
                    if (entityPropertyValue is Entity<*> && dtoProperty.type.isSubtypeOf(dtoKType)) {
                        entityPropertyValue.toDTO(
                            dtoProperty.type.classifier as KClass<EntityDTO<*>>,
                            defaultValue as Map<String, Any>
                        )
                    } else if (entityPropertyValue is Iterable<*> && dtoProperty.type.isSubtypeOf(iterableDTOKType)) {
                        (entityPropertyValue as Iterable<Entity<*>>).map { nestedEntity ->
                            nestedEntity.toDTO(
                                dtoProperty.type.arguments[0].type!!.classifier as KClass<EntityDTO<*>>,
                                defaultValue as Map<String, Any>
                            )
                        }
                    } else entityPropertyValue
                } ?: defaultValue
            }
        })
}