package com.zbapp.location.models.circle

import com.sun.org.apache.xpath.internal.operations.Bool

data class Circle constructor(
    val id: String,
    val name: String,
    val color: Int,
    val emoji: String,
    val userIds: List<String>
) {
    companion object {
        val personalTracking = Circle("personalTracking", "Personal", 0, "\uD83D\uDD12", emptyList())
        val publicSharing = Circle("publicSharing", "Public", 0, "\uD83C\uDFD9️", emptyList())
    }

    val isFromUser: Boolean get() = id == personalTracking.id || id == publicSharing.id

}
