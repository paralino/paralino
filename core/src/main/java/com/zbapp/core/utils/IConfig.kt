package com.zbapp.core.utils

interface IConfig {

    var token: String
    var email: String
    var userId: String
    var emailConfirmed: Boolean
    var mainServerUrl: String
    var mainServerKey: String

    val isTokenValid: Boolean
        get() = token.isNotBlank()

    fun clear()
}