package com.zbapp.locationtrack.data.models.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_codes")
data class PersonalCodeEntity(
    @PrimaryKey
    val id: String,
    val keyId: String,
    val encryptedCode: String,
    val encryptedCodeData: String,
    val validityUnix: Long
)