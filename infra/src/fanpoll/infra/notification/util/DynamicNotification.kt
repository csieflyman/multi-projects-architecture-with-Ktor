/*
 * Copyright (c) 2023. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.util

import fanpoll.infra.notification.*
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.notification.channel.email.EmailContent
import fanpoll.infra.notification.channel.push.PushContent
import fanpoll.infra.notification.channel.sms.SMSContent
import io.konform.validation.Validation
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import kotlinx.serialization.Serializable

private val DynamicNotificationType = object : NotificationType {
    override val id: String = "DynamicNotification"
    override val broadcast: Boolean = true
    override val channels: Set<NotificationChannel> = setOf(NotificationChannel.Email, NotificationChannel.SMS, NotificationChannel.Push)
    override val category: NotificationCategory = NotificationCategory.System
}

class DynamicNotification private constructor(
    projectId: String,
    recipients: MutableSet<Recipient>,
    content: NotificationContent,
    dataLoader: NotificationDataLoader,
    val dataLoaderArg: Form
) : Notification(projectId, DynamicNotificationType, recipients, content, dataLoader = dataLoader) {

    @Serializable
    class Form(
        val recipients: MutableSet<Recipient>? = null,
        val recipientsQueryFilter: Map<String, String>? = null,
        val content: ContentForm
    ) : fanpoll.infra.base.form.Form<Form>() {
        override fun validator(): Validation<Form> = VALIDATOR

        companion object {
            private val VALIDATOR: Validation<Form> = Validation {
                Form::content required {
                    addConstraint("content must not be empty") { content -> !content.isEmpty() }
                    addConstraint("email subject or body is empty or blank") { content ->
                        content.email.all { !it.subject.isNullOrBlank() && !it.body.isNullOrBlank() }
                    }
                }
                addConstraint("either recipients or recipientsQueryFilter must not be null") {
                    it.recipients != null || it.recipientsQueryFilter != null
                }
            }
        }

        fun toNotification(projectId: String, dataLoader: NotificationDataLoader): Notification {
            if (content.email.isNotEmpty())
                buildEmailHTMLBody(content.email)
            return DynamicNotification(
                projectId, recipients = recipients ?: mutableSetOf(), content = content.toChannelContent(),
                dataLoader = dataLoader, dataLoaderArg = this
            )
        }

        private fun buildEmailHTMLBody(email: List<EmailContent>) {
            email.forEach { emailContent ->
                emailContent.body = buildString {
                    appendHTML(false).html {
                        head {
                            meta(charset = "UTF-8")
                        }
                        body {
                            div {
                                p {
                                    +emailContent.body!!
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Serializable
    class ContentForm(
        val email: List<EmailContent> = mutableListOf(),
        val push: List<PushContent> = mutableListOf(),
        val sms: List<SMSContent> = mutableListOf(),
        val args: MutableMap<String, String> = mutableMapOf()
    ) {
        fun toChannelContent(): NotificationContent {
            return NotificationContent(listOf(email, push, sms).flatten().toMutableList(), args)
        }

        fun isEmpty(): Boolean = email.isEmpty() && push.isEmpty() && sms.isEmpty()
    }
}