package app.paralino.core.models.location

data class MemberGroupId constructor(
    val groupId: String,
    val memberId: String,
    val locationId: String,
    val locationData: LocationDataModel?,
    val deviceStatus: MemberDeviceStatus?
)
