package app.paralino.core.models.location

import app.paralino.core.models.base.GroupOrSpaceId

data class MemberGroupId constructor(
    val groupId: GroupOrSpaceId,
    val memberId: String,
    val locationId: String,
    val locationData: LocationDataModel?,
    val deviceStatus: MemberDeviceStatus?,
    val locationHistory: LocationHistoryPointModel?,
)
