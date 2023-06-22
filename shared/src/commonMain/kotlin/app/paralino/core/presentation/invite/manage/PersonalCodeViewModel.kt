package app.paralino.core.presentation.invite.manage

import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class PersonalCodeViewModel(
    private val repo: PersonalCodesRepository,
    private val deviceApiProvider: IDeviceApiProvider
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<PersonalCodeScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<PersonalCodeScreenState>> = _uiState

    private val _eventChannel = Channel<PersonalCodeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        repo.getAndRefreshMyPersonalCodes()
            .onEach { result ->
                _uiState.update {

                    val codeData = result
                        .map {
                            PersonalCodeScreenCodeData(
                                id = it.id,
                                code = it.formattedCode,
                                userImage = it.codeData.data?.ownerImage ?: ImageModel.emoji(Emojis.robot),
                                userName = it.codeData.data?.ownerName ?: "---",
                                canDelete = it.isStillValid(),
                                isValid = it.isStillValid()
                            )
                        }

                    Payload.Success(
                        PersonalCodeScreenState(
                            activeCode = codeData.firstOrNull { it.isValid },
                            inactiveCodes = codeData.filter { !it.isValid },
                            canCreate = codeData.size < 10,
                            maxCodes = 10,
                            createLoading = false,
                            actionLoading = false
                        )
                    )
                }
            }
            .launchIn(coroutineScope)
    }

    fun createPersonalCode() {
        if(_uiState.value is Payload.Loading || _uiState.value.data?.loading == true)
            return

        flow { emit(repo.createAndSavePersonalCode()) }
            .mapToPayload { Unit }
            .onEach { result ->
                _uiState.update {
                    it.mapSuccess { it.copy(createLoading = result is Payload.Loading) }
                }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Error -> {
                        _eventChannel.send(PersonalCodeScreenEvent.Error(result.exception))
                    }
                    is Payload.Success -> {
                        _eventChannel.send(PersonalCodeScreenEvent.CodeCreated)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun deletePersonalCode() {
        if(_uiState.value is Payload.Loading || _uiState.value.data?.loading == true)
            return

        repo.revokePersonalCode()
            .onEach { result ->
                when (result) {
                    Payload.Loading -> {
                        _uiState.update { it.mapSuccess { it.copy(actionLoading = true) } }
                    }
                    is Payload.Error -> {
                        _eventChannel.send(PersonalCodeScreenEvent.Error(result.exception))
                        _uiState.update { it.mapSuccess { it.copy(actionLoading = false) } }
                    }
                    is Payload.Success -> {
                        _eventChannel.send(PersonalCodeScreenEvent.CodeDeleted)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun activatePersonalCode(code: String) {
        if(_uiState.value is Payload.Loading || _uiState.value.data?.loading == true)
            return

        repo.activateUserCode(code)
            .onEach { result ->
                _uiState.update {
                    val loading = result is Payload.Loading
                    it.mapSuccess { it.copy(createLoading = loading, actionLoading = loading) }
                }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Error -> {
                        _eventChannel.send(PersonalCodeScreenEvent.Error(result.exception))
                    }
                    is Payload.Success -> {
                        _eventChannel.send(PersonalCodeScreenEvent.CodeCreated)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun copyToClipboard(data: String) {
        deviceApiProvider.copyToClipboard(data)
    }

}