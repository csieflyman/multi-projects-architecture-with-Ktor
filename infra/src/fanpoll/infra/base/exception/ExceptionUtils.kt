/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.exception

import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.response.ResponseCodeType
import fanpoll.infra.logging.logString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.locations.LocationRoutingException
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.plugins.ParameterConversionException

object ExceptionUtils {

    private val logger = KotlinLogging.logger {}

    @OptIn(KtorExperimentalLocationsAPI::class)
    fun wrapException(e: Throwable): BaseException = when (e) {
        is BaseException -> e
        is ParameterConversionException -> RequestException(InfraResponseCode.BAD_REQUEST_PATH_OR_QUERYSTRING, e.message, e)
        is MissingRequestParameterException -> RequestException(InfraResponseCode.BAD_REQUEST_PATH_OR_QUERYSTRING, e.message, e)
        is BadRequestException -> RequestException(InfraResponseCode.BAD_REQUEST, e.message, e)
        is LocationRoutingException -> RequestException(InfraResponseCode.BAD_REQUEST_PATH_OR_QUERYSTRING, e.message, e)
        is kotlinx.serialization.SerializationException -> RequestException(InfraResponseCode.BAD_REQUEST_BODY_FORMAT, e.message, e)
        else -> InternalServerException(InfraResponseCode.UNEXPECTED_ERROR, cause = e)
    }

    fun getStackTrace(e: Throwable): String = org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e)

    fun writeLogToFile(e: Throwable, call: ApplicationCall? = null) {
        val ee = wrapException(e)
        val message = "${call?.logString()} => "
        when (ee.code.type) {
            ResponseCodeType.SUCCESS -> logger.debug { "$message${ee.message}" }
            ResponseCodeType.CLIENT_INFO -> logger.info { "$message${ee.message}" }
            ResponseCodeType.CLIENT_ERROR -> logger.warn(ee) { message }
            ResponseCodeType.SERVER_ERROR -> logger.error(ee) { message }
        }
    }
}