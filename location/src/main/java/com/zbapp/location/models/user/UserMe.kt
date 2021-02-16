package com.zbapp.location.models.user

data class UserMe (
    val id: String,

    val name: String,
    val email: String,
    val imageBase64: String?,
    val userSince: String
)
