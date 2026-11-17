package app.paralino.core.models.settings

import app.paralino.core.models.location.LocationHistoryPointId
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.Serializable

@Serializable
sealed class RawPresentationDataType() {
    val key
        get() = when (this) {
            is GroupPresentation -> "group"
            is GroupMemberPresentation -> "group_member"
            is GroupPlacePresentation -> "place"
            is GroupPrivacyZonePresentation -> "privacy_zone"
            is LocationHistoryPointPresentation -> "history"
            UserPresentation -> "user"
            PersonalSpacePresentation -> "space"
        }

    companion object {
        fun fromKey(key: String, payload: String): RawPresentationDataType? {
            return when (key) {
                "group" -> GroupPresentation(payload)
                "group_member" -> GroupMemberPresentation(payload)
                "place" -> GroupPlacePresentation(payload)
                "privacy_zone" -> GroupPrivacyZonePresentation(payload)
                "history" -> {
                    try {
                        BaseRemoteModule.formatter.decodeFromString<LocationHistoryPointPresentation?>(payload)
                    } catch (e: Exception) {
                        null
                    }
                }
                "user" -> UserPresentation
                "space" -> PersonalSpacePresentation
                else -> null
            }
        }
    }

    @Serializable
    data object UserPresentation : RawPresentationDataType()

    @Serializable
    data object PersonalSpacePresentation : RawPresentationDataType()

    @Serializable
    data class GroupPresentation(val groupId: String) : RawPresentationDataType()

    @Serializable
    data class GroupMemberPresentation(val memberId: String) : RawPresentationDataType()

    @Serializable
    data class GroupPlacePresentation(val placeId: String) : RawPresentationDataType()

    @Serializable
    data class GroupPrivacyZonePresentation(val privacyZoneId: String) : RawPresentationDataType()

    @Serializable
    data class LocationHistoryPointPresentation(
        val groupOrSpaceId: String,
        val memberUserId: String,
        val pointIds: List<LocationHistoryPointId>
    ) : RawPresentationDataType()
}