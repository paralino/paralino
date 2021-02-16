package com.zbapp.location.models.location

data class LocationSharingContract(
    val circleId: String,
    val endTimeStamp: Long?
) {

    fun asValidContract(): LocationSharingContract? {
        if(endTimeStamp == null) return null
        return this
    }

}
