package app.paralino.core.presentation.rawdata

import app.paralino.core.domain.crypto.RawCryptoPresentation
import app.paralino.core.models.Payload
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class RawDataPresentationViewModel constructor(
    private val rawCryptoPresentation: RawCryptoPresentation,
    private val rawPresentationType: RawPresentationDataType,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<RawDataScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<RawDataScreenState>> = _uiState

    private val _eventChannel = Channel<RawDataScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        when (rawPresentationType) {
            is RawPresentationDataType.GroupMemberPresentation -> {
                rawCryptoPresentation.getGroupMemberRawPresentation(rawPresentationType.memberId)
                    .mapToPayload { it }
                    .onEach { _uiState.value = it.mapSuccess { RawDataScreenState(it) } }
                    .launchIn(coroutineScope)
            }
            is RawPresentationDataType.GroupPlacePresentation -> {
                rawCryptoPresentation.getGroupPlaceRawPresentation(rawPresentationType.placeId)
                    .mapToPayload { it }
                    .onEach { _uiState.value = it.mapSuccess { RawDataScreenState(it) } }
                    .launchIn(coroutineScope)
            }
            is RawPresentationDataType.GroupPresentation -> {
                rawCryptoPresentation.getGroupRawPresentation(rawPresentationType.groupId)
                    .mapToPayload { it }
                    .onEach { _uiState.value = it.mapSuccess { RawDataScreenState(it) } }
                    .launchIn(coroutineScope)
            }
            RawPresentationDataType.UserPresentation -> {
                rawCryptoPresentation.getUserRawPresentation()
                    .mapToPayload { it }
                    .onEach { _uiState.value = it.mapSuccess { RawDataScreenState(it) } }
                    .launchIn(coroutineScope)
            }
        }
    }

}