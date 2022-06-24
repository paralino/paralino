package com.zbapp.core.models.user

import com.zbapp.core.models.base.ImageModel
import kotlinx.serialization.Serializable

@Serializable
data class UserMe constructor(
    val id: String = "",

    val email: String = "",
    val registration: Long = 0,
    val emailVerification: Boolean = false,

    val data: UserMeData? = null
) {

    val name: String get() = data?.userName ?: ""

    val hasValidData: Boolean get() = !data?.userName.isNullOrBlank() && data?.userImage?.takeIfValid() != null
}

@Serializable
data class UserMeData constructor(
    var userImage: ImageModel? = null,
    val userName: String = ""
)