package app.paralino.android.ui.personal_space

import app.paralino.core.app_rating.AppRatingManager
import app.paralino.core.app_rating.AppRatingSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.domain.user.DeactivatePersonalDeviceUseCase
import app.paralino.core.domain.user.DeletePersonalDeviceUseCase
import app.paralino.core.domain.user.PersonalSpaceRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.exception
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.UserDeviceModel
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.groups.detail.LocationSharingState
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.places.PlaceActivityDataSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class PersonalSpaceViewModel(
    private val userRepo: UserRepository,
    private val personalSpaceLocal: UserPersonalSpaceLocalDataSource,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val deletePersonalDevice: DeletePersonalDeviceUseCase,
    private val deactivatePersonalDevice: DeactivatePersonalDeviceUseCase,
    private val personalSpaceRefreshUseCase: PersonalSpaceRefreshUseCase,
    private val deviceStatusProcessor: MemberDeviceStatusProcessor,
    private val sharingManager: LocationSharingManager,
    private val placeActivityRepo: PlaceActivityDataSource,
    private val valueFormatter: IValueFormatter,
    val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs,
    private val appRatingManager: AppRatingManager
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<PersonalSpaceScreenState>(
        PersonalSpaceScreenState(
            isLoadingData = true,
            loadingAction = false,
            sharingStatus = null,
            devicesWithSessions = emptyList(),
            devicesLimit = 0,
            shouldShowPlanBanner = false,
            arePlacesOverLimit = false,
            errors = emptyList(),
            plan = userRepo.userPlans.value.highestValidPlanConfig,
            alertDialog = null
        )
    )
    val uiState: StateFlow<PersonalSpaceScreenState> = _uiState

    private val devicesStateFlow = MutableStateFlow<List<UserDeviceItemState>>(emptyList())
    private val sessionsStateFlow = MutableStateFlow<List<UserSessionItemState>>(emptyList())

    private val _eventChannel = Channel<PersonalSpaceScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        appRatingManager.tryShowingAppRating(source = AppRatingSource.GROUP_DETAIL)

        updateSessions()

        combine(
            deviceApiProvider.deviceStatus,
            personalSpaceLocal.personalSpaceState.mapNotNull { it.data },
            sharingManager.sharingContractForGroup(UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()),
            userRepo.userPlans
        ) { deviceStatus, personalSpaceData, sharingStatus, userPlans ->

            val errors = buildList {
                sharingStatus?.let {
                    if (!deviceStatus.networkStatus.isOnline) {
                        add(ConditionErrorType.Network)
                    }

                    addAll(deviceStatus.locationStatus.getPossibleErrors(forBackground = it.isLongTerm))
                }

                if (!deviceStatus.notificationStatus.isNotificationPermissionAllowed) {
                    add(ConditionErrorType.PermissionNotifications)
                }
            }

            val sharingState = sharingStatus?.let {
                LocationSharingState(
                    endTime = it.endTimeUTC?.let {
                        valueFormatter.fromUTCtoMapSharingTime(
                            timeUTC = it,
                            splitTimeToOneLine = false
                        )
                    },
                    shareBattery = it.shareBattery,
                    shareSpeed = it.shareSpeed,
                    shareElevation = it.shareElevation,
                    shareNetwork = it.shareNetwork,
                    shareDeviceStatus = it.shareDeviceStatus,
                    saveToHistory = it.saveToHistory,
                    approximateLocation = it.approximateLocationInMeters?.let {
                        valueFormatter.getDistanceSmall(it.toDouble())
                    }
                )
            }

            val devicesLimit = personalSpaceData.plan.personalDevicesLimit

            val devices = personalSpaceData.devices
                .sortedBy { it.deviceId != appPrefs.deviceId } // Current device first
                .map { device ->
                    UserDeviceItemState(
                        device = device,
                        isCurrentDevice = device.deviceId == appPrefs.deviceId,
                        deleteAction = if(device.location == null) UserDeviceAction.DeleteDevice() else null,
                        deactivateAction = if(device.location != null) UserDeviceAction.DeactivateDevice() else null,
                        formatter = valueFormatter
                    )
                }

            this.devicesStateFlow.update { devices }

            _uiState.update { state ->
                val newState = state.copy(
                    isLoadingData = false,
                    sharingStatus = sharingState,
                    devicesLimit = devicesLimit,
                    shouldShowPlanBanner = !userPlans.highestValidPlanConfig.planTypeEnum.isPremium,
                    // TODO check place limit when it is done
                    arePlacesOverLimit = false,
                    errors = errors,
                    plan = personalSpaceData.plan
                )

                newState.copy(
                    devicesWithSessions = mapDevicesAndSessions(
                        devices = devices,
                        sessions = sessionsStateFlow.value,
                        previousState = newState
                    )
                )
            }
        }
            .mapToPayload { it }
            .onEach { result ->
                if (result is Payload.Error) {
                    showSnackbar(result.exception.toString())
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun updateSessions() {
        coroutineScope.launch(Dispatchers.IO) {
            val sessions = try {
                userRepo.getAllSessionsFromRemote()
                    .map {
                        UserSessionItemState(
                            session = it,
                            deleteSessionAction = UserDeviceAction.DeleteSession()
                        )
                    }
            } catch (e: Exception) {
                showSnackbar(e.toString())
                emptyList()
            }

            this@PersonalSpaceViewModel.sessionsStateFlow.update {
                sessions
            }

            try {
                personalSpaceRefreshUseCase.refreshPersonalSpace(cacheTimeMillis = 10.seconds.inWholeMilliseconds)
            } catch (e: Exception) {
            }

            _uiState.update { state ->
                state.copy(
                    devicesWithSessions = mapDevicesAndSessions(
                        devices = devicesStateFlow.value,
                        sessions = sessionsStateFlow.value,
                        previousState = state
                    )
                )
            }
        }
    }

    private fun mapDevicesAndSessions(
        devices: List<UserDeviceItemState>,
        sessions: List<UserSessionItemState>,
        previousState: PersonalSpaceScreenState
    ): List<UserDeviceOrSessionItemState> {
        val newList = buildList {
            addAll(
                devices.map { device ->
                    val session = sessions.firstOrNull { it.session.id == device.sessionId }
                    val previousSessionState = previousState.devicesWithSessions.firstOrNull {
                        it.session?.session?.id == session?.session?.id
                    }
                    val previousDeviceState = previousState.devicesWithSessions.firstOrNull {
                        it.device?.device?.id == device.device.id
                    }
                    UserDeviceOrSessionItemState.DeviceWithSession(
                        device = device.copy(
                            deactivateAction = if (device.location != null) {
                                previousDeviceState?.device?.deactivateAction ?: UserDeviceAction.DeactivateDevice()
                            } else {
                                null
                            },
                            deleteAction = if (device.location == null && session == null) {
                                previousDeviceState?.device?.deleteAction ?: UserDeviceAction.DeleteDevice()
                            } else {
                                null
                            }
                        ),
                        session = session?.copy(
                            deleteSessionAction = previousSessionState?.session?.deleteSessionAction ?: session.deleteSessionAction
                        )
                    )
                }
            )

            addAll(
                sessions.mapNotNull { session ->
                    if (devices.none { it.sessionId == session.session.id }) {
                        val previousSessionState = previousState.devicesWithSessions.firstOrNull {
                            it.session?.session?.id == session.session.id
                        }
                        UserDeviceOrSessionItemState.SessionWithoutDevice(
                            session = session.copy(
                                deleteSessionAction = previousSessionState?.session?.deleteSessionAction ?: session.deleteSessionAction
                            )
                        )
                    } else {
                        null
                    }
                }
            )
        }

        return newList
    }

    fun onEvent(event: PersonalSpaceScreenUiEvent) {
        when (event) {
            is PersonalSpaceScreenUiEvent.OnDeviceEvent -> {
                when (event.event) {
                    is UserDeviceItemEvent.onDeleteDeviceClick -> {
                        _uiState.update {
                            it.copy(
                                alertDialog = MainAlertDialog.DeleteInactiveUserOwnDevice(
                                    device = event.event.item.device
                                )
                            )
                        }
                    }

                    is UserDeviceItemEvent.onDeactivateDeviceClick -> {
                        _uiState.update {
                            it.copy(
                                alertDialog = MainAlertDialog.DeactivateUserOwnDevice(
                                    device = event.event.item.device,
                                    isCurrentDevice = event.event.item.isCurrentDevice
                                )
                            )
                        }
                    }

                    is UserDeviceItemEvent.onOpenRawData -> {
                        coroutineScope.launch {
                            _eventChannel.send(PersonalSpaceScreenEvent.OpenRawDataScreen)
                        }
                    }

                    is UserDeviceItemEvent.onOpenSessionClick -> {
                        _uiState.update {
                            it.copy(
                                alertDialog = MainAlertDialog.SessionInfo(
                                    content = "${event.event.item.countryName} - ${event.event.item.deviceModel}",
                                    canDelete = !event.event.item.current,
                                    session = event.event.item
                                )
                            )
                        }
                    }
                }
            }

            PersonalSpaceScreenUiEvent.OpenLocation -> {
                coroutineScope.launch {
                    _eventChannel.send(PersonalSpaceScreenEvent.OpenLocation)
                }
            }

            PersonalSpaceScreenUiEvent.OpenPlaces -> {
                coroutineScope.launch {
                    _eventChannel.send(PersonalSpaceScreenEvent.OpenPlaces)
                }
            }

            PersonalSpaceScreenUiEvent.OpenSharingScreen -> {
                coroutineScope.launch {
                    _eventChannel.send(PersonalSpaceScreenEvent.OpenSharingScreen)
                }
            }

            PersonalSpaceScreenUiEvent.OpenTimeline -> {
                coroutineScope.launch {
                    _eventChannel.send(PersonalSpaceScreenEvent.OpenTimeline)
                }
            }

            is PersonalSpaceScreenUiEvent.DialogAction -> {
                when (val dialog = event.action.dialog) {
                    is MainAlertDialog.SessionInfo -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            _uiState.update {
                                it.copy(
                                    alertDialog = MainAlertDialog.SessionDeleteConfirmation(
                                        content = "${dialog.session.countryName} - ${dialog.session.deviceModel}",
                                        session = dialog.session
                                    )
                                )
                            }
                        } else {
                            _uiState.update { it.copy(alertDialog = null) }
                        }
                    }

                    is MainAlertDialog.SessionDeleteConfirmation -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            logout(sessionId = dialog.session.id)
                        }
                        _uiState.update { it.copy(alertDialog = null) }
                    }

                    is MainAlertDialog.DeleteInactiveUserOwnDevice -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            deleteDevice(device = dialog.device)
                        }
                        _uiState.update { it.copy(alertDialog = null) }
                    }

                    is MainAlertDialog.DeactivateUserOwnDevice -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            deactivateDevice(device = dialog.device)
                        }
                        _uiState.update { it.copy(alertDialog = null) }
                    }

                    else -> {}
                }
            }

            PersonalSpaceScreenUiEvent.OpenPaywall -> {
                coroutineScope.launch {
                    _eventChannel.send(PersonalSpaceScreenEvent.OpenPaywall)
                }
            }
        }
    }

    private fun logout(sessionId: String) {
        flow {
            userRepo.remoteLogout(sessionId = sessionId)
            emit(Unit)
        }
            .mapToPayload { Unit }
            .onEach { state ->
                state.exception?.let { messageDispatcher.showSnackbar(it.errorString()) }

                _uiState.update {
                    it.copy(loadingAction = state is Payload.Loading)
                }
            }
            .launchIn(coroutineScope)
    }

    private fun deleteDevice(device: UserDeviceModel) {
        coroutineScope.launch {
            try {
                _uiState.update { it.copy(loadingAction = true) }
                deletePersonalDevice.deletePersonalDevice(deviceId = device.deviceId)
                _uiState.update { it.copy(loadingAction = false) }
//                showSnackbar(MR.strings.group_members_item_clear_location_success.localizedString())
            } catch (e: Exception) {
                _uiState.update { it.copy(loadingAction = false) }
                logg.e("Error deleting device", e)
                showSnackbar(e.toString())
            }
        }
    }

    private fun deactivateDevice(device: UserDeviceModel) {
        coroutineScope.launch {
            try {
                _uiState.update { it.copy(loadingAction = true) }
                deactivatePersonalDevice.deactivateLocationInPersonalSpace(deviceId = device.deviceId)
                _uiState.update { it.copy(loadingAction = false) }
//                showSnackbar(MR.strings.group_members_item_clear_location_success.localizedString())
            } catch (e: Exception) {
                _uiState.update { it.copy(loadingAction = false) }
                logg.e("Error deleting device", e)
                showSnackbar(e.toString())
            }
        }
    }

    fun stopSharing() {
        coroutineScope.launch {
            sharingManager.stopSharing(
                groupId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId(),
                deleteContract = false
            )
            placeActivityRepo.clearForGroup(groupId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId())
            try {
                deviceStatusProcessor.pushDeviceStatus(groupId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId())
            } catch (e: Exception) {
                logg.w("Error while updating device status", e)
            }
        }
    }
} 