package com.zbapp.core.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserMe (
    val id: String = "",

    val name: String = "",
    val email: String = "",
    var imageBase64: String? = null,
    val registration: Long = 0,
    val emailVerification: Boolean = false
)
