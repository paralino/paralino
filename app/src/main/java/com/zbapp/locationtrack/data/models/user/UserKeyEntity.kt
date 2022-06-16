package com.zbapp.locationtrack.data.models.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_keys")
data class UserKeyEntity constructor(
    @PrimaryKey
    val id: String,
    val ownerUserId: String,
    val encryptedSymmetricKey: String,
    val encryptedPrivateKey: String,
    val publicKey: String,
    val version: Long
)