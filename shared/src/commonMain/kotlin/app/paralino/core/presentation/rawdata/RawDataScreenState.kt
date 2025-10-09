package app.paralino.core.presentation.rawdata

import app.paralino.core.domain.crypto.ContentIdItem
import app.paralino.core.domain.crypto.RawPresentationModel
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.location.LocationHistoryPointId
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.Serializable

data class RawDataScreenState constructor(
    val subtitle: String?,
    val sections: List<RawDataSectionState>
) {

    constructor(model: RawPresentationModel) : this(
        subtitle = model.subtitle,
        sections = model.sections.map { section ->
            RawDataSectionState(
                title = section.title,
                subtitle = section.subtitle,
                decryptedContentSubtitle = section.decryptedDataSubtitle,
                rawContent = section.rawData,
                contentIdInfo = section.contentIdInfo,
                decryptedContent = when (section.decryptedData) {
                    is CryptoResource.Ready -> section.decryptedData.data
                    else -> section.decryptedData.toString()
                },
                type = section.type
            )
        }
    )

}

data class RawDataSectionState(
    val title: String?,
    val subtitle: String?,
    val decryptedContentSubtitle: String?,
    val contentIdInfo: List<ContentIdItem>,
    val rawContent: String,
    val decryptedContent: String,
    val type: RawPresentationDataType,
)

sealed class RawPresentationDataType() {
    val key
        get() = when (this) {
            is GroupPresentation -> "group"
            is GroupMemberPresentation -> "group_member"
            is GroupPlacePresentation -> "place"
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

    object UserPresentation : RawPresentationDataType()
    object PersonalSpacePresentation : RawPresentationDataType()
    data class GroupPresentation(val groupId: String) : RawPresentationDataType()
    data class GroupMemberPresentation(val memberId: String) : RawPresentationDataType()
    data class GroupPlacePresentation(val placeId: String) : RawPresentationDataType()

    @Serializable
    data class LocationHistoryPointPresentation(
        val groupOrSpaceId: String,
        val memberUserId: String,
        val pointIds: List<LocationHistoryPointId>
    ) : RawPresentationDataType()
}

sealed class RawDataScreenEvent() {
    data class Error(val error: Throwable) : RawDataScreenEvent()
}

sealed interface RawDataScreenUiEvent {
    data class CopyContent(val item: ContentIdItem) : RawDataScreenUiEvent
}