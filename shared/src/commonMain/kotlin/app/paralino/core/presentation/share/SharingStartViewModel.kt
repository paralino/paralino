package app.paralino.core.presentation.share

import app.paralino.core.MR
import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.nameRes
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.SharingDurationPreset
import app.paralino.core.utils.exceptions.TimeoutException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.poll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SharingStartViewModel(
    groupId: String?,
    private val isOnboarding: Boolean,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val groupRepo: GroupRepository,
    private val sharingManager: LocationSharingManager,
    val locationStatusProvider: ILocationStatusProvider,
    private val connectionStatus: IConnectionStatusProvider,
    private val registerDevice: RegisterDeviceForSharingUseCase,
    private val userMemberIdUseCase: UserMemberIdUseCase,
    private val locationRepo: LocationLocalDataSource,
    private val userRepo: UserRepository
) : CoreViewModel() {

    private val _uiState =
        MutableStateFlow(
            SharingStartScreenState(
                groupState = if (groupId.isNullOrBlank())
                    Payload.Success(null)
                else Payload.Loading
            )
        )
    val uiState: StateFlow<SharingStartScreenState> = _uiState

    private val currentGroup = MutableStateFlow<UserGroupWithMetadata?>(null)

    private val _eventChannel = Channel<SharingStartScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    val shouldProceedToPaywall: Boolean get() {
        return isOnboarding
                && currentGroup.value?.group?.isCurrentUserOwner == true
                && !userRepo.userPlans.value.highestValidPlanConfig.planTypeEnum.isPremium
    }

    init {

        groupId?.let { setSelectedGroup(it) }

        combine(
            locationStatusProvider.status,
            connectionStatus.status,
            currentGroup.flatMapMerge {
                if (it == null) {
                    flowOf(0)
                } else {
                    locationRepo.getLocationCountForMember(
                        userMemberIdUseCase.getCurrentUserMemberIdForGroup(it.id)!!
                    )
                }
            }
        ) { locationStatus, connection, locationCount ->
            _uiState.update {
                it.copy(
                    deviceName = userMemberIdUseCase.getDefaultDeviceName(groupId),
                    devicesCount = locationCount,
                    errors = buildList {
                        addAll(locationStatus.getPossibleErrors(it.isLongTerm))

                        if (!connection.isOnline) {
                            add(ConditionErrorType.NETWORK)
                        }
                    },
                    warnings = buildList {
                        if (!locationStatus.isNotificationPermissionAllowed) {
                            add(ConditionErrorType.PERMISSION_NOTIFICATIONS)
                        }
                    }
                )
            }
        }.launchIn(coroutineScope)
    }

    fun onEvent(event: SharingStartScreenUiEvent) {
        when (event) {
            is SharingStartScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.PlanLimit, is MainAlertDialog.PlanLimitForMembers -> {
                        _uiState.update { it.copy(alertDialog = null) }
                    }

                    is MainAlertDialog.SharingBackgroundLocationMissing -> {
                        _uiState.update { it.copy(alertDialog = null) }
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            coroutineScope.launch {
                                _eventChannel.send(SharingStartScreenEvent.AskForPermission(ConditionErrorType.PERMISSION_LOCATION_BACKGROUND))
                            }
                        }
                    }

                    is MainAlertDialog.UnverifiedGroupInfo -> {
                        _uiState.update { it.copy(alertDialog = null) }
                    }

                    is MainAlertDialog.SharingErrors -> {
                        _uiState.update { it.copy(alertDialog = null) }
                        coroutineScope.launch {
                            _eventChannel.send(SharingStartScreenEvent.AdjustScrollPosition)
                            if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                                uiState.value.errors.firstOrNull()?.let {
                                    _eventChannel.send(SharingStartScreenEvent.AskForPermission(it))
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    suspend fun getGroupPickerState(): BottomSheetPickerType.GroupPickerBottomSheet {
        return BottomSheetPickerType.GroupPickerBottomSheet(
            onlyShowGroups = true,
            userPlan = userRepo.userPlans.value.highestValidPlanConfig,
            groups = getAllGroups().map { GroupItemState(it) }
        )
    }

    private suspend fun getAllGroups(): List<UserGroupWithMetadata> {
        return groupRepo.getAllGroupsLocally().firstOrNull() ?: emptyList()
    }

    fun setSelectedGroup(groupId: String) {
        coroutineScope.launch {
            getAllGroups().firstOrNull { it.id == groupId }?.let { group ->
                setSelectedGroup(group)
            } ?: let {
                currentGroup.update { null }
                _uiState.update { it.copy(groupState = Payload.Success(null)) }
            }
        }
    }

    private fun setSelectedGroup(group: UserGroupWithMetadata) {
        val groupItem = GroupItemState(group)
        currentGroup.update { group }
        _uiState.update {
            it.copy(
                groupState = Payload.Success(groupItem),
                devicesLimit = group.group.devicesLimit(remoteConfigRepo.configState.value)
            )
        }
    }

    fun startSharing() {

        if (getErrors(forLongTerm = _uiState.value.isLongTerm).isEmpty() && _uiState.value.canShare) {
            proceedWithSharing()
            return
        }

        _uiState.value.groupState.data ?: let {
            showSnackbar(MR.strings.invite_connect_alert_pickGroup.localizedString())
            return
        }

        if (_uiState.value.groupState.data?.uiType == GroupItemState.GroupUiType.UNVERIFIED) {
            _uiState.update { it.copy(alertDialog = MainAlertDialog.UnverifiedGroupInfo) }
        } else if (getErrors(forLongTerm = false).isEmpty() && _uiState.value.isLongTerm) {
            // only background location permission is missing
            _uiState.update { it.copy(alertDialog = MainAlertDialog.SharingBackgroundLocationMissing) }
        } else {
            _uiState.update { it.copy(alertDialog = MainAlertDialog.SharingErrors(uiState.value.errors.map { it.nameRes.localizedString() })) }
        }
    }

    private fun proceedWithSharing() {
        val group = _uiState.value.groupState.data ?: let {
            showSnackbar(MR.strings.invite_connect_alert_pickGroup.localizedString())
            return
        }

        coroutineScope.launch {
            _uiState.update { it.copy(loadingAction = true) }

            val limit = _uiState.value.devicesLimit
            val devicesCount = _uiState.value.devicesCount
            val maxDeviceLimit =
                remoteConfigRepo.configState.value.plans.maxBy { it.planTypeEnum.order }.devicesLimit
            val isDeviceRegistered = registerDevice.isDeviceAlreadyRegistered(groupId = group.id)

            if (group.isCurrentUserOwner && devicesCount >= maxDeviceLimit && !isDeviceRegistered) {
                _uiState.update {
                    it.copy(
                        alertDialog = MainAlertDialog.PlanLimit(
                            content = MR.strings.plan_limit_devices_alert_info.localizedString(),
                            limit = limit
                        )
                    )
                }
                _uiState.update { it.copy(loadingAction = false) }
                return@launch
            } else if (limit in 0..devicesCount && !isDeviceRegistered) {
                if (group.isCurrentUserOwner) {
                    _eventChannel.send(SharingStartScreenEvent.DevicesLimitReached(limit = limit))
                } else {
                    _uiState.update {
                        it.copy(
                            alertDialog = MainAlertDialog.PlanLimitForMembers(
                                content = MR.strings.plan_limit_devices_alert_info.localizedString(),
                                limit = limit
                            )
                        )
                    }
                }
                _uiState.update { it.copy(loadingAction = false) }
                return@launch
            }

            val contract = LocationSharingContract(
                groupId = group.id,
                endTimeUTC = LocationSharingContract.forDuration(_uiState.value.sharingTimeMillis),
                shareBattery = _uiState.value.shareBattery,
                shareSpeed = _uiState.value.shareSpeed,
                shareElevation = _uiState.value.shareElevation,
                shareNetwork = _uiState.value.shareNetwork,
                approximateLocationInMeters = _uiState.value.approximateLocationMeters.takeIf { _uiState.value.shareApproximateLocation }
            )

            try {
                registerDevice.registerDevice(
                    groupId = group.id,
                    deviceName = _uiState.value.deviceName,
                    contract = contract,
                    notifyOthers = _uiState.value.notifyOthers
                )
            } catch (e: Exception) {
                logg.e("Unable to register device", e)
                _uiState.update { it.copy(loadingAction = false) }
                showSnackbar(MR.strings.error_generic_snacbar.localizedString())
                return@launch
            }

            logg.tag("sharing_start").d("Device registered - register sharing contract")
            sharingManager.startSharing(contract)

            logg.tag("sharing_start").d("Check sharing contract validity")
            val active = try {
                poll(
                    firstDelayMillis = 500,
                    delay = 1000,
                    timeout = 3000
                ) {
                    sharingManager.isSharingActiveForGroup(group.id).takeIf { it }
                }
            } catch (e: TimeoutException) {
                logg.e("Sharing timeout", e)
                false
            }

            logg.tag("sharing_start").d("Sharing location setup finished")

            _uiState.update { it.copy(loadingAction = false) }
            coroutineScope.launch {
                if (active) {
                    _eventChannel.send(SharingStartScreenEvent.Started)
                } else {
                    showSnackbar(Exception("Unable to start sharing").errorString())
                }
            }
        }

    }

    fun turnOnApproximateLocationSharing(state: Boolean) {
        _uiState.update {
            it.copy(shareApproximateLocation = state)
        }
    }

    fun turnOnBatterySharing(state: Boolean) {
        _uiState.update {
            it.copy(shareBattery = state)
        }
    }

    fun approximateLocationChanged(index: Int) {
        _uiState.update {
            it.copy(
                approximateLocationMeters = it.approximateLocationPresetsMeters.getOrNull(index)
                    ?: it.approximateLocationMeters
            )
        }
    }

    fun sharingDurationChanged(index: Int) {
        _uiState.update {
            val time = SharingDurationPreset.presets.getOrNull(index) ?: SharingDurationPreset.hour
            it.copy(
                sharingTimeMillis = time,
                errors = getErrors(LocationSharingContract.isLongTerm(time))
            )
        }
    }

    fun turnOnSpeedSharing(state: Boolean) {
        _uiState.update {
            it.copy(shareSpeed = state)
        }
    }

    fun turnOnElevationSharing(state: Boolean) {
        _uiState.update {
            it.copy(shareElevation = state)
        }
    }

    fun turnOnNetworkSharing(state: Boolean) {
        _uiState.update {
            it.copy(shareNetwork = state)
        }
    }

    fun toggleNotifyOthers(state: Boolean) {
        _uiState.update {
            it.copy(notifyOthers = state)
        }
    }

    fun onDeviceNameChange(name: String) {
        if (_uiState.value.loadingAction) return
        _uiState.update { it.copy(deviceName = name) }
    }

    private fun getErrors(forLongTerm: Boolean): List<ConditionErrorType> {
        return buildList {
            addAll(
                locationStatusProvider.currentStatus.getPossibleErrors(forLongTerm)
            )
            if (!connectionStatus.status.value.isOnline) {
                add(ConditionErrorType.NETWORK)
            }
        }
    }
}