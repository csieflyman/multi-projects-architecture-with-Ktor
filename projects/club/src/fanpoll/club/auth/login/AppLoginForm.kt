/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.auth.login

import fanpoll.infra.auth.login.LoginForm
import fanpoll.infra.auth.principal.ClientAttributeKey
import fanpoll.infra.base.form.ValidationUtils
import fanpoll.infra.release.app.domain.AppOS
import io.konform.validation.Validation
import io.ktor.server.application.ApplicationCall
import kotlinx.serialization.Serializable

@Serializable
data class AppLoginForm(
    override val account: String,
    override val password: String,
    override var clientVersion: String? = null,
    val appOS: AppOS,
    val deviceId: String,
    val devicePushToken: String? = null
) : LoginForm<AppLoginForm>() {

    override fun populateRequest(call: ApplicationCall) {
        super.populateRequest(call)
        call.attributes.put(ClientAttributeKey.CLIENT_ID, deviceId)
    }

    override fun validator(): Validation<AppLoginForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<AppLoginForm> = Validation {
            AppLoginForm::account required { run(ValidationUtils.EMAIL_VALIDATOR) }
            AppLoginForm::password required { run(ValidationUtils.PASSWORD_VALIDATOR) }
        }
    }
}