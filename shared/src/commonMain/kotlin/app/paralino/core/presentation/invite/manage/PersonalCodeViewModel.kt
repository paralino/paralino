package app.paralino.core.presentation.invite.manage

import app.paralino.core.models.Payload
import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonalCodeViewModel(
    private val personalCodeRepo: PersonalCodesRepository,
    private val deviceApiProvider: IDeviceApiProvider,
    private val appConfigRepo: AppConfigRepository
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<PersonalCodeScreenState>>(
        mapPersonalCodes(
            personalCodes = personalCodeRepo.personalCodeLocalState.value,
            config = appConfigRepo.configState.value,
            activeAlertDialog = null
        )
    )
    val uiState: StateFlow<Payload<PersonalCodeScreenState>> = _uiState

    private val _eventChannel = Channel<PersonalCodeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        coroutineScope.launch {
            try {
                personalCodeRepo.refreshPersonalCodes()
            } catch (e: Exception) {
            }
        }

        combine(
            personalCodeRepo.getAndRefreshMyPersonalCodes(),
            appConfigRepo.configState
        ) { personalCodes, config ->
            _uiState.update {
                mapPersonalCodes(
                    personalCodes = personalCodes,
                    config = config,
                    activeAlertDialog = it.data?.alertDialog
                )
            }
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun mapPersonalCodes(
        personalCodes: List<UserPersonalCode>,
        config: ConfigData?,
        activeAlertDialog: MainAlertDialog?
    ): Payload<PersonalCodeScreenState> {
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
        return Payload.Success(
            PersonalCodeScreenState(
                activeCode = codeData.firstOrNull { it.isValid },
                inactiveCodes = inactiveCodes,
                canCreateAlternativeId = maxAlternativeIDs > 0 && codeData.size > 0 && maxAlternativeIDs > inactiveCodes.size,
                maxCodes = maxAlternativeIDs,
                createLoading = false,
                alertDialog = activeAlertDialog
            )
        )
    }

    private fun createPersonalCode() {
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
                        showSnackbar(result.exception.errorString())
                    }

                    is Payload.Success -> {
                        _eventChannel.send(PersonalCodeScreenEvent.CodeCreated)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun deletePersonalCode() {
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
                        showSnackbar(result.exception.errorString())
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

    private fun activatePersonalCode(code: String) {
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
                        showSnackbar(result.exception.errorString())
                    }

                    is Payload.Success -> {
                        _eventChannel.send(PersonalCodeScreenEvent.CodeCreated)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun onEvent(event: PersonalCodeScreenUiEvent) {
        when (event) {
            is PersonalCodeScreenUiEvent.ActivateCode -> activatePersonalCode(event.code)
            is PersonalCodeScreenUiEvent.CopyToClipboard -> deviceApiProvider.copyToClipboard(event.content)
            PersonalCodeScreenUiEvent.CreateCode -> createPersonalCode()
            PersonalCodeScreenUiEvent.DisableCode -> _uiState.update {
                it.mapSuccess {
                    it.copy(alertDialog = it.activeCode?.code?.let {
                        MainAlertDialog.PersonalCodeDelete(it)
                    })
                }
            }

            PersonalCodeScreenUiEvent.HideDialog -> {
                _uiState.update {
                    it.mapSuccess { it.copy(alertDialog = null) }
                }
            }

            PersonalCodeScreenUiEvent.DisableCodeConfirm -> deletePersonalCode()
        }
    }
}