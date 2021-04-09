package com.zbapp.core.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserMe (
    val id: String = "",

    val name: String = "",
    val email: String = "",
    val imageBase64: String? = null,
    val userSince: String = ""
)
