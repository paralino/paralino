package app.paralino.core.presentation.rawdata

import app.paralino.core.domain.crypto.ContentIdItem
import app.paralino.core.domain.crypto.RawPresentationModel
import app.paralino.core.models.CryptoResource

data class RawDataScreenState constructor(
    val sections: List<RawDataSectionState>
) {

    constructor(model: RawPresentationModel) : this(
        model.sections.map { section ->
            RawDataSectionState(
                title = section.title,
                subtitle = section.subtitle,
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
            UserPresentation -> "user"
        }

    companion object {
        fun fromKey(key: String, payload: String): RawPresentationDataType? {
            return when (key) {
                "group" -> GroupPresentation(payload)
                "group_member" -> GroupMemberPresentation(payload)
                "place" -> GroupPlacePresentation(payload)
                "user" -> UserPresentation
                else -> null
            }
        }
    }

    object UserPresentation : RawPresentationDataType()
    data class GroupPresentation(val groupId: String) : RawPresentationDataType()
    data class GroupMemberPresentation(val memberId: String) : RawPresentationDataType()
    data class GroupPlacePresentation(val placeId: String) : RawPresentationDataType()
}

sealed class RawDataScreenEvent() {
    data class Error(val error: Throwable) : RawDataScreenEvent()
}

sealed interface RawDataScreenUiEvent {
    data class CopyContent(val item: ContentIdItem) : RawDataScreenUiEvent
}