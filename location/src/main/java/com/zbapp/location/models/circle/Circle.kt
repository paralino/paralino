package com.zbapp.location.models.circle

data class Circle(
    val id: String,
    val name: String,
    val color: Int,
    val userIds: List<String>
) {
    companion object {
        val personalTracking = Circle("personalTracking", "Personal", 0, emptyList())
        val publicSharing = Circle("publicSharing", "Public", 0, emptyList())
    }
}
