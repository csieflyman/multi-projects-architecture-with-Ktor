/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.auth.login.WebLoginForm
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.notification.NotificationContent
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.Recipient
import fanpoll.infra.notification.channel.email.EmailContent
import fanpoll.infra.notification.channel.push.PushContent
import fanpoll.infra.notification.channel.sms.SMSContent
import fanpoll.infra.notification.util.SendNotificationForm
import fanpoll.infra.openapi.OpenApiOperation
import fanpoll.infra.openapi.ProjectOpenApi
import fanpoll.infra.openapi.schema.Tag
import fanpoll.infra.openapi.schema.component.support.ComponentLoader
import fanpoll.infra.openapi.schema.operation.definitions.PropertyDef
import fanpoll.infra.openapi.schema.operation.definitions.ReferenceObject
import fanpoll.infra.openapi.schema.operation.definitions.SchemaDataType
import fanpoll.infra.openapi.schema.operation.support.utils.ResponseUtils
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

object OpsOpenApi {

    private val AuthTag = Tag("auth")
    private val UserTag = Tag("user")
    private val MonitorTag = Tag("monitor")
    private val queryLogTag = Tag("queryLog")
    private val DataTag = Tag("data")
    private val AppReleaseTag = Tag("appRelease")

    val CreateUser = OpenApiOperation("CreateUser", listOf(UserTag))
    val UpdateUser = OpenApiOperation("UpdateUser", listOf(UserTag))
    val FindUsers = OpenApiOperation("FindUsers", listOf(UserTag))
    val UpdateMyPassword = OpenApiOperation("UpdateMyPassword", listOf(UserTag))
    val SendNotification = OpenApiOperation("SendNotification", listOf(UserTag)) {

        addRequestExample(
            SendNotificationForm(
                recipients = mutableSetOf(Recipient("tester@test.com", name = "tester", email = "tester@test.com")),
                userFilters = mapOf(OpsUserType.User.value to "[account = tester@test.com]"),
                content = NotificationContent(
                    email = mutableMapOf(Lang.zh_TW to EmailContent("Test Email", "This is a test")),
                    push = mutableMapOf(Lang.zh_TW to PushContent("Test Push", "This is a test")),
                    sms = mutableMapOf(Lang.zh_TW to SMSContent("Test SMS"))
                ),
                contentArgs = mutableMapOf("data" to "test")
            )
        )
    }

    val Login = OpenApiOperation("Login", listOf(AuthTag)) {

        addErrorResponses(
            InfraResponseCode.AUTH_PRINCIPAL_DISABLED,
            InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED
        )

        addRequestExample(WebLoginForm("tester@test.com", "test123"))
    }
    val Logout = OpenApiOperation("Logout", listOf(AuthTag))

    val HealthCheck = OpenApiOperation("HealthCheck", listOf(MonitorTag))

    val QueryNotificationMessageLog = OpenApiOperation("QueryNotificationMessageLog", listOf(queryLogTag))

    val DataReport = OpenApiOperation("DataReport", listOf(DataTag))

    val CreateAppRelease = OpenApiOperation("CreateAppRelease", listOf(AppReleaseTag))
    val UpdateAppRelease = OpenApiOperation("UpdateAppRelease", listOf(AppReleaseTag))
    val FindAppReleases = OpenApiOperation("FindAppReleases", listOf(AppReleaseTag))
    val CheckAppRelease = OpenApiOperation("CheckAppRelease", listOf(AppReleaseTag))

    private val operationType = typeOf<OpenApiOperation>()

    private val allOperations = OpsOpenApi::class.memberProperties
        .filter { it.returnType == operationType }
        .map { it.getter.call(this) as OpenApiOperation }

    private val components = object : ComponentLoader {

        private val UserTypeSchema = PropertyDef(
            UserType::class.simpleName!!, SchemaDataType.string,
            kClass = UserType::class
        )

        private val NotificationTypeSchema = PropertyDef(
            NotificationType::class.simpleName!!, SchemaDataType.string,
            kClass = NotificationType::class
        )

        private val ResponseCodeValueSchema = PropertyDef(
            "OpsResponseCode", SchemaDataType.string,
            ResponseUtils.buildResponseCodesDescription(OpsResponseCode.AllCodes),
            enum = OpsResponseCode.AllCodes.map { it.value }.toList()
        )

        override fun load(): List<ReferenceObject> {
            UserTypeSchema.enum = UserType.values().filter { it.projectId == OpsConst.projectId }.map { it.id }
            NotificationTypeSchema.enum = NotificationType.values().filter { it.projectId == OpsConst.projectId }.map { it.id }
            return listOf(UserTypeSchema, NotificationTypeSchema, ResponseCodeValueSchema).map { it.createRef() }
        }
    }

    val Instance = ProjectOpenApi(
        OpsConst.projectId, OpsConst.urlRootPath, OpsAuth.allAuthSchemes,
        allOperations, components
    )
}
