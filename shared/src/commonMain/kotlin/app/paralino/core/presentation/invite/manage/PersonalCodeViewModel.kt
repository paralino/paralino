package app.paralino.core.presentation.invite.manage

import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPlans
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.LocationConditionStatus
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType
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
    val locationStatusProvider: ILocationStatusProvider,
    private val userRepo: UserRepository
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<PersonalCodeScreenState>(
        mapPersonalCodes(
            personalCodes = personalCodeRepo.personalCodeLocalState.value,
            userPlans = userRepo.userPlans.value,
            locationStatus = locationStatusProvider.currentStatus,
            activeAlertDialog = null
        )
    )
    val uiState: StateFlow<PersonalCodeScreenState> = _uiState

    private val _eventChannel = Channel<PersonalCodeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        coroutineScope.launch {
            try {
                personalCodeRepo.refreshPersonalCodesAndRestockKeys()
            } catch (e: Exception) {
            }
        }

        combine(
            personalCodeRepo.personalCodeLocalState,
            locationStatusProvider.status,
            userRepo.userPlans
        ) { personalCodes, permissionStatus, plans ->
            _uiState.update {
                mapPersonalCodes(
                    personalCodes = personalCodes,
                    userPlans = plans,
                    locationStatus = permissionStatus,
                    activeAlertDialog = it.alertDialog
                )
            }
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun mapPersonalCodes(
        personalCodes: List<UserPersonalCode>,
        userPlans: UserPlans,
        locationStatus: LocationConditionStatus,
        activeAlertDialog: MainAlertDialog?
    ): PersonalCodeScreenState {
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
                    isActionLoading = false,
                )
            }

        val maxIDs = userPlans.highestValidPlanConfig.paralinoIdsLimit
        val inactiveCodes = codeData.filter { !it.isValid }
        return PersonalCodeScreenState(
            activeCode = codeData.firstOrNull { it.isValid },
            inactiveCodes = inactiveCodes,
            canCreateAlternativeId = AppBuildConfig.instance.hasPreviewFeatures && maxIDs > 0 && codeData.size < maxIDs,
            maxCodes = maxIDs,
            createLoading = false,
            alertDialog = activeAlertDialog,
            errors = buildList {
                if (!locationStatus.isNotificationPermissionAllowed) {
                    add(ConditionErrorType.PERMISSION_NOTIFICATIONS)
                }
            }
        )
    }

    private fun createPersonalCode() {
        if (_uiState.value.loading)
            return

        flow { emit(personalCodeRepo.createAndSavePersonalCode()) }
            .mapToPayload { Unit }
            .onEach { result ->
                _uiState.update {
                    it.copy(createLoading = result is Payload.Loading)
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
        if (_uiState.value.loading)
            return

        personalCodeRepo.revokePersonalCode()
            .onEach { result ->
                when (result) {
                    Payload.Loading -> {
                        _uiState.update {
                            it.copy(
                                activeCode = it.activeCode?.copy(
                                    isActionLoading = true
                                )
                            )
                        }
                    }

                    is Payload.Error -> {
                        showSnackbar(result.exception.errorString())
                        _uiState.update {
                            it.copy(
                                activeCode = it.activeCode?.copy(
                                    isActionLoading = false
                                )
                            )
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
        if (_uiState.value.loading)
            return

        personalCodeRepo.activateUserCode(code)
            .onEach { result ->
                _uiState.update {
                    val loading = result is Payload.Loading
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
            is PersonalCodeScreenUiEvent.CopyPersonalCode -> {
                deviceApiProvider.copyToClipboard(uiState.value.activeCode?.code ?: "")
            }
            PersonalCodeScreenUiEvent.CreateCode -> createPersonalCode()
            PersonalCodeScreenUiEvent.DisableCode -> _uiState.update {
                it.copy(
                    alertDialog = it.activeCode?.code?.let {
                        MainAlertDialog.PersonalCodeDelete(it)
                    }
                )
            }

            PersonalCodeScreenUiEvent.HideDialog -> {
                _uiState.update {
                    it.copy(alertDialog = null)
                }
            }

            PersonalCodeScreenUiEvent.DisableCodeConfirm -> deletePersonalCode()
        }
    }
}