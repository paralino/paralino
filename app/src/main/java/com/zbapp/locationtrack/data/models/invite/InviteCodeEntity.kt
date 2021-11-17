package com.zbapp.locationtrack.data.models.invite

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invites")
data class InviteCodeEntity(
    @PrimaryKey
    val id: String,
    val ownerId: String?,
    val data: String
)