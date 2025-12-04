package app.paralino.core.presentation.places.create

import androidx.compose.ui.text.input.TextFieldValue
import app.paralino.core.MR
import app.paralino.core.domain.group.PlaceAddressSearchUseCase
import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.domain.user.PremiumStatusLimitState
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.groups.components.SuggestionSectionChipData
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class CreatePlaceViewModel(
    val groupId: String,
    placeId: String?,
    lat: Double? = null,
    lng: Double? = null,
    private val placesRepo: PlacesRepository,
    private val placesLocalDataSource: PlacesLocalDataSource,
    private val premiumStatusManager: PremiumStatusManager,
    private val refreshEverythingUseCase: RefreshEverythingUseCase,
    private val valueFormatter: IValueFormatter,
    private val placeAddressSearch: PlaceAddressSearchUseCase,
    private val cachedLocation: LocationCacheRepository
) : CoreViewModel() {

    private val placeFlow = MutableStateFlow<Payload<GroupPlace>>(Payload.Loading)
    private val _uiState = MutableStateFlow<Payload<CreatePlaceScreenState>>(Payload.Loading)
    val uiState: StateFlow<Payload<CreatePlaceScreenState>> = _uiState

    private val _eventChannel = Channel<CreatePlaceScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()
    private var addressSearchJob: Job? = null
    private var mapPosition: LatLng? = null

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
                            lat = lat,
                            lng = lng,
                            fallbackCenterLatLng = cachedLocation.lastLocation?.let {
                                PlaceMapFallbackLatLng(
                                    location = LatLng(
                                        lat = it.lat,
                                        lng = it.lng
                                    ),
                                    isRegion = false
                                )
                            } ?: placeAddressSearch.getDefaultRegionLocation()?.let {
                                PlaceMapFallbackLatLng(
                                    location = it,
                                    isRegion = true
                                )
                            },
                            radiusMeters = CreatePlaceScreenState.defaultRadiusMeters,
                            radiusText = valueFormatter.getDistanceSmall(
                                meters = CreatePlaceScreenState.defaultRadiusMeters.toDouble(),
                                distanceType = DistanceType.DISTANCE
                            ),
                            nameSuggestions = getNameSuggestions()
                        )
                    )

                    else -> value.mapSuccess { place ->
                        CreatePlaceScreenState(
                            groupId = groupId.asGroupOrSpaceId(),
                            isNewPlace = false,
                            screenStep = CreatePlaceScreenStep.FULL,
                            name = place.data.data?.name ?: "---",
                            image = place.data.data?.image ?: ImageModel.emoji(Emojis.lock),
                            nameSuggestions = getNameSuggestions(),
                            address = TextFieldValue(text = ""),
                            addressUiMode = CreatePlaceAddressSearchUiMode.HIDDEN,
                            addressSuggestions = emptyList(),
                            isAddressLoading = false,
                            hasNameError = false,
                            private = false,
                            fallbackCenterLatLng = null,
                            lat = place.data.data?.lat,
                            lng = place.data.data?.lng,
                            radiusMeters = place.data.data?.radiusMeters ?: 0,
                            radiusText = valueFormatter.getDistanceSmall(
                                meters = place.data.data?.radiusMeters?.toDouble() ?: 0.0,
                                distanceType = DistanceType.DISTANCE
                            ),
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
                when (uiState.value.data?.screenStep) {
                    CreatePlaceScreenStep.NAME_SUGGESTION -> goToFinalCreateStep()
                    CreatePlaceScreenStep.FULL -> tryCreatePlace()
                    null -> {}
                }
            }

            is CreatePlaceScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.ServiceMaxLimit,
                    is MainAlertDialog.PlanLimitForOwner,
                    is MainAlertDialog.PlanLimitForMembers -> {
                        _uiState.update {
                            it.mapSuccess { it.copy(alertDialog = null) }
                        }
                    }

                    else -> {}
                }
            }

            is CreatePlaceScreenUiEvent.OnNameSuggestionSelected -> {
                updateNameAndImage(
                    name = event.suggestion.name,
                    image = event.suggestion.image
                )
            }

            is CreatePlaceScreenUiEvent.OnAddressSuggestionSelected -> selectSuggestion(event.suggestion)
            is CreatePlaceScreenUiEvent.OnAddressChanged -> searchAddress(event.address)
            is CreatePlaceScreenUiEvent.OnImageChanged -> updateNameAndImage(image = event.image)
            is CreatePlaceScreenUiEvent.OnNameChanged -> updateNameAndImage(name = event.name)
            is CreatePlaceScreenUiEvent.OnRadiusChanged -> updateRadius(event.radiusMeters)
            is CreatePlaceScreenUiEvent.OnMapTap -> updatePosition(lat = event.lat, lng = event.lng)
            is CreatePlaceScreenUiEvent.OnMapPositionChanged -> mapPosition = event.position

            is CreatePlaceScreenUiEvent.ChangeSearchFieldFocus -> {
                _uiState.update {
                    it.mapSuccess {
                        it.copy(
                            addressUiMode = if (event.isFocused) {
                                if (it.addressSuggestions.isEmpty()) {
                                    CreatePlaceAddressSearchUiMode.SEARCH_EXPANDED
                                } else {
                                    CreatePlaceAddressSearchUiMode.FULLY_EXPANDED
                                }
                            } else {
                                CreatePlaceAddressSearchUiMode.HIDDEN
                            }
                        )
                    }
                }

                if (event.isFocused) {
                    _eventChannel.trySend(CreatePlaceScreenEvent.RequestKeyboardFocusOnAddressInput)
                }
            }

            CreatePlaceScreenUiEvent.ToggleSearchFieldFocus -> {
                val shouldFocus =
                    _uiState.value.data?.addressUiMode == CreatePlaceAddressSearchUiMode.HIDDEN
                _uiState.update {
                    it.mapSuccess {
                        it.copy(
                            addressUiMode = if (shouldFocus) {
                                if (it.addressSuggestions.isEmpty()) {
                                    CreatePlaceAddressSearchUiMode.SEARCH_EXPANDED
                                } else {
                                    CreatePlaceAddressSearchUiMode.FULLY_EXPANDED
                                }
                            } else {
                                CreatePlaceAddressSearchUiMode.HIDDEN
                            },
                            isAddressLoading = false
                        )
                    }
                }

                if (shouldFocus) {
                    _eventChannel.trySend(CreatePlaceScreenEvent.RequestKeyboardFocusOnAddressInput)
                }
            }
        }
    }

    private fun tryCreatePlace() {

        val data = _uiState.value.data ?: return

        if (data.fullLoading || data.actionLoading) {
            return
        }

        if (data.lat == null || data.lng == null) {
            coroutineScope.launch {
                _eventChannel.send(CreatePlaceScreenEvent.MissingLocation)
            }
            return
        }

        val filteredName = data.name.trim().replace("\n", " ")
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
                        isInPersonalSpace = groupId.asGroupOrSpaceId().isPersonalSpace,
                        data = placeData
                    ).processPlaceFlow()
                }
            }
        }
    }

    private suspend fun checkPlaceLimits(): Boolean {
        val premiumStatus = premiumStatusManager.checkPlaceCountLimits(groupId.asGroupOrSpaceId())
        when (premiumStatus) {
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
                return false
            }

            is PremiumStatusLimitState.PlanLimitReached -> {
                if (premiumStatus.payload.isOwner) {
                    messageDispatcher.showSnackbar(MR.strings.places_create_plan_limit_info.localizedString())
                    _uiState.update {
                        it.mapSuccess {
                            it.copy(
                                alertDialog = MainAlertDialog.PlanLimitForOwner(
                                    content = MR.strings.plan_limit_places_alert_info.localizedString(),
                                    limit = premiumStatus.payload.limit ?: -1
                                )
                            )
                        }
                    }
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
                return false
            }

            is PremiumStatusLimitState.WithinLimits,
            null -> {
            }
        }

        return premiumStatus is PremiumStatusLimitState.WithinLimits
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
                                refreshEverythingUseCase.scheduleRefreshAllGroupsWithPlaces()
                                showSnackbar(data.exception.errorString())
                                checkPlaceLimits()
                            }
                        } else {
                            showSnackbar(data.exception.errorString())
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

    private fun goToFinalCreateStep() {
        val data = _uiState.value.data ?: return

        if (data.fullLoading || data.actionLoading) {
            return
        }

        val filteredName = data.name.trim().replace("\n", " ")
        if (filteredName.isBlank()) {
            coroutineScope.launch {
                _eventChannel.send(CreatePlaceScreenEvent.MissingInfo)
            }
            _uiState.update {
                it.mapSuccess { it.copy(hasNameError = true) }
            }
            return
        }

        _uiState.update {
            it.mapSuccess {
                it.copy(screenStep = CreatePlaceScreenStep.FULL)
            }
        }

        _eventChannel.trySend(CreatePlaceScreenEvent.RequestKeyboardFocusOnAddressInput)
    }

    private fun updateImage(image: ImageModel) {
        _uiState.update {
            it.mapSuccess { it.copy(image = image) }
        }
        updateNameAndImage(image = image)
    }

    private fun updateName(name: String) {
        updateNameAndImage(name = name)
    }

    private fun updateNameAndImage(
        name: String? = uiState.value.data?.name,
        image: ImageModel? = uiState.value.data?.image
    ) {
        val name = if ((name?.trim()?.length ?: 0) > placeNameLengthLimit) {
            uiState.value.data?.name ?: ""
        } else {
            name ?: ""
        }

        val image = image ?: uiState.value.data?.image ?: ImageModel.emoji(Emojis.circles.random())

        _uiState.update {
            it.mapSuccess {
                it.copy(
                    name = name,
                    hasNameError = false,
                    image = image
                )
            }
        }
    }

    private fun updatePosition(lat: Double, lng: Double) {
        _uiState.update {
            it.mapSuccess {
                it.copy(
                    lat = lat,
                    lng = lng,
                    addressUiMode = if (it.lat == null || it.lng == null) {
                        // if previous state didn't have location, hide search proceed
                        CreatePlaceAddressSearchUiMode.HIDDEN
                    } else {
                        it.addressUiMode
                    }
                )
            }
        }
    }

    private fun updateRadius(radiusMeters: Int) {
        _uiState.update {
            it.mapSuccess {
                it.copy(
                    radiusMeters = radiusMeters,
                    radiusText = valueFormatter.getDistanceSmall(
                        meters = radiusMeters.toDouble(),
                        distanceType = DistanceType.DISTANCE
                    )
                )
            }
        }
    }

    private fun searchAddress(address: TextFieldValue) {
        addressSearchJob?.cancel()
        addressSearchJob = null

        val address = address.copy(text = address.text.replace("\n", " "))

        _uiState.update {
            it.mapSuccess {
                it.copy(
                    address = address,
                    isAddressLoading = false,
                    addressSuggestions = if (address.text.isBlank()) {
                        emptyList()
                    } else {
                        it.addressSuggestions
                    },
                    addressUiMode = if (address.text.isBlank()) {
                        CreatePlaceAddressSearchUiMode.SEARCH_EXPANDED
                    } else {
                        it.addressUiMode
                    }
                )
            }
        }

        if (address.text.isBlank()) {
            return
        }

        addressSearchJob = coroutineScope.launch(Dispatchers.IO) {
            delay(300)
            _uiState.update {
                it.mapSuccess {
                    it.copy(isAddressLoading = true)
                }
            }
            try {
                val result = placeAddressSearch.searchAddress(
                    address = address.text.trim(),
                    latLng = mapPosition ?: uiState.value.data?.fallbackCenterLatLng?.location
                )
                val suggestions = result.results.map {
                    PlaceAddressSuggestion(
                        title = it.title,
                        info = it.address,
                        latLng = LatLng(
                            lat = it.lat,
                            lng = it.lng
                        )
                    )
                }

                _uiState.update {
                    it.mapSuccess {
                        it.copy(
                            isAddressLoading = false,
                            addressUiMode = CreatePlaceAddressSearchUiMode.FULLY_EXPANDED,
                            addressSuggestions = suggestions
                        )
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    showSnackbar(e.errorString())
                }

                _uiState.update {
                    it.mapSuccess {
                        it.copy(
                            isAddressLoading = false,
                            addressSuggestions = emptyList()
                        )
                    }
                }
            }
        }
    }

    private fun selectSuggestion(suggestion: PlaceAddressSuggestion) {
        _uiState.update {
            it.mapSuccess {
                it.copy(
                    isAddressLoading = false,
                    addressUiMode = CreatePlaceAddressSearchUiMode.HIDDEN,
                    lat = suggestion.latLng.lat,
                    lng = suggestion.latLng.lng,
                )
            }
        }
    }

    private fun getNameSuggestions(): List<SuggestionSectionChipData> {
        return buildList {
            add(
                SuggestionSectionChipData(
                    image = ImageModel.emoji(Emojis.home),
                    name = MR.strings.place_create_suggestion_home.localizedString()
                )
            )

            add(
                SuggestionSectionChipData(
                    image = ImageModel.emoji(Emojis.moneyBag),
                    name = MR.strings.place_create_suggestion_work.localizedString()
                )
            )

            add(
                SuggestionSectionChipData(
                    image = ImageModel.emoji(Emojis.schoolBooks),
                    name = MR.strings.place_create_suggestion_school.localizedString()
                )
            )

            add(
                SuggestionSectionChipData(
                    image = ImageModel.emoji(Emojis.personGym),
                    name = MR.strings.place_create_suggestion_gym.localizedString()
                )
            )

            add(
                SuggestionSectionChipData(
                    image = ImageModel.emoji(Emojis.basketball),
                    name = MR.strings.place_create_suggestion_playground.localizedString()
                )
            )

            add(
                SuggestionSectionChipData(
                    image = ImageModel.emoji(Emojis.shoppingCart),
                    name = MR.strings.place_create_suggestion_shopping.localizedString()
                )
            )

            add(
                SuggestionSectionChipData(
                    image = ImageModel.emoji(Emojis.croissant),
                    name = MR.strings.place_create_suggestion_bakery.localizedString()
                )
            )

            add(
                SuggestionSectionChipData(
                    image = ImageModel.emoji(Emojis.coffee),
                    name = MR.strings.place_create_suggestion_coffee.localizedString()
                )
            )
        }
    }

    override fun onCleared() {
        addressSearchJob?.cancel()
        addressSearchJob = null
        super.onCleared()
    }
}