package app.paralino.core.presentation.personal_space

import app.paralino.core.MR
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
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.user.UserSession
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.groups.detail.LocationSharingState
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.places.PlaceActivityDataSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.SnackbarDuration
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
            userName = userRepo.localUser.value.data?.name ?: "",
            userImage = userRepo.localUser.value.data?.data?.userImage,
            isLoadingData = true,
            loadingAction = false,
            sharingStatus = null,
            devicesWithSessions = emptyList(),
            devicesLimit = 0,
            shouldShowPlanBanner = false,
            arePlacesOverLimit = null,
            isSpaceOverLimit = null,
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
            userRepo.localUser.mapNotNull { it.data },
            userRepo.userPlans
        ) { deviceStatus, personalSpaceData, sharingStatus, user, userPlans ->

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
                        deleteAction = if (device.location == null) UserDeviceAction.DeleteDevice() else null,
                        deactivateAction = if (device.location != null) UserDeviceAction.DeactivateDevice() else null,
                        formatter = valueFormatter
                    )
                }

            this.devicesStateFlow.update { devices }

            _uiState.update { state ->
                val newState = state.copy(
                    userName = user.name,
                    userImage = user.data?.userImage,
                    isLoadingData = false,
                    sharingStatus = sharingState,
                    devicesLimit = devicesLimit,
                    shouldShowPlanBanner = !userPlans.highestValidPlanConfig.planTypeEnum.isPremium,
                    arePlacesOverLimit = personalSpaceData.arePlacesOverLimits(),
                    isSpaceOverLimit = personalSpaceData.isSpaceOverLimits(),
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
                    showSnackbar(result.exception.errorString())
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        _uiState
            .filter { it.isSpaceOverLimit == true }
            .map { it.isSpaceOverLimit == true }
            .distinctUntilChanged()
            .onEach {
                showSnackbar(
                    text = MR.strings.plan_limit_overLimit_personalSpace_banner_title.localizedString(),
                    duration = SnackbarDuration.Forever
                )

                delay(1000)
                _eventChannel.trySend(PersonalSpaceScreenEvent.OpenPaywall)
            }
            .launchIn(coroutineScope)
    }

    private fun updateSessions() {
        coroutineScope.launch(Dispatchers.IO) {
            val sessions = try {
                userRepo.getAllSessionsFromRemote()
                    .map {
                        UserSessionItemState(session = it)
                    }
            } catch (e: Exception) {
                showSnackbar(e.errorString())
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
                                previousDeviceState?.device?.deactivateAction
                                    ?: UserDeviceAction.DeactivateDevice()
                            } else {
                                null
                            },
                            // if the device has session, user should first log out and then delete the device
                            deleteAction = if (device.location == null && session == null) {
                                previousDeviceState?.device?.deleteAction
                                    ?: UserDeviceAction.DeleteDevice()
                            } else {
                                null
                            }
                        ),
                        session = session
                    )
                }
            )

            addAll(
                sessions.mapNotNull { session ->
                    if (devices.none { it.sessionId == session.session.id }) {
                        UserDeviceOrSessionItemState.SessionWithoutDevice(
                            session = session
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

                    is UserDeviceItemEvent.onOpenSessionClick -> {
                        val sessionInfo = getSessionInfo(event.event.item)
                        _uiState.update {
                            it.copy(
                                alertDialog = MainAlertDialog.SessionInfo(
                                    content = sessionInfo,
                                    canDelete = !event.event.item.current,
                                    session = event.event.item
                                )
                            )
                        }
                    }
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

            PersonalSpaceScreenUiEvent.OpenMap -> {
                coroutineScope.launch {
                    appPrefs.isPersonalSpaceSelected = true
                    _eventChannel.send(PersonalSpaceScreenEvent.OpenMap)
                }
            }

            is PersonalSpaceScreenUiEvent.DialogAction -> {
                when (val dialog = event.action.dialog) {
                    is MainAlertDialog.SessionInfo -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            val sessionInfo = getSessionInfo(dialog.session)
                            _uiState.update {
                                it.copy(
                                    alertDialog = MainAlertDialog.SessionDeleteConfirmation(
                                        content = sessionInfo,
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
                        if (event.action.isOk) {
                            coroutineScope.launch {
                                _uiState.update { it.copy(alertDialog = null) }
                                delay(200)
                                _uiState.update {
                                    it.copy(
                                        alertDialog = MainAlertDialog.DeleteInactiveUserOwnDeviceConfirm(event.action.dialog.device)
                                    )
                                }
                            }
                        } else {
                            _uiState.update { it.copy(alertDialog = null) }
                        }
                    }

                    is MainAlertDialog.DeleteInactiveUserOwnDeviceConfirm -> {
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

    private fun getSessionInfo(session: UserSession): String {
        return buildString {
            append(session.deviceOrClientName)
            append("\n")

            append(session.osName)
            append(" ")
            append(session.osVersion)
            append("\n")

            session.createdAt.isoWithOffsetToInstant()?.let {
                valueFormatter.formatRelativeDateTime(
                    timeUTC = it.toLocalDateTime(TimeZone.UTC)
                )
            }?.let {
                append(MR.strings.personalSpace_session_createdAt.localizedString())
                append(" ")
                append(it)
                append("\n")
            }

            session.updatedAt.isoWithOffsetToInstant()?.let {
                valueFormatter.formatRelativeDateTime(
                    timeUTC = it.toLocalDateTime(TimeZone.UTC)
                )
            }?.let {
                append(MR.strings.personalSpace_session_updatedAt.localizedString())
                append(" ")
                append(it)
                append("\n")
            }

            session.expire.isoWithOffsetToInstant()?.let {
                valueFormatter.formatSimpleDateTime(
                    timeUTC = it.toLocalDateTime(TimeZone.UTC)
                )
            }?.let {
                append(MR.strings.personalSpace_session_expiresAt.localizedString())
                append(" ")
                append(it)
                append("\n")
            }

            append(session.countryName)
            append(", ")
            append(session.ip)
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

                if (state is Payload.Success) {
                    updateSessions()
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
                showSnackbar(e.errorString())
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
                showSnackbar(e.errorString())
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