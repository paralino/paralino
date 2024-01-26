package app.paralino.core.presentation.places.create

import app.paralino.core.models.*
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CreatePlaceViewModel(
    val groupId: String,
    placeId: String?,
    lat: Double? = null,
    lng: Double? = null,
    private val placesRepo: PlacesRepository,
    private val groupRepo: GroupRepository,
) : CoreViewModel() {


    private val placeFlow = MutableStateFlow<Payload<GroupPlace>>(Payload.Loading)
    private val groupFlow = MutableStateFlow<Resource<UserGroup>>(Resource.NotInitialized)
    private val _uiState = MutableStateFlow<Payload<CreatePlaceScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<CreatePlaceScreenState>> = _uiState

    private val _eventChannel = Channel<CreatePlaceScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    val placeNameLengthLimit = 30

    init {
        coroutineScope.launch {
            val value = when {
                placeId.isNullOrBlank() -> null
                else -> placesRepo.getPlaceLocally(placeId)
                    .map { Payload.Success(it) }
                    .firstOrNull()
            } ?: Payload.Error(NullPointerException("No such place"))

            groupFlow.update {
                Resource.Ready(
                    groupRepo.getGroupWithoutMembersLocally(groupId).first()
                )
            }

            placeFlow.update { value }
            _uiState.update {
                when {
                    placeId.isNullOrBlank() -> Payload.Success(
                        CreatePlaceScreenState.empty(
                            groupId = groupId,
                            lat = lat,
                            lng = lng
                        )
                    )

                    else -> value.mapSuccess { place ->
                        CreatePlaceScreenState(
                            groupId = groupId,
                            isNewPlace = false,
                            name = place.data.data?.name ?: "---",
                            image = place.data.data?.image ?: ImageModel.emoji(Emojis.lock),
                            hasNameError = false,
                            private = false,
                            lat = place.data.data?.lat,
                            lng = place.data.data?.lng,
                            radiusMeters = place.data.data?.radiusMeters ?: 0,
                            actionLoading = false,
                            fullLoading = false
                        )
                    }
                }
            }
        }
    }

    fun createPlace() {

        val data = _uiState.value.data ?: return

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
            type = ""
        )

        val groupUpdateFlow = if (place != null) {
            val newPlace = place.copy(data = CryptoResource.Ready(placeData))
            placesRepo.updatePlace(newPlace)
        } else {
            placesRepo.createPlace(
                groupId = groupId,
                private = data.private,
                data = placeData
            )
        }

        groupUpdateFlow
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
                        _eventChannel.send(CreatePlaceScreenEvent.Error(data.exception))
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
            it.mapSuccess { it.copy(radiusMeters = radiusMeters) }
        }
    }
}