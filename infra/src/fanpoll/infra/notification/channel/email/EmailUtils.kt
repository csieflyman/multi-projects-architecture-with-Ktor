/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.email

import javax.activation.DataHandler
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

object EmailUtils {

    fun populateMimeMessage(mimeMessage: MimeMessage, content: EmailContent, from: String, to: List<String>) {
        mimeMessage.setSubject(content.subject, "UTF-8")
        mimeMessage.setFrom(InternetAddress(from))
        mimeMessage.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(to.joinToString(",")))

        val multipart = MimeMultipart()

        // html body
        val htmlBodyPart = MimeBodyPart()
        htmlBodyPart.setContent(content.body, "text/html; charset=UTF-8")
        multipart.addBodyPart(htmlBodyPart)

        // attachment
        content.attachments?.forEach {
            val attachmentPart = MimeBodyPart()
            val fds = ByteArrayDataSource(it.content, it.mimeType.value)
            fds.name = it.fileName
            attachmentPart.dataHandler = DataHandler(fds)
            attachmentPart.fileName = fds.name
            multipart.addBodyPart(attachmentPart)
        }

        mimeMessage.setContent(multipart)
    }
}