/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.exception

import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.response.ResponseCodeType
import fanpoll.infra.logging.logString
import io.ktor.application.ApplicationCall
import io.ktor.features.BadRequestException
import io.ktor.features.MissingRequestParameterException
import io.ktor.features.ParameterConversionException
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.LocationRoutingException
import mu.KotlinLogging

object ExceptionUtils {

    private val logger = KotlinLogging.logger {}

    @OptIn(KtorExperimentalLocationsAPI::class)
    fun wrapException(e: Throwable): BaseException = when (e) {
        is BaseException -> e
        is ParameterConversionException -> RequestException(InfraResponseCode.BAD_REQUEST_PATH_OR_QUERYSTRING, e.message, e)
        is MissingRequestParameterException -> RequestException(InfraResponseCode.BAD_REQUEST_PATH_OR_QUERYSTRING, e.message, e)
        is BadRequestException -> RequestException(InfraResponseCode.BAD_REQUEST, e.message, e)
        is LocationRoutingException -> RequestException(InfraResponseCode.BAD_REQUEST_PATH_OR_QUERYSTRING, e.message, e)
        is kotlinx.serialization.SerializationException -> RequestException(InfraResponseCode.BAD_REQUEST_BODY, e.message, e)
        else -> InternalServerException(InfraResponseCode.UNEXPECTED_ERROR, cause = e)
    }

    fun getStackTrace(e: Throwable): String = org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e)

    fun writeLogToFile(e: Throwable, call: ApplicationCall? = null) {
        val ee = wrapException(e)
        val message = "${call?.logString()} => "
        when (ee.code.type) {
            ResponseCodeType.SUCCESS -> logger.debug("$message${ee.message}")
            ResponseCodeType.CLIENT_INFO -> logger.info("$message${ee.message}")
            ResponseCodeType.CLIENT_ERROR -> logger.warn(message, ee)
            ResponseCodeType.SERVER_ERROR -> logger.error(message, ee)
        }
    }
}