package app.paralino.core.presentation.settings.status

import app.paralino.core.app_update.IAppUpdateManager
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.LocationUploadEngine
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.domain.user.ServerConnectionTestStep
import app.paralino.core.domain.user.ServerConnectionTestUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.DeviceStatusData
import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class AppStatusViewModel(
    val deviceApiProvider: IDeviceApiProvider,
    private val locationSharingManager: LocationSharingManager,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val locationUploadEngine: LocationUploadEngine,
    private val groupRepo: UserGroupLocalDataSource,
    private val personalSpaceLocal: UserPersonalSpaceLocalDataSource,
    val notificationServiceManager: NotificationSocketServiceManager,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val appUpdateManager: IAppUpdateManager,
    private val premiumStatusManager: PremiumStatusManager,
    private val valueFormatter: IValueFormatter,
    private val serverConnectionTest: ServerConnectionTestUseCase,
    private val appConfig: AppConfigRepository,
    private val appPrefs: AppPrefs
) : CoreViewModel() {

    private val manualBoostDuration = 30.seconds
    private val manualBoostLongDuration = 1.hours

    private val _uiState = MutableStateFlow(AppStatusScreenState())
    val uiState: StateFlow<AppStatusScreenState> = _uiState

    // values without subscription api are refreshed with every tick
    private val refreshTicker: Flow<Instant> = flow {
        while (true) {
            emit(nowUTC())
            delay(3.seconds)
        }
    }.shareIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(),
        replay = 1
    )

    init {
        combine(
            deviceApiProvider.deviceStatus.onStart { emit(deviceApiProvider.currentDeviceStatus()) },
            refreshTicker
        ) { deviceStatus, _ ->
            safeUpdate("device status") {
                deviceApiProvider.connectionStatusProvider.initAndGetStatus()
                val signalStrength = deviceApiProvider.connectionStatusProvider.getSignalStrength()
                _uiState.update {
                    it.copy(
                        requirements = mapRequirements(deviceStatus),
                        connection = AppStatusConnectionState(
                            status = deviceStatus.networkStatus.status,
                            networkType = deviceStatus.networkStatus.networkType,
                            signalStrength = NetworkSignalStrength.fromLevel(signalStrength)
                        )
                    )
                }
            }
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        combine(
            notificationServiceManager.connectionState,
            refreshTicker
        ) { connectionState, _ ->
            safeUpdate("push service") {
                _uiState.update {
                    it.copy(
                        pushService = if (notificationServiceManager.isServiceAvailableAndEnabled()) {
                            AppStatusPushServiceState(
                                connectionState = connectionState,
                                connectionType = notificationServiceManager.connectionType
                            )
                        } else {
                            null
                        }
                    )
                }
            }
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        combine(
            locationPerformanceManager.locationPerformanceType,
            refreshTicker
        ) { performance, _ ->
            safeUpdate("location mode") {
                val allowances = if (appConfig.isInAppDevMode) mapAllowances() else emptyList()
                val isThrottled = performance.isThrottlingPerformance
                val timeLeft = if (isThrottled) null else mapTimeLeft(performance.current)
                _uiState.update {
                    it.copy(
                        locationMode = AppStatusLocationModeState(
                            current = performance.current.displayNameWithIndex,
                            timeLeft = timeLeft,
                            requested = performance.requested
                                .takeIf { isThrottled }
                                ?.displayNameWithIndex,
                            isThrottled = isThrottled,
                            canBoostLong = appConfig.isInAppDevMode,
                            allowances = allowances
                        )
                    )
                }
            }
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        combine(
            locationSharingManager.allContractsFlow,
            groupRepo.getAllGroupsWithMetadata(),
            personalSpaceLocal.personalSpaceState
        ) { contracts, groups, personalSpace ->
            safeUpdate("sharing") {
                val activeIds = contracts.data.orEmpty().map { it.groupOrSpaceId }
                _uiState.update {
                    it.copy(
                        sharing = AppStatusSharingState(
                            groups = groups
                                .filter { group -> activeIds.any { id -> id.isGroup && id.id == group.id } }
                                .map { group ->
                                    AppStatusSharingTargetState(
                                        id = group.id,
                                        name = group.name,
                                        image = group.image
                                    )
                                },
                            personalSpaceImage = personalSpace.data?.let { space -> space.user.data.data?.userImage }
                                ?: ImageModel.emoji(Emojis.robot),
                            isSharingToPersonalSpace = activeIds.any { id -> id.isPersonalSpace }
                        )
                    )
                }
            }
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        combine(
            locationUploadEngine.locationFlow.debounce(100.milliseconds),
            refreshTicker
        ) { location, _ ->
            safeUpdate("pending location") {
                _uiState.update { it.copy(currentLocation = location?.let { mapLocation(it) }) }
            }
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    fun onEvent(event: AppStatusScreenUiEvent) {
        when (event) {
            AppStatusScreenUiEvent.TriggerAppUpdate -> {
                appUpdateManager.startUpdateProcess(data = remoteConfigRepository.getCachedConfigOnce())
            }

            AppStatusScreenUiEvent.TestApiConnection -> testApiConnection()

            AppStatusScreenUiEvent.ClearApiTestResult -> {
                _uiState.update { it.copy(apiTest = AppStatusApiTestState.Idle) }
            }

            is AppStatusScreenUiEvent.BoostLocation -> boostLocation(isLong = event.isLong)
        }
    }

    private fun boostLocation(isLong: Boolean) {
        if (isLong && !appConfig.isInAppDevMode) return
        val duration = if (isLong) manualBoostLongDuration else manualBoostDuration
        logg.v("Status screen manual location boost for $duration")
        appPrefs.trackingBoostEndTime = nowUTC() + duration
        locationPerformanceManager.locationBoostRequested(
            performanceType = LocationPerformanceType.MANUAL_BOOST,
            groupId = null,
            from = "App status manual boost"
        )
    }

    private fun testApiConnection() {
        if (_uiState.value.apiTest == AppStatusApiTestState.Loading) return
        _uiState.update { it.copy(apiTest = AppStatusApiTestState.Loading) }

        coroutineScope.launch(Dispatchers.IO) {
            val failedSteps = try {
                serverConnectionTest.runTest()
            } catch (e: Exception) {
                logg.w("Unable to test api connection", e)
                ServerConnectionTestStep.entries
            }
            _uiState.update {
                it.copy(
                    apiTest = if (failedSteps.isEmpty()) {
                        AppStatusApiTestState.Success
                    } else {
                        AppStatusApiTestState.Failed(failedTags = failedSteps.map { step -> step.tag })
                    }
                )
            }
        }
    }

    private fun mapRequirements(deviceStatus: DeviceStatusData): List<AppStatusRequirementState> {
        val locationStatus = deviceStatus.locationStatus
        val locationPermissionError = when {
            !locationStatus.isCoarsePermissionAllowed -> ConditionErrorType.PermissionLocation.PermissionLocationAny
            !locationStatus.isFinePermissionAllowed -> ConditionErrorType.PermissionLocation.PermissionLocationPrecise
            else -> null
        }
        val pushServiceError = notificationServiceManager.getServiceSetupError()

        return buildList {
            add(
                AppStatusRequirementState(
                    type = locationPermissionError
                        ?: ConditionErrorType.PermissionLocation.PermissionLocationAny,
                    isResolved = locationPermissionError == null
                )
            )
            add(
                AppStatusRequirementState(
                    type = ConditionErrorType.PermissionLocationBackground,
                    isResolved = locationStatus.isBackgroundPermissionAllowed,
                )
            )
            add(
                AppStatusRequirementState(
                    type = ConditionErrorType.PermissionNotifications,
                    isResolved = deviceStatus.notificationStatus.isNotificationPermissionAllowed,
                )
            )
            add(
                AppStatusRequirementState(
                    type = ConditionErrorType.UnrestrictedBackgroundAccess,
                    isResolved = locationStatus.isUnrestrictedBackgroundPermissionAllowed,
                )
            )
            add(
                AppStatusRequirementState(
                    type = ConditionErrorType.GPS,
                    isResolved = locationStatus.isGpsOn,
                )
            )
            if (notificationServiceManager.hasNotificationSocketService()) {
                add(
                    AppStatusRequirementState(
                        type = pushServiceError
                            ?: ConditionErrorType.PushNotificationService.PushNotificationServiceOff,
                        isResolved = pushServiceError == null
                    )
                )
            }
            add(
                AppStatusRequirementState(
                    type = ConditionErrorType.Network,
                    isResolved = deviceStatus.networkStatus.isOnline,
                )
            )
            add(
                AppStatusRequirementState(
                    type = ConditionErrorType.NewAppUpdate,
                    isResolved = !remoteConfigRepository.getNewAppVersionUpdateType().isNewVersionAvailable
                )
            )
        }.sortedBy { it.isResolved }
    }

    private fun mapLocation(location: LocationUploadEngine.LocationsForUpload): AppStatusCurrentLocationState? {
        val currentLocation = location.mostRecent ?: return null
        val coordinates = listOf(currentLocation.lat, currentLocation.lng).joinToString(", ") {
            valueFormatter.formatNumber(number = it, minFractions = 5, maxFractions = 5)
        }
        val time = currentLocation.timeUTC?.isoToLocalDateTime()?.let { timeUTC ->
            val formattedTime = valueFormatter.formatTimeOnly(timeUTC = timeUTC, showSeconds = true)
            val relativeTime = valueFormatter.formatRelativeDateTime(timeUTC)
                .takeIf { currentLocation.isOlderThan(2.minutes).not() }
            listOfNotNull(formattedTime, relativeTime).joinToString(" · ")
        }

        return AppStatusCurrentLocationState(
            coordinates = coordinates,
            time = time,
            accuracy = valueFormatter.getDistanceSmall(
                meters = currentLocation.rawHorizontalAccuracy.toDouble(),
                distanceType = DistanceType.DISTANCE
            ),
            isAccuracyLow = currentLocation.rawHorizontalAccuracy > 100,
            speed = currentLocation.speed?.let { valueFormatter.getSpeedSmall(it) },
            elevation = currentLocation.altitude?.let {
                valueFormatter.getDistanceSmall(
                    meters = it.toDouble(),
                    distanceType = DistanceType.ELEVATION
                )
            },
            provider = currentLocation.provider,
            isFiltered = currentLocation != location.filtered
        )
    }

    private fun mapTimeLeft(current: LocationPerformanceType): String? {
        val allowanceType = when (current) {
            LocationPerformanceType.HIGH,
            LocationPerformanceType.HIGHER_FOCUS -> LocationPerformanceType.HIGH
            LocationPerformanceType.VERY_HIGH -> LocationPerformanceType.VERY_HIGH
            LocationPerformanceType.MANUAL_BOOST -> {
                return appPrefs.trackingBoostEndTime
                    ?.let { it - nowUTC() }
                    ?.formatMinutesSeconds()
            }
            else -> return null
        }
        return locationPerformanceManager.getPerformanceAllowances()
            .firstOrNull { it.type == allowanceType }
            ?.remaining
            ?.formatMinutesSeconds()
    }

    private suspend fun mapAllowances(): List<AppStatusAllowanceState> {
        val hasBestLocationPerformance = premiumStatusManager.hasBestLocationPerformance()
        return locationPerformanceManager.getPerformanceAllowances().map { allowance ->
            val isAvailable = allowance.type != LocationPerformanceType.VERY_HIGH || hasBestLocationPerformance
            AppStatusAllowanceState(
                name = allowance.type.displayName,
                windowMinutes = allowance.window.inWholeMinutes,
                time = if (isAvailable) {
                    "${allowance.remaining.formatMinutesSeconds()} / ${allowance.allowed.formatMinutesSeconds()}"
                } else {
                    null
                },
                slots = if (isAvailable) mapAllowanceSlots(allowance) else null,
                isUsedUp = !isAvailable || !allowance.remaining.isPositive()
            )
        }
    }

    private fun mapAllowanceSlots(allowance: LocationPerformanceManager.LocationPerformanceAllowance): List<Float> {
        val slotDuration = 1.minutes
        val slotCount = (allowance.window / slotDuration).toInt().coerceAtLeast(1)
        val windowStart = allowance.windowEnd - allowance.window

        return List(slotCount) { index ->
            val slotStart = windowStart + slotDuration * index
            val slotEnd = slotStart + slotDuration
            val used = allowance.usedIntervals.fold(Duration.ZERO) { total, interval ->
                val overlap = minOf(slotEnd, interval.endInclusive) - maxOf(slotStart, interval.start)
                if (overlap.isPositive()) total + overlap else total
            }
            (used / slotDuration).toFloat().coerceIn(0f, 1f)
        }
    }

    private suspend fun safeUpdate(section: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logg.w("Unable to refresh app status $section", e)
        }
    }

    private fun Duration.formatMinutesSeconds(): String {
        val seconds = inWholeSeconds.coerceAtLeast(0)
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
}
