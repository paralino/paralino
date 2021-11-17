package com.zbapp.core.utils

interface IConfig {

    var token: String
    var email: String
    var userId: String
    var emailConfirmed: Boolean

    var mainServerUrl: String
    var mainServerKey: String
    val collectionGroupsID: String
    val collectionLocationsID: String
    val collectionInvitesID: String
    val functionInviteCreateID: String
    val functionInviteGetCodeID: String
    val functionInviteGetUrlID: String

    val isTokenValid: Boolean
        get() = token.isNotBlank()

    fun clear()
}