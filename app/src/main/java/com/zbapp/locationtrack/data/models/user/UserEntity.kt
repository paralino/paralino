package com.zbapp.locationtrack.data.models.user

import androidx.room.Entity

@Entity(tableName = "user_me")
data class UserEntity(
    val id: String,

    val name: String,
    val email: String,
    val imageBase64: String?,
    val userSince: String
) {
}