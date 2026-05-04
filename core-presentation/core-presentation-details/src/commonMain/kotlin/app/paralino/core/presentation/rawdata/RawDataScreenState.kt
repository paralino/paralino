package app.paralino.core.presentation.rawdata

import app.paralino.core.domain.crypto.ContentIdItem
import app.paralino.core.domain.crypto.RawPresentationModel
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.settings.RawPresentationDataType

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
                decryptedContent = when (val finalData = section.decryptedData) {
                    is CryptoResource.Ready -> finalData.data
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

sealed class RawDataScreenEvent() {
    data class Error(val error: Throwable) : RawDataScreenEvent()
}

sealed interface RawDataScreenUiEvent {
    data class CopyContent(val item: ContentIdItem) : RawDataScreenUiEvent
}