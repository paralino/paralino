package com.zbapp.locationtrack.data.models.group

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.zbapp.locationtrack.data.database.ListOfStringsConverters
import kotlinx.parcelize.Parcelize


@Parcelize
@Entity(tableName = "groups")
data class UserGroupEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val name: String,
    val color: Int,
    val emoji: String,

    @TypeConverters(ListOfStringsConverters::class)
    val userIds: List<String>
): Parcelable
