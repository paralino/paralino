package com.zbapp.locationtrack.data.models.group

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.zbapp.locationtrack.data.models.location.MemberEntity
import kotlinx.parcelize.Parcelize


@Parcelize
@Entity(tableName = "groups")
data class UserGroupEntity(
    @PrimaryKey
    val id: String,
    val ownerId: String?,
    val data: String,

    @Ignore
    val members: List<MemberEntity>
): Parcelable {

    // to satisfy room
    constructor(id: String, ownerId: String?, data: String): this(id, ownerId, data, emptyList())

}
