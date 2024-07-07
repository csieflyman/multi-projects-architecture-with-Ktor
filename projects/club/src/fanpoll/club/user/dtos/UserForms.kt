/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.user.dtos

import fanpoll.club.ClubUserRole
import fanpoll.club.club.domain.ClubRole
import fanpoll.club.user.domain.Gender
import fanpoll.club.user.domain.User
import fanpoll.club.user.domain.UserJoinedClub
import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.extension.toObject
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.form.ValidationUtils
import fanpoll.infra.base.json.kotlinx.UUIDSerializer
import fanpoll.infra.base.location.EntityIdLocation
import fanpoll.infra.i18n.Lang
import fanpoll.infra.openapi.schema.operation.support.OpenApiModel
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

private const val USER_NAME_LENGTH = 30

@OpenApiModel(propertyNameOrder = ["account", "password", "enabled", "name"])
@Serializable
data class CreateUserForm(
    val account: String,
    var password: String,
    val enabled: Boolean = true,
    val name: String,
    val gender: Gender? = null,
    val birthYear: Int? = null,
    val email: String? = null,
    val mobile: String? = null,
    val lang: Lang? = null,
    val roles: Set<ClubUserRole>
) : EntityForm<CreateUserForm, User, UUID>() {

    @Transient
    val id: UUID = UUID.randomUUID()

    override fun getEntityId(): UUID = id

    override fun toEntity(): User = toObject(User::class)

    override fun validator(): Validation<CreateUserForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<CreateUserForm> = Validation {
            CreateUserForm::account required { run(ValidationUtils.EMAIL_VALIDATOR) }
            CreateUserForm::password required { run(ValidationUtils.PASSWORD_VALIDATOR) }
            CreateUserForm::name required { maxLength(USER_NAME_LENGTH) }
            CreateUserForm::email ifPresent { run(ValidationUtils.EMAIL_VALIDATOR) }
            CreateUserForm::mobile ifPresent { run(ValidationUtils.MOBILE_NUMBER_VALIDATOR) }
            CreateUserForm::roles required { addConstraint("roles must not be empty") { roles -> roles.isNotEmpty() } }
        }
    }
}

@OpenApiModel(propertyNameOrder = ["id", "enabled", "name"])
@Serializable
data class UpdateUserForm(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val enabled: Boolean? = null,
    val name: String? = null,
    val gender: Gender? = null,
    val birthYear: Int? = null,
    val email: String? = null,
    val mobile: String? = null,
    val lang: Lang? = null,
    val roles: Set<ClubUserRole>? = null
) : EntityForm<UpdateUserForm, User, UUID>() {

    override fun getEntityId(): UUID = id

    override fun toEntity(): User = toObject(User::class)

    override fun validator(): Validation<UpdateUserForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<UpdateUserForm> = Validation {
            UpdateUserForm::name ifPresent { maxLength(USER_NAME_LENGTH) }
            UpdateUserForm::email ifPresent { run(ValidationUtils.EMAIL_VALIDATOR) }
            UpdateUserForm::mobile ifPresent { run(ValidationUtils.MOBILE_NUMBER_VALIDATOR) }
        }
    }
}

@Serializable
data class UpdateUserPasswordForm(val oldPassword: String, val newPassword: String) : Form<UpdateUserPasswordForm>() {

    @Transient
    lateinit var userId: UUID
    override fun validator(): Validation<UpdateUserPasswordForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<UpdateUserPasswordForm> = Validation {
            UpdateUserPasswordForm::oldPassword required { run(ValidationUtils.PASSWORD_VALIDATOR) }
            UpdateUserPasswordForm::newPassword required { run(ValidationUtils.PASSWORD_VALIDATOR) }
        }
    }
}

@Serializable
data class UserJoinClubForm(
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    val clubId: String,
    val role: ClubRole,
) : Form<UserJoinClubForm>() {

    fun toUserJoinedClub(): UserJoinedClub {
        return UserJoinedClub(userId, clubId).apply {
            role = this@UserJoinClubForm.role
        }
    }
}

@OptIn(KtorExperimentalLocationsAPI::class)
@io.ktor.server.locations.Location("/{entityId}/joinClub")
data class UserJoinClubLocation(override val entityId: UUID) : EntityIdLocation()