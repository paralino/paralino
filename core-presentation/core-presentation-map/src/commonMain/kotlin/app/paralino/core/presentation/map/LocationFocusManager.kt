package app.paralino.core.presentation.map

import app.paralino.core.domain.location.LocationRequestUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.RealtimeSubscriptionManager
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class LocationFocusManager(
    private val locationRequestUseCase: LocationRequestUseCase,
    private val realtimeManager: RealtimeSubscriptionManager,
    private val sharingManager: LocationSharingManager,
    private val deviceApiProvider: IDeviceApiProvider,
    private val coroutineScope: CoroutineScope,
    private val uiRefreshPingFlow: MutableStateFlow<Instant?>,
    private val uiFocusedStatus: MutableStateFlow<Boolean>,
    private val groupIdData: MutableStateFlow<GroupOrSpaceId>
) {

    private var uiRefreshPingSubscriptionJob: Job? = null
    private var activePingSubscriptionJob: Job? = null
    private var activeLocationBoostJob: Job? = null

    fun keepUiRefreshPing() {
        uiRefreshPingSubscriptionJob?.cancel()
        if (deviceApiProvider.appLifecycleState.value.current.activeOrInactive && uiFocusedStatus.value) {
            uiRefreshPingSubscriptionJob = coroutineScope.launch(Dispatchers.IO) {
                while (currentCoroutineContext().isActive) {
                    uiRefreshPingFlow.update { nowUTC() }
                    delay(5.seconds)
                }
            }
        }
    }

    fun keepLocationFocusOn(lifecycleData: MapScreenLifecycleData) {
        activePingSubscriptionJob?.cancel()
        activeLocationBoostJob?.cancel()

        val groupId = groupIdData.value

        if (groupId.id.isNullOrBlank()) {
            logg.v("location_performance", "Group id not valid, ignore active ping")
            return
        }

        if (!deviceApiProvider.appLifecycleState.value.current.activeOrInactive) {
            logg.v("location_performance", "App is in background, ignore active ping")
            return
        }

        if (lifecycleData.mapFocusType.isLocationHistoryFocused) {
            logg.v("location_performance", "Location history is focused, ignore active ping")
            return
        }

        activeLocationBoostJob = locationRequestUseCase.keepLocationBoost(
            groupId = groupId
        )
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        if (!uiFocusedStatus.value) {
            logg.v("location_performance", "Map Ui is not focused, ignore active ping")
            return
        }

        activePingSubscriptionJob = locationRequestUseCase.keepLocationFocus(
            groupId = groupId,
            focusType = lifecycleData.mapFocusType,
            planConfig = lifecycleData.planConfig
        )
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    fun initRealtime() {
        if (deviceApiProvider.appLifecycleState.value.current.activeOrInactive) {
            realtimeManager.startUiDataSubscription()
        } else {
            realtimeManager.cancelUiDataSubscription()
        }
    }

    fun getSharingContracts(): Flow<List<LocationSharingContract>> {
        return sharingManager.allContractsFlow.mapNotNull { it.data?.mapNotNull { it.asValidContract() } }
    }

    fun updateRefreshPing() {
        uiRefreshPingFlow.update { nowUTC() }
    }

    fun cancelJobs() {
        uiRefreshPingSubscriptionJob?.cancel()
        activePingSubscriptionJob?.cancel()
        activeLocationBoostJob?.cancel()
        realtimeManager.cancelUiDataSubscription()
    }
} 