/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.club.dtos

import fanpoll.club.club.domain.Club
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.extension.toObject
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

private const val Club_ID_LENGTH = 30
private const val Club_NAME_LENGTH = 30

@Serializable
data class CreateClubForm(
    val id: String,
    val name: String,
    val enabled: Boolean = true
) : EntityForm<CreateClubForm, Club, String>() {

    @Transient
    lateinit var creatorId: UUID

    override fun getEntityId(): String = id

    override fun toEntity(): Club = toObject(Club::class)

    override fun validator(): Validation<CreateClubForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<CreateClubForm> = Validation {
            CreateClubForm::id required { maxLength(Club_ID_LENGTH) }
            CreateClubForm::name required { maxLength(Club_NAME_LENGTH) }
        }
    }
}

@Serializable
data class UpdateClubForm(
    val id: String,
    val name: String? = null,
    val enabled: Boolean? = null
) : EntityForm<UpdateClubForm, Club, String>() {

    @Transient
    lateinit var currentUser: UserPrincipal

    override fun getEntityId(): String = id

    override fun toEntity(): Club = toObject(Club::class)

    override fun validator(): Validation<UpdateClubForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<UpdateClubForm> = Validation {
            UpdateClubForm::name ifPresent { maxLength(Club_NAME_LENGTH) }
        }
    }
}