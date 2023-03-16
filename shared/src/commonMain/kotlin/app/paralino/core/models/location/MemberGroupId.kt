package app.paralino.core.models.location

data class MemberGroupId constructor(
    val groupId: String,
    val locationId: String,
    val member: MemberModel,
    val data: LocationDataModel?
)
