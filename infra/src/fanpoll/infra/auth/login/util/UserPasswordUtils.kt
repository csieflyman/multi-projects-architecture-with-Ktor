/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login.util

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlin.random.Random

object UserPasswordUtils {

    private val DICTIONARY_CHARS: CharArray =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray()

    private const val RANDOM_PASSWORD_LENGTH = 8
    private val RANDOM_PASSWORD_CHARS: CharArray = DICTIONARY_CHARS

    fun generateRandomPassword(): String {
        val randomPassword = Random.nextString(RANDOM_PASSWORD_LENGTH, RANDOM_PASSWORD_CHARS)
        return hashPassword(randomPassword)
    }

    private const val FORGOT_PASSWORD_CODE_LENGTH = 6
    private val FORGOT_PASSWORD_CODE_CHARS: CharArray = DICTIONARY_CHARS

    fun generateForgotPasswordCode(): String {
        return Random.nextString(FORGOT_PASSWORD_CODE_LENGTH, FORGOT_PASSWORD_CODE_CHARS)
    }

    private fun Random.nextString(length: Int, dictionary: CharArray): String {
        val chars = CharArray(length)
        val dictionarySize = dictionary.size

        for (index in 0 until length) {
            chars[index] = dictionary[nextInt(dictionarySize)]
        }

        return String(chars)
    }

    private const val cost = 10
    private val hashser = BCrypt.withDefaults()
    private val verifyer = BCrypt.verifyer()

    fun hashPassword(plainTextPassword: String): String = hashser.hashToString(cost, plainTextPassword.toCharArray())

    fun verifyPassword(plainTextPassword: String, hashedPassword: String): Boolean =
        verifyer.verify(plainTextPassword.toCharArray(), hashedPassword).verified
}