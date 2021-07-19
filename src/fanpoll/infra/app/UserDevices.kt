/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.app

import fanpoll.infra.auth.principal.PrincipalSourceType
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.json.InstantSerializer
import fanpoll.infra.base.json.UUIDSerializer
import fanpoll.infra.database.sql.UUIDTable
import fanpoll.infra.database.util.ResultRowDTOMapper
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.timestamp
import java.time.Instant
import java.util.*

@Serializable
data class CreateUserDeviceForm(
    @Serializable(with = UUIDSerializer::class) val id: UUID = UUID.randomUUID(),
    val userType: UserType,
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    val sourceType: PrincipalSourceType,
    val pushToken: String? = null,
    val osVersion: String? = null,
    val userAgent: String? = null
) : EntityForm<UserDeviceDTO, UUID, UUID>() {

    @Transient
    val enabled: Boolean = true

    @Transient
    val enabledAt: Instant = Instant.now()

    override fun getEntityId(): UUID = id
}

@Serializable
data class UpdateUserDeviceForm(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val pushToken: String? = null,
    val osVersion: String? = null,
    val userAgent: String? = null
) : EntityForm<UserDeviceDTO, UUID, UUID>() {

    var enabled: Boolean? = null

    @Transient
    var enabledAt: Instant? = null

    override fun getEntityId(): UUID = id

    init {
        if (enabled != null)
            enabledAt = Instant.now()
    }
}

@Serializable
data class UserDeviceDTO(@JvmField @Serializable(with = UUIDSerializer::class) val id: UUID) : EntityDTO<UUID> {

    @Serializable(with = UUIDSerializer::class)
    var userId: UUID? = null
    var sourceType: PrincipalSourceType? = null
    var enabled: Boolean? = null
    var pushToken: String? = null
    var osVersion: String? = null
    var userAgent: String? = null

    @Serializable(with = InstantSerializer::class)
    var enabledAt: Instant? = null

    override fun getId(): UUID = id

    companion object {
        val mapper: ResultRowDTOMapper<UserDeviceDTO> = ResultRowDTOMapper(UserDeviceDTO::class, UserDeviceTable)
    }
}

object UserDeviceTable : UUIDTable(name = "infra_user_device") {

    val userId = uuid("user_id")
    val sourceType = enumerationByName("source_type", 20, PrincipalSourceType::class)
    val enabled = bool("enabled")

    val pushToken = varchar("push_token", 255).nullable()
    val osVersion = varchar("os_version", 200).nullable() // for app
    val userAgent = varchar("user_agent", 200).nullable() // for browser

    val enabledAt = timestamp("enabled_at")
    val createdAt = timestamp("created_at")
        .defaultExpression(org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp())
    val updatedAt = timestamp("updated_at")
        .defaultExpression(org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp())

    override val naturalKeys: List<Column<out Any>> = listOf(id)
    override val surrogateKey: Column<EntityID<UUID>> = id
}