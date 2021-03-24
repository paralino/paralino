package com.zbapp.location.models.location

data class LocationSharingContract(
    val circleId: String,
    val endTimeStamp: Long?
) {

    val isEndless: Boolean get() = endTimeStamp == 0L

}
