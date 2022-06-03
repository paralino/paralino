package com.zbapp.core.models.location

data class MemberGroupId constructor(
    val memberId: String,
    val groupId: String,
    val data: LocationDataModel?
)
