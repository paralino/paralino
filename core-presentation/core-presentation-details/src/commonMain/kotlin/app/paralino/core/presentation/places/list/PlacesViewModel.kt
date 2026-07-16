package app.paralino.core.presentation.places.list

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.notifications.IGroupActivityNotificationPresenter
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.domain.user.PremiumStatusLimitState
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.models.settings.NotificationGroupingType
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.presentation.common.SnackbarDuration
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
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
import kotlinx.coroutines.flow.mapNotNull
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
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val notificationPresenter: IGroupActivityNotificationPresenter,
    private val premiumStatusManager: PremiumStatusManager,
    val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<PlacesListScreenState>>(Payload.Loading)
    val uiState: StateFlow<Payload<PlacesListScreenState>> = _uiState

    private val _eventChannel = Channel<PlacesScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        if (groupId.asGroupOrSpaceId().isPersonalSpace) {
            loadPersonalPlaces()
        } else {
            loadGroupPlaces(groupId = groupId)
        }
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
                if (it.isGroupOwner) {
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
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun loadPersonalPlaces(): Flow<PlacesListScreenState> {
        return combine(
            personalSpaceRepo.personalSpaceState.mapNotNull { it.data },
            placesRepo.getAndRefreshPersonalPlaces(),
            deviceApiProvider.deviceStatus
        ) { space, places, deviceStatus ->

            val placesState = places.map {
                PlaceListItemData(it)
            }

            val error = if (!deviceStatus.notificationStatus.isNotificationPermissionAllowed) {
                ConditionErrorType.PermissionNotifications
            } else {
                null
            }

            val placesLimit = space.plan.personalPlacesLimit
                .takeIf { it >= 0 }
            val canShowPlanBanner = !space.plan.planTypeEnum.isPremium
            val state = PlacesListScreenState(
                groupId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId(),
                groupImage = space.user.data?.userImage ?: ImageModel.emoji(Emojis.robot),
                groupName = MR.strings.personalSpace_spaceTitle.localizedString(),
                placesLimit = placesLimit,
                isGroupOwner = true,
                places = placesState,
                canShowPlacesLimit = placesLimit != null,
                shouldShowPlanBanner = if (isOnboarding && placesLimit != null) {
                    // in onboarding only show upgrade banner if places limit is reached
                    canShowPlanBanner && placesState.size >= placesLimit
                } else {
                    canShowPlanBanner
                },
                error = error,
                androidMapSDKProvider = premiumStatusManager.defaultMapProvider(),
                alertDialog = _uiState.value.data?.alertDialog
            )
            state
        }
    }

    private fun loadGroupPlaces(groupId: String): Flow<PlacesListScreenState> {
        return combine(
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

            val placesLimit = group.placesLimit(remoteConfig.getConfigOnce())
            val canShowPlanBanner = group.isCurrentUserOwner && !group.isGroupPremium(
                config = remoteConfig.getConfigOnce()
            )
            val state = PlacesListScreenState(
                groupId = groupId.asGroupOrSpaceId(),
                groupImage = group.image,
                groupName = group.name,
                placesLimit = placesLimit,
                isGroupOwner = group.isCurrentUserOwner,
                places = placesState,
                canShowPlacesLimit = if (placesLimit == null) {
                    false
                } else if (group.isCurrentUserOwner) {
                    true
                } else {
                    // only show places limit to other members if close to limit
                    placesState.size >= placesLimit.minus(2)
                },
                shouldShowPlanBanner = if (isOnboarding && placesLimit != null) {
                    // in onboarding only show upgrade banner if places limit is reached
                    canShowPlanBanner && placesState.size >= placesLimit
                } else {
                    canShowPlanBanner
                },
                error = error,
                androidMapSDKProvider = premiumStatusManager.defaultMapProvider(),
                alertDialog = _uiState.value.data?.alertDialog
            )
            state
        }
    }

    fun onEvent(event: PlacesScreenUiEvent) {
        when (event) {
            PlacesScreenUiEvent.CreatePlace -> {
                tryCreatePlace(isExplicitCreate = true)
            }

            PlacesScreenUiEvent.OnNextClick -> {
                tryCreatePlace(isExplicitCreate = false)
            }

            is PlacesScreenUiEvent.DialogAction -> {
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

            is PlacesScreenUiEvent.PostSampleNotification -> {
                if (uiState.value.data?.error != null) {
                    _eventChannel.trySend(PlacesScreenEvent.MissingNotificationPermission)
                } else {
                    notificationPresenter.notify(
                        payload = IPushNotificationPresenter.Payload(
                            notificationId = event.content.hashCode(),
                            notificationGroupId = when (appPrefs.notificationGrouping) {
                                NotificationGroupingType.NONE -> null
                                NotificationGroupingType.EVERYTHING -> IPushNotificationPresenter.notificationGroupSystem
                                NotificationGroupingType.TYPE -> "place_activity"
                                NotificationGroupingType.GROUP -> _uiState.value.data?.groupId?.id
                                    ?: "place_activity"

                                NotificationGroupingType.PERSON -> appPrefs.userId
                            },
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

    private fun tryCreatePlace(isExplicitCreate: Boolean) {
        val state = _uiState.value.data ?: return

        if (!isExplicitCreate) {
            if (state.placesLimitReached) {
                if (isOnboarding) {
                    _eventChannel.trySend(PlacesScreenEvent.SkipOnboarding)
                    return
                }
            }
        }

        coroutineScope.launch {
            val premiumStatus = premiumStatusManager.checkPlaceCountLimits(groupId = state.groupId)

            when (premiumStatus) {
                is PremiumStatusLimitState.PlanLimitReached -> {
                    if (premiumStatus.payload.isOwner) {
                        _eventChannel.send(PlacesScreenEvent.OpenPaywall)
                    } else {
                        _uiState.update {
                            it.mapSuccess {
                                it.copy(
                                    alertDialog = MainAlertDialog.PlanLimitForMembers(
                                        content = MR.strings.plan_limit_places_alert_info.localizedString(),
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
                                    content = MR.strings.plan_limit_places_alert_info.localizedString(),
                                    limit = premiumStatus.payload.limit ?: -1
                                )
                            )
                        }
                    }
                }

                is PremiumStatusLimitState.WithinLimits -> {
                    _eventChannel.send(PlacesScreenEvent.AddPlace)
                }

                null -> {}
            }
        }
    }
}