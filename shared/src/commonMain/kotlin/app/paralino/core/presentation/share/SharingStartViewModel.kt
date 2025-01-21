package app.paralino.core.presentation.share

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.SnackbarDuration
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.nameRes
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.SharingDurationPreset
import app.paralino.core.utils.exceptions.TimeoutException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.poll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SharingStartViewModel(
    groupId: String?,
    val isOnboarding: Boolean,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val groupRepo: UserGroupLocalDataSource,
    private val sharingManager: LocationSharingManager,
    val deviceApiProvider: IDeviceApiProvider,
    private val registerDevice: RegisterDeviceForSharingUseCase,
    private val userMemberIdUseCase: UserMemberIdUseCase,
    private val locationRepo: LocationLocalDataSource,
    private val userRepo: UserRepository,
    private val appPrefs: AppPrefs
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

    val shouldProceedToPaywall: Boolean
        get() {
            return isOnboarding
                    && currentGroup.value?.group?.isCurrentUserOwner == true
                    && !userRepo.userPlans.value.highestValidPlanConfig.planTypeEnum.isPremium
        }

    init {

        groupId?.let { setSelectedGroup(it) }

        combine(
            deviceApiProvider.deviceStatus,
            currentGroup.flatMapMerge {
                if (it == null) {
                    flowOf(0)
                } else {
                    locationRepo.getLocationCountForMember(
                        groupId = it.id,
                        userId = appPrefs.userId
                    )
                }
            }
        ) { deviceStatus, locationCount ->
            _uiState.update {
                it.copy(
                    deviceName = userMemberIdUseCase.getDefaultDeviceName(groupId),
                    devicesCount = locationCount,
                    errors = buildList {
                        addAll(deviceStatus.locationStatus.getPossibleErrors(it.isLongTerm))

                        if (!deviceStatus.networkStatus.isOnline) {
                            add(ConditionErrorType.Network)
                        }
                    },
                    warnings = buildList {
                        if (!deviceStatus.notificationStatus.isNotificationPermissionAllowed) {
                            add(ConditionErrorType.PermissionNotifications)
                        }
                    }
                )
            }
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        currentGroup
            .filterNotNull()
            .flatMapMerge {
                groupRepo.getGroupWithLimits(it.group.id)
                    .onEach {
                        if (it.isGroupOverLimits(remoteConfigRepo.getConfigOnce())) {
                            if (it.group.isCurrentUserOwner) {
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
            }
            .launchIn(coroutineScope)
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
                                _eventChannel.send(
                                    SharingStartScreenEvent.AskForPermission(
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
        val allGroups = groupRepo.getAllGroupsWithMetadata().firstOrNull() ?: emptyList()
        return BottomSheetPickerType.GroupPickerBottomSheet(
            onlyShowGroups = true,
            userPlan = null,
            createdGroups = allGroups.filter { it.group.isCurrentUserOwner }
                .map { GroupItemState(it) },
            joinedGroups = allGroups.filter { !it.group.isCurrentUserOwner }
                .map { GroupItemState(it) },
        )
    }

    fun setSelectedGroup(groupId: String) {
        coroutineScope.launch {
            groupRepo.getGroupWithMetadata(groupId = groupId).firstOrNull().let { group ->
                setSelectedGroup(group)
            }
        }
    }

    private suspend fun setSelectedGroup(group: UserGroupWithMetadata?) {
        currentGroup.update { group }
        val isGroupPremium =
            group?.group?.hasPremiumSharingFeatures(remoteConfigRepo.getConfigOnce()) ?: false
        val presets = SharingDurationPreset.presets(isPremium = isGroupPremium)
        _uiState.update {
            it.copy(
                groupState = Payload.Success(group?.let { GroupItemState(it) }),
                devicesLimit = group?.group?.devicesLimit(remoteConfigRepo.getConfigOnce()) ?: 0,
                showNotifyOthers = isGroupPremium || group?.group?.isCurrentUserOwner == true,
                isPremiumSectionVisible = !isGroupPremium && group?.group?.isCurrentUserOwner == true,
                notifyOthers = if (isGroupPremium) it.notifyOthers else false,
                sharingPresetsMillis = presets,
                sharingTimeMillis = it.sharingTimeMillis.let {
                    if (isGroupPremium) it
                    else it.coerceIn(presets.first(), presets.last())
                }
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
                remoteConfigRepo.getConfigOnce().plans.maxByOrNull { it.planTypeEnum.order }?.devicesLimit
            val isDeviceRegistered = registerDevice.isDeviceAlreadyRegistered(groupId = group.id)

            if (group.isCurrentUserOwner && maxDeviceLimit != null && devicesCount >= maxDeviceLimit && !isDeviceRegistered) {
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
                    _eventChannel.send(SharingStartScreenEvent.OpenPaywall)
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
            val isGroupPremium =
                currentGroup.value?.group?.hasPremiumSharingFeatures(remoteConfigRepo.getCachedConfigOnce())
                    ?: true
            val time = SharingDurationPreset.presets(isPremium = isGroupPremium).getOrNull(index)
                ?.let {
                    if (isGroupPremium) {
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
                errors = getErrors(LocationSharingContract.isLongTerm(time))
            )
        }
    }

    fun demoSharingDurationChanged(index: Int) {
        _uiState.update {
            val time = SharingDurationPreset.presets(isPremium = true).getOrNull(index)
                ?: SharingDurationPreset.hour
            it.copy(demoSharingTimeMillis = time)
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
        if (_uiState.value.showNotifyOthers) {
            if (_uiState.value.isPremiumSectionVisible) {
                _eventChannel.trySend(SharingStartScreenEvent.OpenPaywall)
            } else {
                _uiState.update {
                    it.copy(notifyOthers = state)
                }
            }
        } else {
            _eventChannel.trySend(SharingStartScreenEvent.OpenPaywall)
        }
    }

    fun onDeviceNameChange(name: String) {
        if (_uiState.value.loadingAction) return
        _uiState.update { it.copy(deviceName = name) }
    }

    private fun getErrors(forLongTerm: Boolean): List<ConditionErrorType> {
        return buildList {
            val deviceStatus = deviceApiProvider.currentDeviceStatus()
            addAll(
                deviceStatus.locationStatus.getPossibleErrors(forLongTerm)
            )
            if (!deviceStatus.networkStatus.isOnline) {
                add(ConditionErrorType.Network)
            }
        }
    }
}