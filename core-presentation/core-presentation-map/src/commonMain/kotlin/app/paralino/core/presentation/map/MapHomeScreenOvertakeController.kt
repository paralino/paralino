package app.paralino.core.presentation.map

import app.paralino.core.db.AppPrefs
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.utils.extensions.isOlderThan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class MapHomeScreenOvertakeController(
    private val uiState: StateFlow<MapHomeScreenState>,
    private val eventChannel: Channel<MapHomeScreenEvent?>,
    private val uiFocusedStatus: MutableStateFlow<Boolean>,
    private val appPrefs: AppPrefs,
    private val remoteConfig: RemoteConfigRepository,
    private val coroutineScope: CoroutineScope
) {
    fun startMapHomeViewOvertakeSub() {
        combine(
            uiState,
            uiFocusedStatus
        ) { state, isUiFocused ->
            state to isUiFocused
        }
            .distinctUntilChanged()
            .debounce(2.seconds)
            .onEach { (state, isUiFocused) ->
                if (isUiFocused) {
                    processPossibleMapOvertake(state)
                }
            }
            .launchIn(coroutineScope)
    }

    private fun processPossibleMapOvertake(state: MapHomeScreenState) {
        val overtakeType: MapOvertakeScreenType? = buildFreePlanPromoOvertake(state)

        when (overtakeType) {
            is MapOvertakeScreenType.FreePromo -> {
                appPrefs.dismissedFreePlanPromoBillingId = overtakeType.promoBillingId
                eventChannel.trySend(MapHomeScreenEvent.OpenPaywall(type = BillingScreenSourceType.UPGRADE_BILLING_PROMO))
            }

            null -> {
                // no overtake
            }
        }
    }

    private fun buildFreePlanPromoOvertake(state: MapHomeScreenState): MapOvertakeScreenType.FreePromo? {
        if (state !is MapHomeScreenState.Ready) return null
        if (state.isPersonalSpace) return null
        if (!state.isCurrentUserOwnerOfSelectedGroup) return null
        if (state.groupPlanConfig.isPremium) return null

        val appReadyTime = appPrefs.detectedAppReadyTime
        if (appReadyTime == null || !appReadyTime.isOlderThan(2.days)) return null

        val promoBillingId = remoteConfig.getCachedConfigOnce().freePlanPromoBillingId
            ?.takeIf { it.isNotBlank() }
            ?: return null

        if (promoBillingId == appPrefs.dismissedFreePlanPromoBillingId) return null

        return MapOvertakeScreenType.FreePromo(promoBillingId = promoBillingId)
    }

    private sealed interface MapOvertakeScreenType {
        data class FreePromo(val promoBillingId: String) : MapOvertakeScreenType
    }
}