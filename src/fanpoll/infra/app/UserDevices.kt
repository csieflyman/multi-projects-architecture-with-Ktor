/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.app

import fanpoll.infra.auth.UserDeviceType
import fanpoll.infra.auth.UserType
import fanpoll.infra.controller.EntityDTO
import fanpoll.infra.controller.EntityForm
import fanpoll.infra.database.ResultRowDTOMapper
import fanpoll.infra.database.UUIDDTOTable
import fanpoll.infra.utils.InstantSerializer
import fanpoll.infra.utils.UUIDSerializer
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
    val type: UserDeviceType,
    val pushToken: String? = null,
    val osVersion: String? = null,
    val userAgent: String? = null
) : EntityForm<UserDeviceDTO, UUID, UUID>() {

    @Transient
    val enabled: Boolean = true

    @Transient
    val enabledTime: Instant = Instant.now()

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
    var enabledTime: Instant? = null

    override fun getEntityId(): UUID = id

    init {
        if (enabled != null)
            enabledTime = Instant.now()
    }
}

@Serializable
data class UserDeviceDTO(@JvmField @Serializable(with = UUIDSerializer::class) val id: UUID) : EntityDTO<UUID> {

    @Serializable(with = UUIDSerializer::class)
    var userId: UUID? = null
    var type: UserDeviceType? = null
    var enabled: Boolean? = null
    var pushToken: String? = null
    var osVersion: String? = null
    var userAgent: String? = null

    @Serializable(with = InstantSerializer::class)
    var enabledTime: Instant? = null

    override fun getId(): UUID = id

    companion object {
        val mapper: ResultRowDTOMapper<UserDeviceDTO> = ResultRowDTOMapper(UserDeviceDTO::class, UserDeviceTable)
    }
}

object UserDeviceTable : UUIDDTOTable(name = "infra_user_device") {

    val userId = uuid("user_id")
    val type = enumeration("type", UserDeviceType::class)
    val enabled = bool("enabled")

    val pushToken = varchar("push_token", 255).nullable()
    val osVersion = varchar("os_version", 10).nullable() // for app
    val userAgent = varchar("user_agent", 200).nullable() // for browser

    val enabledTime = timestamp("enabled_time")
    val createTime = timestamp("create_time")
        .defaultExpression(org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp())
    val updateTime = timestamp("update_time")
        .defaultExpression(org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp())

    override val naturalKeys: List<Column<out Any>> = listOf(id)
    override val surrogateKey: Column<EntityID<UUID>> = id
}