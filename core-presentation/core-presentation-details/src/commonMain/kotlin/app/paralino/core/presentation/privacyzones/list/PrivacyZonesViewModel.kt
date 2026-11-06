package app.paralino.core.presentation.privacyzones.list

import app.paralino.core.resources.moko.*
import app.paralino.core.domain.privacyzone.GetPrivacyZonesUseCase
import app.paralino.core.domain.user.PremiumStatusLimitState
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.models.Payload
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.presentation.common.SnackbarDuration
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.domain.privacyzone.nameOrDefault
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.utils.extensions.errorString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PrivacyZonesViewModel(
    groupId: String,
    val isOnboarding: Boolean,
    private val remoteConfig: RemoteConfigRepository,
    private val getPrivacyZonesUseCase: GetPrivacyZonesUseCase,
    private val groupRepo: UserGroupLocalDataSource,
    private val premiumStatusManager: PremiumStatusManager,
    private val valueFormatter: IValueFormatter
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<PrivacyZonesListScreenState>>(Payload.Loading)
    val uiState: StateFlow<Payload<PrivacyZonesListScreenState>> = _uiState

    private val _eventChannel = Channel<PrivacyZonesScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        loadGroupPrivacyZones(groupId = groupId)
            .onEach { state -> _uiState.update { Payload.Success(state) } }
            .catch { showSnackbar(it.errorString()) }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        _uiState
            .map { it.data }
            .filterNotNull()
            .filter { it.isOverLimit }
            .distinctUntilChanged()
            .onEach {
                showSnackbar(
                    text = MR.strings.plan_limit_overLimit_privacy_zones_banner_title.localizedString(),
                    duration = SnackbarDuration.Forever
                )
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun loadGroupPrivacyZones(groupId: String): Flow<PrivacyZonesListScreenState> {
        return combine(
            groupRepo.getGroupWithoutMembers(groupId),
            getPrivacyZonesUseCase.getAndRefreshPrivacyZonesForGroup(groupId),
        ) { group, zones ->

            val zonesState = zones.mapNotNull { zone ->
                zone.data.data?.let {
                    PlaceListItemData(
                        zone = zone,
                        zoneName = zone.nameOrDefault,
                        expirationStatus = valueFormatter.formatExpirationDateTimeUTC(
                            expiresAtTimeUTC = it.expiresAt,
                            showActiveStatus = false
                        )
                    )
                }
            }

            val privacyZonesLimit = group.privacyZonesLimit(remoteConfig.getConfigOnce())
            val canShowPlanBanner = group.isCurrentUserOwner && !group.isGroupPremium(
                config = remoteConfig.getConfigOnce()
            )
            PrivacyZonesListScreenState(
                groupId = groupId.asGroupOrSpaceId(),
                groupImage = group.image,
                groupName = group.name,
                privacyZonesLimit = privacyZonesLimit,
                isGroupOwner = group.isCurrentUserOwner,
                zones = zonesState,
                shouldShowPlanBanner = if (isOnboarding && privacyZonesLimit != null) {
                    canShowPlanBanner && zonesState.size >= privacyZonesLimit
                } else {
                    canShowPlanBanner
                },
                androidMapSDKProvider = premiumStatusManager.defaultMapProvider(),
                alertDialog = _uiState.value.data?.alertDialog
            )
        }
    }

    fun onEvent(event: PrivacyZonesScreenUiEvent) {
        when (event) {
            PrivacyZonesScreenUiEvent.CreatePrivacyZone -> tryCreatePrivacyZone()
            is PrivacyZonesScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.ServiceMaxLimit,
                    is MainAlertDialog.PlanLimitForMembers -> {
                        _uiState.update {
                            it.mapSuccess { it.copy(alertDialog = null) }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun tryCreatePrivacyZone() {
        val state = _uiState.value.data ?: return
        coroutineScope.launch {
            val premiumStatus = premiumStatusManager.checkPrivacyZoneCountLimits(groupId = state.groupId)

            when (premiumStatus) {
                is PremiumStatusLimitState.PlanLimitReached -> {
                    if (premiumStatus.payload.isOwner) {
                        _eventChannel.send(PrivacyZonesScreenEvent.OpenPaywall)
                    } else {
                        _uiState.update {
                            it.mapSuccess {
                                it.copy(
                                    alertDialog = MainAlertDialog.PlanLimitForMembers(
                                        content = MR.strings.plan_limit_privacy_zones_alert_info.localizedString(),
                                        limit = premiumStatus.payload.limit ?: -1
                                    )
                                )
                            }
                        }
                    }
                }

                is PremiumStatusLimitState.ServiceLimitReached -> {
                    _uiState.update {
                        it.mapSuccess {
                            it.copy(
                                alertDialog = MainAlertDialog.ServiceMaxLimit(
                                    content = MR.strings.plan_limit_privacy_zones_alert_info.localizedString(),
                                    limit = premiumStatus.payload.limit ?: -1
                                )
                            )
                        }
                    }
                }

                is PremiumStatusLimitState.WithinLimits -> {
                    _eventChannel.send(PrivacyZonesScreenEvent.AddPrivacyZone)
                }

                null -> {}
            }
        }
    }
}
