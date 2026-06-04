package app.paralino.core.domain.location

import app.paralino.core.domain.LimitedExecution
import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.provider.AppLifecycleType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.user.IPushProviderManager
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

interface ILocationRequestAutoUpgradeUseCase {
    fun markAsUpgradeRequested()
    fun refreshNotificationsIfNeeded()
}

class NoOpLocationRequestAutoUpgradeUseCase : ILocationRequestAutoUpgradeUseCase {
    override fun markAsUpgradeRequested() {}
    override fun refreshNotificationsIfNeeded() {}
}

class LocationRequestAutoUpgradeUseCase(
    private val locationPerformanceManager: LocationPerformanceManager,
    private val locationSharingManager: LocationSharingManager,
    private val deviceApiProvider: IDeviceApiProvider,
    private val pushProviderManager: IPushProviderManager,
    private val scopeProvider: ICoroutineScopeProvider,
) : LimitedExecution(), ILocationRequestAutoUpgradeUseCase {

    private val executionKey = "location_request_auto_upgrade"
    private val lastUpgradeRequest = MutableStateFlow<Instant?>(null)

    override fun markAsUpgradeRequested() {
        lastUpgradeRequest.update { nowUTC() }
    }

    override fun refreshNotificationsIfNeeded() {
        val performance = locationPerformanceManager.locationPerformanceType.value.current
        val isHigherPerformance = when (performance) {
            LocationPerformanceType.HIGH,
            LocationPerformanceType.VERY_HIGH -> true

            else -> false
        }
        val isInBackground =
            deviceApiProvider.appLifecycleState.value.current == AppLifecycleType.BACKGROUND
        val hasUpcomingReset = locationPerformanceManager.isPerformanceResetScheduledIn(5.seconds)
        val acceptsRealtime = locationSharingManager.allContractsFlow.value.data
            ?.any { it.isRealtimeUpdatesOn != false }
            ?: true
        val wasUpgradeRequested = lastUpgradeRequest.value?.let {
            val diff = nowUTC() - it
            diff in 30.seconds..60.seconds
        } ?: false

        if (
            !isHigherPerformance ||
            !isInBackground ||
            !hasUpcomingReset ||
            !acceptsRealtime ||
            !wasUpgradeRequested ||
            isProcessing(executionKey)
        ) {
            return
        }

        scopeProvider.userSessionIOScope.launch {
            try {
                startProcessing(
                    key = executionKey,
                    delayedThresholdMillis = 30.seconds.inWholeMilliseconds
                )
            } catch (_: Exception) {
                return@launch
            }

            try {
                logg.tag(executionKey)
                    .v("Triggering manual push sync for background auto-upgrade")
                pushProviderManager.syncPushNotifications()
            } catch (e: Exception) {
                logg.tag(executionKey).w("Error while syncing push notifications", e)
            } finally {
                stopProcessing(executionKey)
            }
        }
    }
}
