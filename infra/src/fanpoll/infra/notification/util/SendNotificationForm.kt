/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.util

import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.notification.Notification
import fanpoll.infra.notification.NotificationContent
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.Recipient
import io.konform.validation.Validation
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import kotlinx.serialization.Serializable

@Serializable
data class SendNotificationForm(
    val recipients: MutableSet<Recipient>? = null,
    val userFilters: Map<UserType, String>? = null,
    val content: NotificationContent,
    val contentArgs: MutableMap<String, String>? = null
) : Form<SendNotificationForm>() {

    override fun validator(): Validation<SendNotificationForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<SendNotificationForm> = Validation {
            SendNotificationForm::content required {
                addConstraint("email subject or body is empty or blank") { dto ->
                    dto.email.all { !it.value.subject.isNullOrBlank() && !it.value.body.isNullOrBlank() }
                }
            }
        }
    }

    fun toNotification(type: NotificationType): Notification {
        content.email.values.forEach {
            it.body = buildEmailHtmlBody(it.body!!)
        }

        val recipients = recipients ?: type.findRecipients(userFilters)
        if (recipients.isEmpty()) {
            throw RequestException(InfraResponseCode.QUERY_RESULT_EMPTY, "recipients is empty")
        }

        return Notification(
            type, recipients = recipients.toMutableSet(),
            content, contentArgs = contentArgs ?: mutableMapOf(),
            remote = false,
            traceId = traceId
        )
    }

    private fun buildEmailHtmlBody(content: String): String {
        return buildString {
            appendHTML(false).html {
                head {
                    meta(charset = "UTF-8")
                }
                body {
                    div {
                        p {
                            +content
                        }
                    }
                }
            }
        }
    }
}