package app.paralino.core.presentation.personal_space

import app.paralino.core.MR
import app.paralino.core.app_rating.AppRatingManager
import app.paralino.core.app_rating.AppRatingSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.domain.location.StopSharingLocationUseCase
import app.paralino.core.domain.user.DeactivatePersonalDeviceUseCase
import app.paralino.core.domain.user.DeletePersonalDeviceUseCase
import app.paralino.core.domain.user.PersonalSpaceRefreshUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.AppSnackbarData
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.presentation.common.SnackbarDuration
import app.paralino.core.presentation.common.onAction
import app.paralino.core.presentation.groups.detail.GroupSpaceFocusedFeatureChildItem
import app.paralino.core.presentation.groups.detail.LocationSharingState
import app.paralino.core.presentation.share.ApproximateLocationDistancePair
import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.MeasurementSystemType
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.MandatoryBackgroundLocationSharingConditionType
import app.paralino.core.utils.MandatoryForegroundLocationSharingConditionType
import app.paralino.core.utils.extensions.combine
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonalSpaceViewModel(
    private val userRepo: UserRepository,
    private val personalSpaceLocal: UserPersonalSpaceLocalDataSource,
    private val deletePersonalDevice: DeletePersonalDeviceUseCase,
    private val deactivatePersonalDevice: DeactivatePersonalDeviceUseCase,
    private val personalSpaceRefreshUseCase: PersonalSpaceRefreshUseCase,
    private val sharingManager: LocationSharingManager,
    private val placesRepo: PlacesRepository,
    private val valueFormatter: IValueFormatter,
    val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs,
    private val appRatingManager: AppRatingManager,
    private val geofenceManager: IGeofenceManager,
    private val locationCacheRepository: LocationCacheRepository,
    private val stopSharingUseCase: StopSharingLocationUseCase,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<PersonalSpaceScreenState>(
        PersonalSpaceScreenState(
            userName = userRepo.localUser.value.data?.name ?: "",
            userImage = userRepo.localUser.value.data?.data?.data?.userImage,
            isLoadingData = true,
            loadingAction = false,
            sharingStatus = null,
            devicesWithSessions = emptyList(),
            placesFeatureItems = emptyList(),
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

    private val devicesHandler = UserDevicesListHandler(
        userRepo = userRepo,
        deletePersonalDevice = deletePersonalDevice,
        deactivatePersonalDevice = deactivatePersonalDevice,
        personalSpaceRefreshUseCase = personalSpaceRefreshUseCase,
        valueFormatter = valueFormatter,
        appPrefs = appPrefs,
        geofenceManager = geofenceManager,
        coroutineScope = coroutineScope,
        messageDispatcher = messageDispatcher,
        onDevicesChanged = { devices ->
            _uiState.update { it.copy(devicesWithSessions = devices) }
        },
        onAlertDialogChanged = { dialog ->
            _uiState.update { it.copy(alertDialog = dialog) }
        },
        onLoadingChanged = { loading ->
            _uiState.update { it.copy(loadingAction = loading) }
        },
    )

    private val _eventChannel = Channel<PersonalSpaceScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        appRatingManager.tryShowingAppRating(source = AppRatingSource.PERSONAL_SPACE)

        devicesHandler.refreshSessions()

        combine(
            deviceApiProvider.deviceStatus,
            personalSpaceLocal.personalSpaceState.mapNotNull { it.data },
            sharingManager.sharingContractForGroup(UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()),
            userRepo.localUser.mapNotNull { it.data },
            userRepo.userPlans,
            placesRepo.getPlacesForGroupLocally(groupId = appPrefs.userId),
            locationCacheRepository.lastLocationFlow,
        ) { deviceStatus, personalSpaceData, sharingStatus, user, userPlans, places, myLocation ->

            val errors = buildList {
                sharingStatus?.let { status ->
                    if (!deviceStatus.networkStatus.isOnline) {
                        add(ConditionErrorType.Network)
                    }

                    addAll(
                        deviceStatus.locationStatus.getPossibleErrors()
                            .filter {
                                if (status.isLongTerm) {
                                    it is MandatoryBackgroundLocationSharingConditionType
                                } else {
                                    it is MandatoryForegroundLocationSharingConditionType
                                }
                            }
                    )
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
                        ApproximateLocationDistancePair.pairForSelectedMeters(meters = it)
                    }?.let {
                        valueFormatter.getDistanceSmall(
                            meters = when (valueFormatter.measurementSystem) {
                                MeasurementSystemType.METRIC -> it.meters.toDouble()
                                MeasurementSystemType.IMPERIAL -> it.metersForImperial
                            },
                            distanceType = DistanceType.DISTANCE
                        )
                    }
                )
            }

            val devicesLimit = personalSpaceData.plan.personalDevicesLimitForDisplay(
                currentCount = personalSpaceData.activeDeviceCount
            )

            val placesData = places
                .filter { it.data.data?.lat != null && it.data.data?.lng != null }
                .map { PlaceListItemData(it) }

            devicesHandler.publishDevices(
                devices = personalSpaceData.devices,
                places = placesData,
                myLocation = myLocation,
            )

            val featureItemsPlaces = placesData.map {
                GroupSpaceFocusedFeatureChildItem(
                    itemId = it.id,
                    groupOrSpaceId = it.groupOrSpaceId,
                    name = it.name,
                    image = it.image
                )
            }

            _uiState.update { state ->
                state.copy(
                    userName = user.name,
                    userImage = user.data.data?.userImage,
                    isLoadingData = false,
                    sharingStatus = sharingState,
                    devicesWithSessions = devicesHandler.latestDevicesState(),
                    devicesLimit = devicesLimit,
                    placesFeatureItems = featureItemsPlaces,
                    shouldShowPlanBanner = !userPlans.highestValidPlanConfig.isPremium,
                    arePlacesOverLimit = personalSpaceData.arePlacesOverLimits(),
                    isSpaceOverLimit = personalSpaceData.isSpaceOverLimits(),
                    errors = errors,
                    plan = personalSpaceData.plan
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
                showSnackbarForResult(
                    data = AppSnackbarData(
                        text = MR.strings.plan_limit_overLimit_personalSpace_banner_title.localizedString(),
                        duration = SnackbarDuration.Forever,
                        actionText = MR.strings.plans_banner_settings_action_small.localizedString()
                    )
                )?.result?.onAction {
                    _eventChannel.trySend(PersonalSpaceScreenEvent.OpenPaywall)
                }
            }
            .launchIn(coroutineScope)
    }

    fun onEvent(event: PersonalSpaceScreenUiEvent) {
        when (event) {
            is PersonalSpaceScreenUiEvent.OnDeviceEvent -> {
                devicesHandler.onDeviceEvent(event.event)
            }

            PersonalSpaceScreenUiEvent.OpenPlaces -> {
                coroutineScope.launch {
                    _eventChannel.send(PersonalSpaceScreenEvent.OpenPlaces)
                }
            }

            is PersonalSpaceScreenUiEvent.OpenPlace -> {
                coroutineScope.launch {
                    _eventChannel.send(PersonalSpaceScreenEvent.OpenPlace(event.item))
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
                devicesHandler.onDialogAction(event.action)
            }

            PersonalSpaceScreenUiEvent.OpenPaywall -> {
                coroutineScope.launch {
                    _eventChannel.send(PersonalSpaceScreenEvent.OpenPaywall)
                }
            }
        }
    }

    fun stopSharing() {
        coroutineScope.launch {
            stopSharingUseCase.stopSharing(groupOrSpaceId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId())
        }
    }
} 