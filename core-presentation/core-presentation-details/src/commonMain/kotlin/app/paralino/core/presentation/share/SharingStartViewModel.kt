package app.paralino.core.presentation.share

import app.paralino.core.MR
import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.domain.location.StopSharingLocationUseCase
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.domain.user.PremiumStatusLimitState
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.GroupOrUserSpace
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.BottomSheetPickerType
import app.paralino.core.presentation.common.GroupItemState
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.SnackbarDuration
import app.paralino.core.presentation.common.nameRes
import app.paralino.core.presentation.share.SharingStartScreenEvent.AskForPermission
import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.FormattingFractionsMode
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.MeasurementSystemType
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.MandatoryBackgroundLocationSharingConditionType
import app.paralino.core.utils.MandatoryForegroundLocationSharingConditionType
import app.paralino.core.utils.SharingDurationPreset
import app.paralino.core.utils.exceptions.TimeoutException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import app.paralino.core.utils.poll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SharingStartViewModel(
    val initialGroupId: GroupOrSpaceId?,
    val isOnboarding: Boolean,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val groupRepo: UserGroupLocalDataSource,
    private val sharingManager: LocationSharingManager,
    private val locationPerformanceManager: LocationPerformanceManager,
    val deviceApiProvider: IDeviceApiProvider,
    private val registerDevice: RegisterDeviceForSharingUseCase,
    private val userMemberIdUseCase: UserMemberIdUseCase,
    private val userRepo: UserRepository,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val premiumStatusManager: PremiumStatusManager,
    private val refreshEverythingUseCase: RefreshEverythingUseCase,
    private val stopSharingUseCase: StopSharingLocationUseCase,
    private val valueFormatter: IValueFormatter
) : CoreViewModel() {

    private val _uiState =
        MutableStateFlow(
            SharingStartScreenState(
                groupState = if (initialGroupId?.id.isNullOrBlank()) {
                    Payload.Success(null)
                } else {
                    Payload.Loading
                },
                canChangeGroup = initialGroupId?.id.isNullOrBlank()
            )
        )
    val uiState: StateFlow<SharingStartScreenState> = _uiState

    private val currentGroup = MutableStateFlow<GroupOrUserSpace?>(null)
    val currentGroupId: String? get() = currentGroup.value?.id

    private val _eventChannel = Channel<SharingStartScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    val isPersonalSpace: Boolean get() = currentGroup.value is GroupOrUserSpace.PersonalSpace

    val shouldProceedToPaywall: Boolean
        get() {
            val isOwner = when (val item = currentGroup.value) {
                is GroupOrUserSpace.Group -> item.group.group.isCurrentUserOwner
                is GroupOrUserSpace.PersonalSpace -> true
                null -> false
            }
            return isOnboarding
                    && isOwner
                    && !userRepo.userPlans.value.highestValidPlanConfig.isPremium
        }

    init {

        initialGroupId?.let { setSelectedGroup(it) }

        coroutineScope.launch(Dispatchers.IO) {
            _uiState.update {
                val deviceName = userMemberIdUseCase.getDefaultDeviceName(
                    groupId = currentGroup.value?.id?.asGroupOrSpaceId()
                )
                it.copy(
                    deviceName = deviceName,
                    initialDeviceName = deviceName
                )
            }
        }

        deviceApiProvider.deviceStatus
            .onEach { deviceStatus ->
                _uiState.update {
                    it.copy(
                        errors = getAllErrors(isForLongTerm = it.isLongTerm),
                        warnings = buildList {
                            if (!deviceStatus.notificationStatus.isNotificationPermissionAllowed) {
                                add(ConditionErrorType.PermissionNotifications)
                            }

                            if (!deviceStatus.locationStatus.isUnrestrictedBackgroundPermissionAllowed) {
                                add(ConditionErrorType.UnrestrictedBackgroundAccess)
                            }
                        }
                    )
                }
            }
            .launchIn(coroutineScope)

        currentGroup
            .filterNotNull()
            .onEach {
                when (it) {
                    is GroupOrUserSpace.Group -> {
                        if (it.group.isGroupOverLimits(remoteConfigRepo.getConfigOnce())) {
                            if (it.group.group.isCurrentUserOwner) {
                                showSnackbar(
                                    text = MR.strings.plan_limit_overLimit_groupOwner_banner_title.localizedString(),
                                    duration = SnackbarDuration.Forever
                                )
                            } else {
                                showSnackbar(
                                    text = MR.strings.plan_limit_overLimit_groupMember_banner_title.localizedString(),
                                    duration = SnackbarDuration.Forever
                                )
                            }
                        }
                    }

                    is GroupOrUserSpace.PersonalSpace -> {
                        if (it.space.isSpaceOverLimits()) {
                            showSnackbar(
                                text = MR.strings.plan_limit_overLimit_personalSpace_banner_title.localizedString(),
                                duration = SnackbarDuration.Forever
                            )
                        }
                    }
                }
            }
            .flatMapLatest {
                sharingManager.getUnfilteredContractForGroup(id = it.id.asGroupOrSpaceId())
                    .onEach { unfilteredContract ->
                        val currentContract = unfilteredContract?.asValidContract()
                        _uiState.update { state ->
                            state.copy(currentContract = currentContract)
                        }
                    }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    fun onEvent(event: SharingStartScreenUiEvent) {
        when (event) {
            is SharingStartScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.ServiceMaxLimit, is MainAlertDialog.PlanLimitForMembers -> {
                        _uiState.update { it.copy(alertDialog = null) }
                    }

                    is MainAlertDialog.SharingBackgroundLocationMissing -> {
                        _uiState.update { it.copy(alertDialog = null) }
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            coroutineScope.launch {
                                _eventChannel.send(
                                    AskForPermission(
                                        ConditionErrorType.PermissionLocationBackground
                                    )
                                )
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
                                    _eventChannel.send(AskForPermission(it))
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }

            SharingStartScreenUiEvent.StopSharing -> {
                stopSharing()
            }

            SharingStartScreenUiEvent.StartSharing -> {
                startSharing()
            }

            is SharingStartScreenUiEvent.ToggleNotifyOthers -> toggleNotifyOthers(state = event.state)
            is SharingStartScreenUiEvent.ToggleRealtimeUpdates -> toggleRealtimeUpdates(state = event.state)
            SharingStartScreenUiEvent.RequestSharingDurationEdit -> enableSharingDurationEdit()
        }
    }

    suspend fun getGroupPickerState(): BottomSheetPickerType.GroupPickerBottomSheet {
        val allGroups = groupRepo.getAllGroupsWithMetadata().firstOrNull() ?: emptyList()
        val personalSpace = personalSpaceRepo.personalSpaceState.value.data
        return BottomSheetPickerType.GroupPickerBottomSheet(
            onlyShowGroups = true,
            showPersonalSpace = true,
            userPlan = null,
            personalSpace = personalSpace?.let {
                GroupItemState(
                    space = it,
                    config = remoteConfigRepo.getConfigOnce()
                )
            },
            createdGroups = allGroups.filter { it.group.isCurrentUserOwner }
                .map { GroupItemState(it, remoteConfigRepo.getConfigOnce()) },
            joinedGroups = allGroups.filter { !it.group.isCurrentUserOwner }
                .map { GroupItemState(it, remoteConfigRepo.getConfigOnce()) },
        )
    }

    fun setSelectedGroup(groupOrSpaceId: GroupOrSpaceId) {
        coroutineScope.launch(Dispatchers.IO) {
            if (groupOrSpaceId.isPersonalSpace) {
                personalSpaceRepo.personalSpaceState.value.data.let { space ->
                    setSelectedGroup(space?.let { GroupOrUserSpace.PersonalSpace(it) })
                }
            } else {
                groupRepo.getGroupWithMetadata(groupId = groupOrSpaceId.id).firstOrNull()
                    .let { group ->
                        setSelectedGroup(group?.let { GroupOrUserSpace.Group(it) })
                    }
            }
        }
    }

    private suspend fun setSelectedGroup(group: GroupOrUserSpace?) {
        currentGroup.update { group }

        if (group == null) {
            val presets = SharingDurationPreset.presets(isWithLimits = false)
            _uiState.update { state ->
                state.copy(
                    groupState = Payload.Success(null),
                    devicesLimit = 0,
                    showNotifyOthers = false,
                    showDeviceStatus = false,
                    showRealtimeUpdatesStatus = false,
                    isNotifyOtherEnabledInPlan = true,
                    isRealtimeSharingEnabledInPlan = true,
                    showApproximateLocation = false,
                    notifyOthers = false,
                    realtimeUpdatesState = null,
                    sharingPresetsMillis = presets
                )
            }
            return
        }

        val unfilteredContract = sharingManager.getUnfilteredContractForGroupOnce(group.id.asGroupOrSpaceId())
        val validContract = unfilteredContract?.asValidContract()
        val isSharingActive = validContract != null
        val endTime = validContract?.let {
            if (it.isAlwaysOn) {
                if (group is GroupOrUserSpace.PersonalSpace) {
                    MR.strings.personalSpace_tracking_status_infinite.localizedString()
                } else {
                    MR.strings.group_details_sharing_status_infinite.localizedString()
                }
            } else {
                valueFormatter.fromUTCtoMapSharingTime(
                    timeUTC = it.endTimeUTC ?: "",
                    splitTimeToOneLine = false
                )?.let {
                    MR.strings.group_details_sharing_status_time.localizedString(it)
                }
            }
        }

        val premiumStatus = premiumStatusManager.checkDeviceCountLimits(groupId = group.id.asGroupOrSpaceId())
        val isDeviceRegistered = registerDevice.isDeviceAlreadyRegistered(groupId = group.id.asGroupOrSpaceId())

        when (group) {
            is GroupOrUserSpace.Group -> {
                val groupPlanConfig = group.group.group
                    .planConfig(remoteConfigRepo.getConfigOnce())
                val isGroupOverLimits = group.group.isGroupOverLimits(remoteConfigRepo.getConfigOnce())
                val presets = SharingDurationPreset.presets(isWithLimits = isGroupOverLimits)

                _uiState.update { state ->
                    state
                        .applyCurrentContract(unfilteredContract)
                        .let { state ->
                            state.copy(
                                groupState = Payload.Success(
                                    GroupItemState(
                                        group = group.group,
                                        config = remoteConfigRepo.getConfigOnce()
                                    )
                                ),
                                devicesCount = premiumStatus?.payload?.count,
                                premiumStatus = premiumStatus,
                                areLimitsExceeded = isGroupOverLimits,
                                devicesLimit = group.group.group.devicesLimit(remoteConfigRepo.getConfigOnce()),
                                isDeviceRegistered = isDeviceRegistered,

                                showNotifyOthers = !isSharingActive && (groupPlanConfig.hasSharingAlerts || group.group.group.isCurrentUserOwner),
                                showDeviceStatus = true,
                                showRealtimeUpdatesStatus = groupPlanConfig.hasOngoingRealtime || group.group.group.isCurrentUserOwner,
                                showApproximateLocation = true,
                                showDuration = !isSharingActive,
                                showMetadata = true,
                                isNotifyOtherEnabledInPlan = groupPlanConfig.hasSharingAlerts,
                                isRealtimeSharingEnabledInPlan = groupPlanConfig.hasOngoingRealtime,
                                notifyOthers = if (groupPlanConfig.hasSharingAlerts) state.notifyOthers else false,
                                realtimeUpdatesState = state.realtimeUpdatesState,

                                formattedEndTime = endTime,
                                sharingPresetsMillis = presets,
                                sharingTimeMillis = state.sharingTimeMillis.let {
                                    if (!isGroupOverLimits) it
                                    else it.coerceIn(presets.first(), presets.last())
                                }
                            )
                        }
                }
            }

            is GroupOrUserSpace.PersonalSpace -> {
                val isOverLimits = group.space.isSpaceOverLimits()
                val presets = SharingDurationPreset.presets(isWithLimits = isOverLimits)
                _uiState.update { state ->
                    state
                        .applyCurrentContract(unfilteredContract)
                        .let { state ->
                            state.copy(
                                groupState = Payload.Success(
                                    GroupItemState(
                                        space = group.space,
                                        config = remoteConfigRepo.getConfigOnce()
                                    )
                                ),
                                devicesCount = premiumStatus?.payload?.count,
                                premiumStatus = premiumStatus,
                                areLimitsExceeded = isOverLimits,
                                devicesLimit = group.space.plan.personalDevicesLimit,
                                isDeviceRegistered = isDeviceRegistered,
                                showNotifyOthers = false,
                                showDeviceStatus = false,
                                showRealtimeUpdatesStatus = false,
                                showApproximateLocation = false,
                                showDuration = false,
                                showMetadata = false,
                                formattedEndTime = endTime,
                                // nothing premium for personal space
                                isNotifyOtherEnabledInPlan = true,
                                isRealtimeSharingEnabledInPlan = true,
                                notifyOthers = false,
                                realtimeUpdatesState = null,
                                sharingPresetsMillis = presets,
                                sharingTimeMillis = state.sharingTimeMillis.let {
                                    if (!isOverLimits) it
                                    else it.coerceIn(presets.first(), presets.last())
                                }
                            )
                        }
                }
            }

            null -> {

            }
        }
    }

    private fun startSharing() {
        if (_uiState.value.shouldRegisterDevice) {
            proceedWithSharing()
        } else {
            // update
            updateCurrentContract()
        }
    }

    private fun updateCurrentContract() {
        coroutineScope.launch {
            val currentGroup = currentGroup.value
            if (currentGroup != null) {
                val contract =
                    sharingManager.currentSharingContractForGroup(id = currentGroup.id.asGroupOrSpaceId())
                val newContract = contract?.copy(
                    endTimeUTCPreference = LocationSharingContract.forDuration(_uiState.value.sharingTimeMillis),
                    shareBattery = _uiState.value.shareBattery,
                    shareSpeed = _uiState.value.shareSpeed,
                    shareElevation = _uiState.value.shareElevation,
                    shareNetwork = _uiState.value.shareNetwork,
                    shareDeviceStatus = _uiState.value.shareDeviceStatus,
                    approximateLocationInMeters = _uiState.value.approximateLocationValue.meters.takeIf { _uiState.value.shareApproximateLocation },
                    saveToHistory = _uiState.value.saveToHistory,
                    isNotifyOthersOn = _uiState.value.notifyOthers,
                    isRealtimeUpdatesOn = _uiState.value.realtimeUpdatesState ?: contract.isRealtimeUpdatesOn,
                    sharingDurationPreset = _uiState.value.sharingTimeMillis,
                )

                if (newContract != null) {
                    sharingManager.startSharing(newContract)
                    _eventChannel.send(SharingStartScreenEvent.Updated)
                }
            }
        }
    }

    private fun proceedWithSharing() {

        if (registerDevice.isProcessingAny() || uiState.value.loadingAction) {
            return
        }

        val group = _uiState.value.groupState.data ?: let {
            showSnackbar(MR.strings.invite_connect_alert_pickGroup.localizedString())
            return
        }

        if (!hasSharingStartErrors(isForLongTerm = _uiState.value.isLongTerm) && _uiState.value.canShare) {
            coroutineScope.launch {
                _uiState.update { it.copy(loadingAction = true) }
                if (didPassPlanLimits(group)) {
                    registerDeviceAndStartSharing(group)
                } else {
                    _uiState.update { it.copy(loadingAction = false) }
                }
            }
        } else if (group.uiType == GroupItemState.GroupUiType.UNVERIFIED) {
            _uiState.update { it.copy(alertDialog = MainAlertDialog.UnverifiedGroupInfo) }
        } else if (hasSharingStartErrors(isForLongTerm = false) && _uiState.value.isLongTerm) {
            // only background location permission is missing
            _uiState.update { it.copy(alertDialog = MainAlertDialog.SharingBackgroundLocationMissing) }
        } else {
            _uiState.update {
                it.copy(
                    alertDialog = MainAlertDialog.SharingErrors(
                        errorMessages = getAllErrors(isForLongTerm = uiState.value.isLongTerm)
                            .filter { it is MandatoryBackgroundLocationSharingConditionType }
                            .map { it.nameRes.localizedString() }
                    )
                )
            }
        }
    }

    private suspend fun didPassPlanLimits(group: GroupItemState): Boolean {
        val isDeviceRegistered = registerDevice.isDeviceAlreadyRegistered(groupId = group.id)

        when (val premiumStatus = _uiState.value.premiumStatus) {
            is PremiumStatusLimitState.PlanLimitReached -> {
                if (isDeviceRegistered) {
                    return true
                } else {
                    if (group.isCurrentUserOwner) {
                        _eventChannel.send(SharingStartScreenEvent.OpenPaywall)
                    } else {
                        _uiState.update {
                            it.copy(
                                alertDialog = MainAlertDialog.PlanLimitForMembers(
                                    content = MR.strings.plan_limit_devices_alert_info.localizedString(),
                                    limit = premiumStatus.payload.limit ?: -1
                                )
                            )
                        }
                    }
                    return false
                }
            }

            is PremiumStatusLimitState.ServiceLimitReached -> {
                if (isDeviceRegistered) {
                    return true
                } else {
                    _uiState.update {
                        it.copy(
                            alertDialog = MainAlertDialog.ServiceMaxLimit(
                                content = MR.strings.plan_limit_devices_alert_info.localizedString(),
                                limit = premiumStatus.payload.limit ?: -1
                            )
                        )
                    }
                    return false
                }
            }

            is PremiumStatusLimitState.WithinLimits -> return true
            null -> return false
        }
    }

    private suspend fun registerDeviceAndStartSharing(group: GroupItemState) {
        val contract = LocationSharingContract(
            groupOrSpaceId = group.id,
            endTimeUTCPreference = LocationSharingContract.forDuration(_uiState.value.sharingTimeMillis),
            endTimeUTCOverride = null,
            shareBattery = _uiState.value.shareBattery,
            shareSpeed = _uiState.value.shareSpeed,
            shareElevation = _uiState.value.shareElevation,
            shareNetwork = _uiState.value.shareNetwork,
            shareDeviceStatus = _uiState.value.shareDeviceStatus,
            approximateLocationInMeters = _uiState.value.approximateLocationValue.meters.takeIf { _uiState.value.shareApproximateLocation },
            saveToHistory = _uiState.value.saveToHistory,
            isNotifyOthersOn = _uiState.value.notifyOthers,
            isRealtimeUpdatesOn = _uiState.value.realtimeUpdatesState ?: _uiState.value.currentContract?.isRealtimeUpdatesOn ?: uiState.value.realtimeUpdatesDefaultState,
            sharingDurationPreset = _uiState.value.sharingTimeMillis,
            isEnabled = true
        )

        try {
            registerDevice.registerDevice(
                groupId = group.id,
                deviceName = _uiState.value.deviceName,
                contract = contract,
                notifyOthers = if (_uiState.value.currentContract != null) {
                    false
                } else {
                    _uiState.value.notifyOthers
                },
                isRetry = false
            )
        } catch (e: Exception) {
            logg.e("Unable to register device", e)
            _uiState.update { it.copy(loadingAction = false) }

            if (e.asRemoteException()?.code == 402) {
                if (didPassPlanLimits(group)) {
                    refreshEverythingUseCase.scheduleRefreshAllGroupsWithPlaces()
                    showSnackbar(e.errorString())
                    didPassPlanLimits(group)
                }
            } else {
                showSnackbar(e.errorString())
            }
            return
        }

        locationPerformanceManager.locationBoostRequested(
            performanceType = LocationPerformanceType.HIGH,
            groupId = contract.groupOrSpaceId,
            from = "sharing started"
        )

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

    fun turnOnApproximateLocationSharing(state: Boolean) {
        _uiState.update {
            it.copy(
                shareApproximateLocation = state,
                formattedApproximateLocation = getFormattedApproximateLocationRadius(it.approximateLocationValue)
            )
        }
    }

    fun turnOnBatterySharing(state: Boolean) {
        _uiState.update {
            it.copy(shareBattery = state)
        }
    }

    fun approximateLocationChanged(index: Int) {
        _uiState.update {
            val approximateLocationValue = it.approximateLocationPresets.getOrNull(index)
                ?: it.approximateLocationValue
            it.copy(
                approximateLocationValue = approximateLocationValue,
                formattedApproximateLocation = getFormattedApproximateLocationRadius(approximateLocationValue)
            )
        }
    }

    fun sharingDurationChanged(index: Int) {
        sharingDurationChangedMillis(
            durationMillis = SharingDurationPreset.presets(isWithLimits = false)
                .getOrNull(index)
                ?: SharingDurationPreset.hour
        )
    }

    private fun sharingDurationChangedMillis(durationMillis: Long) {
        _uiState.update {
            val isOverLimits = when (val item = currentGroup.value) {
                is GroupOrUserSpace.Group -> {
                    item.group.isGroupOverLimits(remoteConfigRepo.getCachedConfigOnce())
                }

                is GroupOrUserSpace.PersonalSpace -> {
                    item.space.isSpaceOverLimits()
                }

                null -> false
            }

            val presets = SharingDurationPreset.presets(isWithLimits = isOverLimits)
            val defaultTime = presets.firstOrNull { it == durationMillis }
                ?: presets.firstOrNull { it < durationMillis }

            val time = defaultTime
                ?.let {
                    if (!isOverLimits) {
                        it
                    } else {
                        it.coerceIn(
                            SharingDurationPreset.freePresets.first(),
                            SharingDurationPreset.freePresets.last()
                        )
                    }
                }
                ?: SharingDurationPreset.hour

            it.copy(
                sharingTimeMillis = time,
                errors = getAllErrors(isForLongTerm = LocationSharingContract.isLongTerm(time))
            )
        }
    }

    fun turnOnSpeedSharing(state: Boolean) {
        if (!_uiState.value.isSpeedElevationAvailable) {
            showSnackbar(
                MR.strings.groups_sharing_setting_approximateLocation_speedElevation_unavailable.localizedString()
            )
            return
        }
        _uiState.update {
            it.copy(shareSpeed = state)
        }
    }

    fun turnOnElevationSharing(state: Boolean) {
        if (!_uiState.value.isSpeedElevationAvailable) {
            showSnackbar(
                MR.strings.groups_sharing_setting_approximateLocation_speedElevation_unavailable.localizedString()
            )
            return
        }
        _uiState.update {
            it.copy(shareElevation = state)
        }
    }

    fun turnOnNetworkSharing(state: Boolean) {
        _uiState.update {
            it.copy(shareNetwork = state)
        }
    }

    fun turnOnDeviceStatusSharing(state: Boolean) {
        _uiState.update {
            it.copy(shareDeviceStatus = state)
        }
    }

    fun turnOnLocationHistory(state: Boolean) {
        _uiState.update {
            it.copy(saveToHistory = state)
        }
    }

    private fun toggleNotifyOthers(state: Boolean) {
        if (_uiState.value.showNotifyOthers) {
            if (!_uiState.value.isNotifyOtherEnabledInPlan) {
                if (_uiState.value.isOwner) {
                    _eventChannel.trySend(SharingStartScreenEvent.OpenPaywall)
                } else {
                    // do nothing
                }
            } else {
                _uiState.update {
                    it.copy(notifyOthers = state)
                }
            }
        } else {
            _eventChannel.trySend(SharingStartScreenEvent.OpenPaywall)
        }
    }

    private fun toggleRealtimeUpdates(state: Boolean) {
        if (!_uiState.value.isRealtimeSharingEnabledInPlan) {
            if (_uiState.value.isOwner) {
                _eventChannel.trySend(SharingStartScreenEvent.OpenPaywall)
            }
            return
        }

        _uiState.update {
            it.copy(realtimeUpdatesState = state)
        }
    }

    fun onDeviceNameChange(name: String) {
        if (_uiState.value.loadingAction) return
        if (name.trim().length > SharingStartScreenState.maxDeviceNameLength) return
        _uiState.update { it.copy(deviceName = name) }
    }

    private fun enableSharingDurationEdit() {
        val existingExpiresAt = uiState.value.currentContract?.endTimeUTC.orEmpty()
        sharingDurationChangedMillis(
            durationMillis = durationMillisForExpiresAt(existingExpiresAt)
        )
        _uiState.update { state ->
            state.copy(
                formattedEndTime = null,
                showDuration = true
            )
        }
    }

    private fun durationMillisForExpiresAt(expiresAt: String): Long {
        if (expiresAt.isBlank()) return SharingDurationPreset.infinite
        val end = expiresAt.fromUTCtoInstant() ?: return SharingDurationPreset.infinite
        val remaining = (end - nowUTC()).inWholeMilliseconds
        if (remaining <= 0L) return 8 * SharingDurationPreset.hour
        val presets = SharingDurationPreset.premiumPresets
        return presets.firstOrNull { it > 0L && it >= remaining } ?: SharingDurationPreset.hour
    }

    private fun getFormattedApproximateLocationRadius(
        approximateLocationValue: ApproximateLocationDistancePair
    ): String {
        val distance = valueFormatter.getDistanceSmall(
            meters = when (valueFormatter.measurementSystem) {
                MeasurementSystemType.METRIC -> approximateLocationValue.meters.toDouble()
                MeasurementSystemType.IMPERIAL -> approximateLocationValue.metersForImperial
            },
            distanceType = DistanceType.DISTANCE,
            fractionsMode = FormattingFractionsMode.MINIMAL
        )

        return "~ $distance"
    }

    private fun hasSharingStartErrors(isForLongTerm: Boolean): Boolean {
        return getAllErrors(isForLongTerm = isForLongTerm).any {
            if (isForLongTerm) {
                it is MandatoryBackgroundLocationSharingConditionType
            } else {
                it is MandatoryForegroundLocationSharingConditionType
            }
        }
    }

    private fun getAllErrors(isForLongTerm: Boolean): List<ConditionErrorType> {
        return buildList {
            val deviceStatus = deviceApiProvider.currentDeviceStatus()
            addAll(
                deviceStatus.locationStatus.getPossibleErrors()
                    .filter {
                        if (isForLongTerm) {
                            true
                        } else {
                            it is MandatoryForegroundLocationSharingConditionType
                        }
                    }
            )
            if (!deviceStatus.networkStatus.isOnline) {
                add(ConditionErrorType.Network)
            }
        }
    }

    private fun stopSharing() {
        val groupId = currentGroupId ?: return
        coroutineScope.launch {
            stopSharingUseCase.stopSharing(groupOrSpaceId = groupId.asGroupOrSpaceId())
            setSelectedGroup(currentGroup.value)
        }
    }

    private fun SharingStartScreenState.applyCurrentContract(unfilteredContract: LocationSharingContract?): SharingStartScreenState {
        val currentContract = unfilteredContract?.asValidContract()
        return this.copy(
            currentContract = currentContract,
            saveToHistory = unfilteredContract?.saveToHistory
                ?: this.saveToHistory,
            shareSpeed = unfilteredContract?.shareSpeed
                ?: this.shareSpeed,
            shareElevation = unfilteredContract?.shareElevation
                ?: this.shareElevation,
            shareBattery = unfilteredContract?.shareBattery
                ?: this.shareBattery,
            shareNetwork = unfilteredContract?.shareNetwork
                ?: this.shareNetwork,
            shareDeviceStatus = unfilteredContract?.shareDeviceStatus
                ?: this.shareDeviceStatus,
            notifyOthers = unfilteredContract?.isNotifyOthersOn
                ?: this.notifyOthers,
            realtimeUpdatesDefaultState = unfilteredContract?.isRealtimeUpdatesOn
                ?: this.realtimeUpdatesDefaultState,
            sharingTimeMillis = unfilteredContract?.sharingDurationPreset
                ?: this.sharingTimeMillis,
            shareApproximateLocation = unfilteredContract?.let { it.approximateLocationInMeters != null }
                ?: this.shareApproximateLocation,
            approximateLocationValue = unfilteredContract?.approximateLocationInMeters?.let {
                ApproximateLocationDistancePair.pairForSelectedMeters(meters = it)
            } ?: this.approximateLocationValue
        )
    }
}