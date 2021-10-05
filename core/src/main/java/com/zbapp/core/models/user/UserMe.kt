package com.zbapp.core.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserMe (
    val id: String = "",

    val name: String = "",

    val email: String = "",
    val registration: Long = 0,
    val emailVerification: Boolean = false,

    val data: UserMeData? = null
)

@Serializable
data class UserMeData(
    var imageBase64: String? = null,
    val friends: List<UserFriend> = emptyList()
)