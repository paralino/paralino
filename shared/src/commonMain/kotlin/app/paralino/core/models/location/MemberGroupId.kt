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

data class MemberGroupIdForUpload constructor(
    val groupId: GroupOrSpaceId,
    val locationId: String,
    val locationData: LocationDataModel?,
    val deviceStatus: MemberDeviceStatus?,
    val uploadPendingHistory: Boolean,
) {
    constructor(model: MemberGroupId): this(
        groupId = model.groupId,
        locationId = model.locationId,
        locationData = model.locationData,
        deviceStatus = model.deviceStatus,
        uploadPendingHistory = model.locationHistory != null
    )
}
