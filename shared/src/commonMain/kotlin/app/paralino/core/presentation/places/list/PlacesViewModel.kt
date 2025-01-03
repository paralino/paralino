package app.paralino.core.presentation.places.list

import app.paralino.core.MR
import app.paralino.core.domain.notifications.IGroupActivityNotificationPresenter
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlacesViewModel(
    groupId: String,
    val isOnboarding: Boolean,
    private val remoteConfig: RemoteConfigRepository,
    private val placesRepo: PlacesRepository,
    private val groupRepo: UserGroupLocalDataSource,
    private val notificationPresenter: IGroupActivityNotificationPresenter,
    val deviceApiProvider: IDeviceApiProvider,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<PlacesListScreenState>>(Payload.Loading)
    val uiState: StateFlow<Payload<PlacesListScreenState>> = _uiState

    private val _eventChannel = Channel<PlacesScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        combine(
            groupRepo.getGroupWithoutMembers(groupId),
            placesRepo.getAndRefreshPlacesForGroup(groupId),
            deviceApiProvider.deviceStatus
        ) { group, places, deviceStatus ->

            val placesState = places.map {
                PlaceListItemData(it)
            }

            val error = if (!deviceStatus.notificationStatus.isNotificationPermissionAllowed) {
                ConditionErrorType.PermissionNotifications
            } else {
                null
            }

            val placesLimit = group.placesLimit(remoteConfig.configState.value)
            val canShowPlanBanner = group.isCurrentUserOwner && !group.isGroupPremium(
                config = remoteConfig.configState.value
            )
            val state = PlacesListScreenState(
                groupId = groupId,
                groupImage = group.image,
                groupName = group.name,
                placesLimit = placesLimit,
                isGroupOwner = group.isCurrentUserOwner,
                places = placesState,
                canShowPlacesLimit = if (group.isCurrentUserOwner) {
                    true
                } else {
                    // only show places limit to other members if close to limit
                    placesState.size >= placesLimit.minus(2)
                },
                shouldShowPlanBanner = if (isOnboarding) {
                    // in onboarding only show upgrade banner if places limit is reached
                    canShowPlanBanner && placesState.size >= placesLimit
                } else {
                    canShowPlanBanner
                },
                error = error,
                alertDialog = _uiState.value.data?.alertDialog
            )
            state
        }
            .onEach { state -> _uiState.update { Payload.Success(state) } }
            .catch { _eventChannel.send(PlacesScreenEvent.Error(it)) }
            .launchIn(coroutineScope)
    }

    fun onEvent(event: PlacesScreenUiEvent) {
        when (event) {
            PlacesScreenUiEvent.CreatePlace -> tryCreatePlace()
            is PlacesScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.PlanLimit, is MainAlertDialog.PlanLimitForMembers -> {
                        _uiState.update {
                            it.mapSuccess { it.copy(alertDialog = null) }
                        }
                    }

                    else -> {}
                }
            }

            is PlacesScreenUiEvent.PostSampleNotification -> {
                if (uiState.value.data?.error != null) {
                    _eventChannel.trySend(PlacesScreenEvent.MissingNotificationPermission)
                } else {
                    notificationPresenter.notify(
                        payload = IPushNotificationPresenter.Payload(
                            notificationId = event.content.hashCode(),
                            title = event.content,
                            message = null,
                            timeMillis = null,
                            timeInfo = null,
                            loggerTag = null
                        )
                    )
                }
            }
        }
    }

    private fun tryCreatePlace() {
        val state = _uiState.value.data ?: return
        coroutineScope.launch {
            val limit = state.placesLimit
            val placeCount = state.places.size
            val maxPlaceLimit =
                remoteConfig.configState.value.plans.maxBy { it.planTypeEnum.order }.placesLimit

            if (state.isGroupOwner && placeCount >= maxPlaceLimit) {
                _uiState.update {
                    it.mapSuccess {
                        it.copy(
                            alertDialog = MainAlertDialog.PlanLimit(
                                content = MR.strings.plan_limit_places_alert_info.localizedString(),
                                limit = limit
                            )
                        )
                    }
                }
            } else if (limit in 0..placeCount) {
                if (state.isGroupOwner) {
                    _eventChannel.send(PlacesScreenEvent.GroupPlacesLimitReached(limit = limit))
                } else {
                    _uiState.update {
                        it.mapSuccess {
                            it.copy(
                                alertDialog = MainAlertDialog.PlanLimitForMembers(
                                    content = MR.strings.plan_limit_places_alert_info.localizedString(),
                                    limit = limit
                                )
                            )
                        }
                    }
                }
            } else {
                _eventChannel.send(PlacesScreenEvent.AddPlace)
            }
        }
    }
}