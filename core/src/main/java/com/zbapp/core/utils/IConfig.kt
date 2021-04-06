package com.zbapp.core.utils

interface IConfig {

    var token: String
    var email: String
    var userId: String

    val isTokenValid: Boolean
        get() = token.isNotBlank()

    fun clear()
}