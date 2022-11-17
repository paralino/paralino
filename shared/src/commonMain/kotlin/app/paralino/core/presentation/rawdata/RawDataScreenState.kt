package app.paralino.core.presentation.rawdata

import app.paralino.core.domain.crypto.RawPresentationModel
import app.paralino.core.models.CryptoResource

data class RawDataScreenState constructor(
    val title: String,
    val rawContent: String,
    val decryptedContent: String,
    val type: RawPresentationDataType
) {

    constructor(model: RawPresentationModel) : this(
        title = model.title,
        rawContent = model.rawData,
        decryptedContent = when (model.decryptedData) {
            is CryptoResource.Ready -> model.decryptedData.data
            else -> model.decryptedData.toString()
        },
        type = model.type
    )

}

sealed class RawPresentationDataType() {
    val key
        get() = when (this) {
            is GroupMemberPresentation -> "group"
            is GroupPlacePresentation -> "place"
            is GroupPresentation -> "group_member"
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