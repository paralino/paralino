package com.zbapp.locationtrack.data.models.group

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Parcelize
@Entity(tableName = "groups")
data class UserGroupEntity(
    @PrimaryKey
    val id: String,
    val ownerUserId: String,
    val data: String
) : Parcelable {

}
