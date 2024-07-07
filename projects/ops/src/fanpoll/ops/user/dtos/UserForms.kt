/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.user.dtos

import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.extension.toObject
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.form.ValidationUtils
import fanpoll.infra.base.json.kotlinx.UUIDSerializer
import fanpoll.infra.i18n.Lang
import fanpoll.infra.openapi.schema.operation.support.OpenApiModel
import fanpoll.ops.OpsUserRole
import fanpoll.ops.user.domain.User
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

private const val USER_NAME_LENGTH = 30

@OpenApiModel(propertyNameOrder = ["account", "password", "enabled", "role", "name"])
@Serializable
data class CreateUserForm(
    val account: String,
    val enabled: Boolean = true,
    val name: String,
    val email: String,
    val mobile: String? = null,
    val lang: Lang? = null,
    var password: String,
    var roles: Set<OpsUserRole>
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
            CreateUserForm::email required { run(ValidationUtils.EMAIL_VALIDATOR) }
            CreateUserForm::mobile ifPresent { run(ValidationUtils.MOBILE_NUMBER_VALIDATOR) }
            CreateUserForm::roles required { addConstraint("roles must not be empty") { roles -> roles.isNotEmpty() } }
        }
    }
}

@OpenApiModel(propertyNameOrder = ["id", "enabled", "role", "name"])
@Serializable
data class UpdateUserForm(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val enabled: Boolean? = null,
    val name: String? = null,
    val email: String? = null,
    val mobile: String? = null,
    val lang: Lang? = null,
    val roles: Set<OpsUserRole>? = null
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