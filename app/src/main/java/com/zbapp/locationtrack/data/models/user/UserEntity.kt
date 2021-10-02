package com.zbapp.locationtrack.data.models.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_me")
data class UserEntity(
    @PrimaryKey
    val id: String,

    val name: String,
    val email: String,
    val imageBase64: String?
) {
}