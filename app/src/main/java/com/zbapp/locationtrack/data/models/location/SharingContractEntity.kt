package com.zbapp.locationtrack.data.models.location

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sharing_contracts")
data class SharingContractEntity(
    @PrimaryKey
    val circleId: String,
    val endTimeStamp: Long?
) {
}