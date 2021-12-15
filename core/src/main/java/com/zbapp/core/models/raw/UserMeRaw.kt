package com.zbapp.core.models.raw

import kotlinx.serialization.Serializable

@Serializable
data class UserMeRaw(
    val id: String = "",

    val name: String = "",
    val email: String = "",
    val registration: Long = 0,
    val emailVerification: Boolean = false,
    val data: String? = null
)
