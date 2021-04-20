/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth

import fanpoll.infra.login.UserSession
import fanpoll.infra.model.TenantId
import fanpoll.infra.utils.IdentifiableObject
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

@Serializable
open class UserType(val projectId: String, val name: String) : IdentifiableObject<String>() {

    override val id: String = "${projectId}_${name}"

    open val roles: Set<UserRole>? = null

    open fun findRunAsUserById(userId: UUID): User = throw UnsupportedOperationException("userType $name runas is not supported")

    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(forClass = UserType::class)
    companion object : KSerializer<UserType> {

        private val registeredUserTypes = mutableMapOf<String, UserType>()

        fun register(vararg types: UserType) {
            register(types.toSet())
        }

        fun register(types: Collection<UserType>) {
            types.forEach {
                require(!registeredUserTypes.contains(it.id)) { "userType ${it.id} had been registered" }
                registeredUserTypes[it.id] = it
            }
        }

        fun values(): List<UserType> = registeredUserTypes.values.toList()

        fun lookup(projectId: String, name: String): UserType = lookup("${projectId}_${name}")

        fun lookup(typeId: String): UserType = registeredUserTypes[typeId] ?: error("userType $typeId is not defined")

        fun lookupRole(roleId: String): UserRole = registeredUserTypes.values
            .filter { it.roles != null }.flatMap { it.roles!! }.first { it.id == roleId }

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.auth.UserType", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): UserType {
            val value = decoder.decodeString()
            return lookup(value)
        }

        override fun serialize(encoder: Encoder, value: UserType) {
            encoder.encodeString(value.id)
        }
    }
}

@Serializable
class UserRole(
    private val userTypeId: String,
    override val name: String,
    @Transient private val parent: UserRole? = null
) : PrincipalRole, IdentifiableObject<String>() {

    override val id: String = "${userTypeId}_$name"

    operator fun contains(other: PrincipalRole): Boolean {
        return if (this.javaClass == other.javaClass)
            this == other || this == (other as UserRole).parent
        else
            false
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(forClass = UserRole::class)
    companion object : KSerializer<UserRole> {

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.auth.UserRole", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): UserRole {
            val value = decoder.decodeString()
            return UserType.lookupRole(value)
        }

        override fun serialize(encoder: Encoder, value: UserRole) {
            encoder.encodeString(value.id)
        }
    }
}

// service can run as user if userId is specified
// if user's role is changed, user will be logout by system instantly
// sessions for a user vary from source
class UserPrincipal(
    val userType: UserType,
    val userId: UUID,
    val roles: Set<UserRole>? = null,
    override val source: PrincipalSource,
    val clientId: String? = null,
    val tenantId: TenantId? = null,
    val runAs: Boolean = false,
    var session: UserSession? = null
) : MyPrincipal() {

    override val id = userId.toString()

    override fun toString(): String {
        return "${if (runAs) "(runAs)" else ""}$userType-$userId-$roles-$source-${clientId ?: "?"}${if (tenantId != null) "-$tenantId" else ""}"
    }

    fun sessionId(): String = session!!.id.value
}

class User(val type: UserType, val id: UUID, val roles: Set<UserRole>? = null, val tenantId: TenantId? = null)