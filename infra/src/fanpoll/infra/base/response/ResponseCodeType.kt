/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.response

enum class ResponseCodeType {

    SUCCESS,
    CLIENT_INFO,
    CLIENT_ERROR,
    SERVER_ERROR;

    fun isError(): Boolean {
        return this != SUCCESS && this != CLIENT_INFO
    }
}