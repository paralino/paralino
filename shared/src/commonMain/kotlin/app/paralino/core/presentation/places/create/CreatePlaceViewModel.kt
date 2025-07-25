package app.paralino.core.presentation.places.create

import androidx.compose.ui.text.AnnotatedString
import app.paralino.core.MR
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.Payload
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.annotatedBold
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreatePlaceViewModel(
    val groupId: String,
    placeId: String?,
    lat: Double? = null,
    lng: Double? = null,
    private val placesRepo: PlacesRepository,
    private val placesLocalDataSource: PlacesLocalDataSource,
    private val groupLocal: UserGroupLocalDataSource,
    private val remoteConfig: RemoteConfigRepository,
    private val deviceApiProvider: IDeviceApiProvider,
    private val valueFormatter: IValueFormatter
) : CoreViewModel() {

    private val placeFlow = MutableStateFlow<Payload<GroupPlace>>(Payload.Loading)
    private val _uiState = MutableStateFlow<Payload<CreatePlaceScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<CreatePlaceScreenState>> = _uiState

    private val _eventChannel = Channel<CreatePlaceScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    val placeNameLengthLimit = 30

    init {
        coroutineScope.launch {
            val value = when {
                placeId.isNullOrBlank() -> null
                else -> placesLocalDataSource.getPlaceWithoutSettingsOnce(placeId)
                    ?.let { Payload.Success(it) }
            } ?: Payload.Error(NullPointerException("No such place"))

            placeFlow.update { value }
            _uiState.update {
                when {
                    placeId.isNullOrBlank() -> Payload.Success(
                        CreatePlaceScreenState.empty(
                            groupId = groupId.asGroupOrSpaceId(),
                            hintText = hintText(),
                            lat = lat,
                            lng = lng,
                            radiusMeters = CreatePlaceScreenState.defaultRadiusMeters,
                            radiusText = valueFormatter.getDistanceSmall(meters = CreatePlaceScreenState.defaultRadiusMeters.toDouble())
                        )
                    )

                    else -> value.mapSuccess { place ->
                        CreatePlaceScreenState(
                            groupId = groupId.asGroupOrSpaceId(),
                            isNewPlace = false,
                            name = place.data.data?.name ?: "---",
                            image = place.data.data?.image ?: ImageModel.emoji(Emojis.lock),
                            hasNameError = false,
                            private = false,
                            lat = place.data.data?.lat,
                            lng = place.data.data?.lng,
                            radiusMeters = place.data.data?.radiusMeters ?: 0,
                            radiusText = valueFormatter.getDistanceSmall(
                                meters = place.data.data?.radiusMeters?.toDouble() ?: 0.0
                            ),
                            hintText = hintText(),
                            actionLoading = false,
                            fullLoading = false,
                            alertDialog = null
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: CreatePlaceScreenUiEvent) {
        when (event) {
            CreatePlaceScreenUiEvent.CreatePlace -> {
                tryCreatePlace()
            }

            is CreatePlaceScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.PlanLimit, is MainAlertDialog.PlanLimitForMembers -> {
                        _uiState.update {
                            it.mapSuccess { it.copy(alertDialog = null) }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun tryCreatePlace() {

        val data = _uiState.value.data ?: return

        if (data.fullLoading || data.actionLoading) {
            return
        }

        val filteredName = data.name.trim().replace("\n", " ")

        if (data.lat == null || data.lng == null) {
            coroutineScope.launch {
                _eventChannel.send(CreatePlaceScreenEvent.MissingLocation)
            }
            return
        }

        if (filteredName.isBlank()) {
            coroutineScope.launch {
                _eventChannel.send(CreatePlaceScreenEvent.MissingInfo)
            }
            _uiState.update {
                it.mapSuccess { it.copy(hasNameError = true) }
            }
            return
        }

        val place = placeFlow.value.data
        val placeData = GroupPlaceData(
            name = data.name,
            image = data.image,
            lat = data.lat,
            lng = data.lng,
            radiusMeters = data.radiusMeters,
            expiresAt = "",
            type = "",
            eventSeed = place?.data?.data?.eventSeed
        )

        if (place != null) {
            val newPlace = place.copy(data = CryptoResource.Ready(placeData))
            placesRepo.updatePlace(newPlace)
                .processPlaceFlow()
        } else {
            coroutineScope.launch {
                if (checkPlaceLimits()) {
                    placesRepo.createPlace(
                        groupId = groupId,
                        private = data.private,
                        // TODO add personal place creation
                        isInPersonalSpace = false,
                        data = placeData
                    ).processPlaceFlow()
                }
            }
        }
    }

    private suspend fun checkPlaceLimits(): Boolean {
        val group = groupLocal.getGroupOnce(groupId) ?: return false
        val limit = group.placesLimit(remoteConfig.getConfigOnce())
        val placeCount = placesLocalDataSource.getPlacesCountForGroupOnce(groupId) ?: 0
        val isGroupOwner = group.isCurrentUserOwner
        val maxPlaceLimit =
            remoteConfig.getConfigOnce().plans.maxByOrNull { it.planTypeEnum.order }?.placesLimit

        if (maxPlaceLimit != null && isGroupOwner && placeCount >= maxPlaceLimit) {
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
            return false
        } else if (limit in 0..placeCount) {
            if (isGroupOwner) {
                messageDispatcher.showSnackbar(MR.strings.places_create_plan_limit_info.localizedString())
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
            return false
        } else {
            return true
        }
    }

    private fun Flow<Unit>.processPlaceFlow() {
        this
            .mapToPayload<Unit, Unit>()
            .onEach { data ->
                _uiState.update {
                    it.mapSuccess {
                        it.copy(
                            actionLoading = data is Payload.Loading,
                            hasNameError = false
                        )
                    }
                }
                when (data) {
                    is Payload.Error -> {
                        if (data.exception.asRemoteException()?.code == 402) {
                            if (checkPlaceLimits()) {
                                _eventChannel.send(CreatePlaceScreenEvent.Error(data.exception))
                            }
                        } else {
                            _eventChannel.send(CreatePlaceScreenEvent.Error(data.exception))
                        }
                    }

                    Payload.Loading -> {}
                    is Payload.Success -> {
                        _eventChannel.send(CreatePlaceScreenEvent.PlaceUpdated)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun updateImage(image: ImageModel) {
        _uiState.update {
            it.mapSuccess { it.copy(image = image) }
        }
    }

    fun updateName(name: String) {
        if (name.trim().length > placeNameLengthLimit)
            return
        _uiState.update {
            it.mapSuccess { it.copy(name = name, hasNameError = false) }
        }
    }

    fun updatePosition(lat: Double, lng: Double) {
        _uiState.update {
            it.mapSuccess { it.copy(lat = lat, lng = lng) }
        }
    }

    fun updateRadius(radiusMeters: Int) {
        _uiState.update {
            it.mapSuccess {
                it.copy(
                    radiusMeters = radiusMeters,
                    radiusText = valueFormatter.getDistanceSmall(meters = radiusMeters.toDouble()),
                )
            }
        }
    }

    private fun hintText(): AnnotatedString {
        return when (deviceApiProvider.deviceType) {
            DeviceType.Android -> MR.strings.places_create_location_hint_longpress.localizedString()
                .annotatedBold()

            DeviceType.iOS -> MR.strings.places_create_location_hint_tap.localizedString()
                .annotatedBold()
        }
    }
}