/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.report

import fanpoll.infra.base.json.kotlinx.UUIDSerializer
import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.base.json.kotlinx.merge
import fanpoll.infra.base.json.kotlinx.toJsonElement
import fanpoll.infra.base.util.IdentifiableObject
import fanpoll.infra.i18n.Lang
import fanpoll.infra.report.data.DatasetItem
import fanpoll.infra.report.export.ReportFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.util.*

@Serializable
class Report(val projectId: String, val type: ReportType, val lang: Lang) : IdentifiableObject<UUID>() {
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID = UUID.randomUUID()

    lateinit var title: String

    @Transient
    lateinit var dataset: List<DatasetItem>

    @Transient
    lateinit var file: ReportFile

    fun toJson(): JsonObject {
        val reportJson = this.toJsonElement()
        return buildJsonObject {
            merge(reportJson)
            if (file.mimeType == ReportFile.MimeType.JSON)
                put("content", json.parseToJsonElement(String(file.content, Charsets.UTF_8)).jsonObject)
        }
    }
}








