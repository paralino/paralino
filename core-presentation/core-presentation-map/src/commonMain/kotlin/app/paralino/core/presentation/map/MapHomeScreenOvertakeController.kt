package app.paralino.core.presentation.map

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.group.PendingGroupJoinWelcomeUseCase
import app.paralino.core.models.group.PendingGroupJoinWelcome
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MapHomeScreenOvertakeController(
    private val uiState: StateFlow<MapHomeScreenState>,
    private val eventChannel: Channel<MapHomeScreenEvent?>,
    private val uiFocusedStatus: MutableStateFlow<Boolean>,
    private val appPrefs: AppPrefs,
    private val remoteConfig: RemoteConfigRepository,
    private val pendingGroupJoinWelcome: PendingGroupJoinWelcomeUseCase,
    private val coroutineScope: CoroutineScope
) {
    private var alreadyOpenedWelcomeGroupId: String? = null

    fun startMapHomeViewOvertakeSub() {
        combine(
            uiState,
            uiFocusedStatus,
            pendingGroupJoinWelcome.observeAll()
        ) { state, isUiFocused, pending ->
            Triple(state, isUiFocused, pending)
        }
            .distinctUntilChanged()
            .debounce(500.milliseconds)
            .map { (state, isUiFocused, pending) ->
                if (isUiFocused) {
                    getPossibleOvertake(state = state, pending = pending)
                } else {
                    alreadyOpenedWelcomeGroupId = null
                    null
                }
            }
            .debounce { overtakeType ->
                when (overtakeType) {
                    is MapOvertakeScreenType.FreePromo -> 2.seconds
                    is MapOvertakeScreenType.GroupJoinWelcome -> 0.seconds
                    null -> 0.seconds
                }
            }
            .onEach { overtakeType ->
                if (overtakeType != null) {
                    processPossibleMapOvertake(overtakeType)
                }
            }
            .launchIn(coroutineScope)
    }

    private fun getPossibleOvertake(
        state: MapHomeScreenState,
        pending: List<PendingGroupJoinWelcome>
    ): MapOvertakeScreenType? {
        return buildFreePlanPromoOvertake(state = state)
            ?: buildGroupJoinWelcomeOvertake(pending = pending, state = state)
    }

    private suspend fun processPossibleMapOvertake(
        overtakeType: MapOvertakeScreenType
    ) {
        when (overtakeType) {
            is MapOvertakeScreenType.GroupJoinWelcome -> {
                if (alreadyOpenedWelcomeGroupId == overtakeType.groupId) return
                alreadyOpenedWelcomeGroupId = overtakeType.groupId
                pendingGroupJoinWelcome.markShown(groupId = overtakeType.groupId)
                eventChannel.trySend(MapHomeScreenEvent.OpenGroupJoinWelcome(groupId = overtakeType.groupId))
            }

            is MapOvertakeScreenType.FreePromo -> {
                appPrefs.dismissedFreePlanPromoBillingId = overtakeType.promoBillingId
                eventChannel.trySend(MapHomeScreenEvent.OpenPaywall(type = BillingScreenSourceType.UPGRADE_BILLING_PROMO))
            }
        }
    }

    private fun buildGroupJoinWelcomeOvertake(
        pending: List<PendingGroupJoinWelcome>,
        state: MapHomeScreenState
    ): MapOvertakeScreenType.GroupJoinWelcome? {
        if (pending.isEmpty()) return null

        val selectedPending = state.selectedGroupId
            ?.takeIf { !state.isPersonalSpace }
            ?.let { selectedGroupId -> pending.firstOrNull { it.groupId == selectedGroupId } }

        if (selectedPending != null) {
            return MapOvertakeScreenType.GroupJoinWelcome(groupId = selectedPending.groupId)
        }

        val firstUnshown = pending.firstOrNull { !it.didShowWelcome } ?: return null
        return MapOvertakeScreenType.GroupJoinWelcome(groupId = firstUnshown.groupId)
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
        data class GroupJoinWelcome(val groupId: String) : MapOvertakeScreenType
    }
}
