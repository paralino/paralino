package com.zbapp.locationtrack.data.models.location

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "members")
data class MemberEntity constructor(
    @PrimaryKey
    val id: String,
    val userId: String,

    val data: String,
    val mutableData: String? = null
) : Parcelable

