package app.paralino.core.presentation.rawdata

import app.paralino.core.domain.crypto.RawCryptoPresentation
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

class RawDataPresentationViewModel constructor(
    private val rawPresentationType: RawPresentationDataType,
    private val rawCryptoPresentation: RawCryptoPresentation,
    private val deviceApiProvider: IDeviceApiProvider
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<RawDataScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<RawDataScreenState>> = _uiState

    private val _eventChannel = Channel<RawDataScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        val rawFlow = when (rawPresentationType) {
            is RawPresentationDataType.GroupMemberPresentation -> {
                rawCryptoPresentation.getGroupMemberRawPresentation(group = null, rawPresentationType.memberId)
            }

            is RawPresentationDataType.GroupPlacePresentation -> {
                rawCryptoPresentation.getGroupPlaceRawPresentation(rawPresentationType.placeId)
            }

            is RawPresentationDataType.GroupPresentation -> {
                rawCryptoPresentation.getGroupRawPresentation(rawPresentationType.groupId)
            }

            RawPresentationDataType.UserPresentation -> {
                rawCryptoPresentation.getUserRawPresentation()
            }

            RawPresentationDataType.PersonalSpacePresentation -> {
                rawCryptoPresentation.getPersonalSpaceRawPresentation()
            }

            is RawPresentationDataType.LocationHistoryPointPresentation -> {
                rawCryptoPresentation.getLocationHistoryPointRawPresentation(rawPresentationType)
            }
        }

        rawFlow
            .mapToPayload { it }
            .onEach {
                _uiState.value = it.mapSuccess { RawDataScreenState(it) }
                if (it is Payload.Error) {
                    logg.e("raw_data", "Error getting raw data", it.exception)
                }
            }
            .launchIn(coroutineScope)
    }

    fun onEvent(event: RawDataScreenUiEvent) {
        when (event) {
            is RawDataScreenUiEvent.CopyContent -> {
                deviceApiProvider.copyToClipboard(event.item.text.trim())
            }
        }
    }
}