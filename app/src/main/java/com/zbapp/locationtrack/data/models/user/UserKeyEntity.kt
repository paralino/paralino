package com.zbapp.locationtrack.data.models.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_keys")
data class UserKeyEntity constructor(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val encryptedPrivateKey: String,
    val publicKey: String,
    val version: Int
)