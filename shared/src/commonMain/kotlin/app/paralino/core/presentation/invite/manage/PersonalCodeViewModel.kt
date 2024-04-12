package app.paralino.core.presentation.invite.manage

import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class PersonalCodeViewModel(
    private val personalCodeRepo: PersonalCodesRepository,
    private val deviceApiProvider: IDeviceApiProvider,
    private val appConfigRepo: AppConfigRepository
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<PersonalCodeScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<PersonalCodeScreenState>> = _uiState

    private val _eventChannel = Channel<PersonalCodeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        combine(
            personalCodeRepo.getAndRefreshMyPersonalCodes(),
            appConfigRepo.configState
        ) { personalCodes, config ->
            _uiState.update {
                val codeData = personalCodes
                    .map {
                        PersonalCodeScreenCodeData(
                            id = it.id,
                            code = it.formattedCode,
                            userImage = it.codeData.data?.ownerImage
                                ?: ImageModel.emoji(Emojis.robot),
                            userName = it.codeData.data?.ownerName ?: "---",
                            canDisable = it.isStillValid(),
                            isValid = it.isStillValid(),
                            isActionLoading = false
                        )
                    }

                val maxAlternativeIDs = config?.maxAlternativeIDs ?: 0
                val inactiveCodes = codeData.filter { !it.isValid }
                Payload.Success(
                    PersonalCodeScreenState(
                        activeCode = codeData.firstOrNull { it.isValid },
                        inactiveCodes = inactiveCodes,
                        canCreateAlternativeId = maxAlternativeIDs > 0 && codeData.size > 0 && maxAlternativeIDs > inactiveCodes.size,
                        maxCodes = maxAlternativeIDs,
                        createLoading = false
                    )
                )
            }
        }
            .launchIn(coroutineScope)
    }

    fun createPersonalCode() {
        if (_uiState.value is Payload.Loading || _uiState.value.data?.loading == true)
            return

        flow { emit(personalCodeRepo.createAndSavePersonalCode()) }
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
        if (_uiState.value is Payload.Loading || _uiState.value.data?.loading == true)
            return

        personalCodeRepo.revokePersonalCode()
            .onEach { result ->
                when (result) {
                    Payload.Loading -> {
                        _uiState.update {
                            it.mapSuccess {
                                it.copy(
                                    activeCode = it.activeCode?.copy(
                                        isActionLoading = true
                                    )
                                )
                            }
                        }
                    }

                    is Payload.Error -> {
                        _eventChannel.send(PersonalCodeScreenEvent.Error(result.exception))
                        _uiState.update {
                            it.mapSuccess {
                                it.copy(
                                    activeCode = it.activeCode?.copy(
                                        isActionLoading = false
                                    )
                                )
                            }
                        }
                    }

                    is Payload.Success -> {
                        _eventChannel.send(PersonalCodeScreenEvent.CodeDeleted)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun activatePersonalCode(code: String) {
        if (_uiState.value is Payload.Loading || _uiState.value.data?.loading == true)
            return

        personalCodeRepo.activateUserCode(code)
            .onEach { result ->
                _uiState.update {
                    val loading = result is Payload.Loading
                    it.mapSuccess {
                        it.copy(
                            inactiveCodes = it.inactiveCodes.map {
                                if (it.code == code) {
                                    it.copy(isActionLoading = loading)
                                } else {
                                    it
                                }
                            }
                        )
                    }
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