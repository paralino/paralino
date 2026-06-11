package app.paralino.core.presentation.privacyzones.create

import androidx.compose.ui.text.input.TextFieldValue
import app.paralino.core.MR
import app.paralino.core.domain.group.PlaceAddressSearchUseCase
import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.domain.privacyzone.CreatePrivacyZoneUseCase
import app.paralino.core.domain.privacyzone.UpdatePrivacyZoneUseCase
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
import app.paralino.core.models.privacyzones.GroupPrivacyZone
import app.paralino.core.models.privacyzones.PrivacyZoneData
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.SuggestionSectionChipData
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.ScreenDimensionType
import app.paralino.core.presentation.places.create.PlaceAddressSearchUiMode
import app.paralino.core.presentation.places.create.PlaceAddressSuggestion
import app.paralino.core.presentation.places.create.PlaceMapFallbackLatLng
import app.paralino.core.presentation.share.ApproximateLocationDistancePair
import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.FormattingFractionsMode
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.MeasurementSystemType
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.privacyzones.PrivacyZonesLocalDataSource
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

class CreatePrivacyZoneViewModel(
    val groupId: String,
    zoneId: String?,
    lat: Double? = null,
    lng: Double? = null,
    private val createPrivacyZoneUseCase: CreatePrivacyZoneUseCase,
    private val updatePrivacyZoneUseCase: UpdatePrivacyZoneUseCase,
    private val privacyZonesLocalDataSource: PrivacyZonesLocalDataSource,
    private val premiumStatusManager: PremiumStatusManager,
    private val refreshEverythingUseCase: RefreshEverythingUseCase,
    private val valueFormatter: IValueFormatter,
    private val placeAddressSearch: PlaceAddressSearchUseCase,
    private val cachedLocation: LocationCacheRepository
) : CoreViewModel() {

    private val zoneFlow = MutableStateFlow<Payload<GroupPrivacyZone>>(Payload.Loading)
    private val _uiState = MutableStateFlow<Payload<CreatePrivacyZoneScreenState>>(Payload.Loading)
    val uiState: StateFlow<Payload<CreatePrivacyZoneScreenState>> = _uiState

    private val _eventChannel = Channel<CreatePrivacyZoneScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private val uiSizeState = MutableStateFlow<ScreenDimensionType?>(null)
    private var addressSearchJob: Job? = null
    private var mapPosition: LatLng? = null
    val isNewZone: Boolean = zoneId.isNullOrBlank()

    val zoneNameLengthLimit = 30

    init {
        coroutineScope.launch {
            val value = when {
                zoneId.isNullOrBlank() -> null
                else -> privacyZonesLocalDataSource.getPrivacyZoneOnce(zoneId)
                    ?.let { Payload.Success(it) }
            } ?: Payload.Error(NullPointerException("No such privacy zone"))

            zoneFlow.update { value }
            _uiState.update {
                when {
                    zoneId.isNullOrBlank() -> Payload.Success(
                        CreatePrivacyZoneScreenState.empty(
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
                            radiusValue = ApproximateLocationDistancePair.default,
                            radiusText = formatRadius(ApproximateLocationDistancePair.default),
                            androidMapSDKProvider = premiumStatusManager.defaultMapProvider(),
                            nameSuggestions = getNameSuggestions()
                        )
                    )

                    else -> value.mapSuccess { zone ->
                        val zoneData = zone.data.data
                        val radiusValue = ApproximateLocationDistancePair.pairForSelectedMeters(
                            zoneData?.radiusMeters ?: ApproximateLocationDistancePair.default.meters
                        ) ?: ApproximateLocationDistancePair.default

                        CreatePrivacyZoneScreenState(
                            groupId = groupId.asGroupOrSpaceId(),
                            isNewZone = false,
                            screenStep = CreatePrivacyZoneScreenStep.NAME_SUGGESTION,
                            name = zoneData?.name ?: "---",
                            image = zoneData?.image ?: ImageModel.emoji(Emojis.lock),
                            nameSuggestions = emptyList(),
                            address = TextFieldValue(text = ""),
                            addressUiMode = PlaceAddressSearchUiMode.HIDDEN,
                            addressSuggestions = emptyList(),
                            isAddressLoading = false,
                            hasNameError = false,
                            notifyOthersInZone = zoneData?.notifyOthersInZone ?: false,
                            inZoneMessage = zoneData?.inZoneMessage,
                            fallbackCenterLatLng = null,
                            lat = zoneData?.lat,
                            lng = zoneData?.lng,
                            radiusPresets = ApproximateLocationDistancePair.pairs,
                            radiusValue = radiusValue,
                            radiusText = formatRadius(radiusValue),
                            actionLoading = false,
                            fullLoading = false,
                            isSatelliteModeEnabled = false,
                            androidMapSDKProvider = premiumStatusManager.defaultMapProvider(),
                            alertDialog = null,
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: CreatePrivacyZoneScreenUiEvent) {
        when (event) {
            is CreatePrivacyZoneScreenUiEvent.OnUiReady -> {
                uiSizeState.update { event.size }
            }

            CreatePrivacyZoneScreenUiEvent.CreatePrivacyZone -> {
                when (uiState.value.data?.screenStep) {
                    CreatePrivacyZoneScreenStep.NAME_SUGGESTION -> goToFinalCreateStep()
                    CreatePrivacyZoneScreenStep.FULL -> tryCreatePrivacyZone()
                    null -> {}
                }
            }

            is CreatePrivacyZoneScreenUiEvent.DialogAction -> {
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

            is CreatePrivacyZoneScreenUiEvent.OnNameSuggestionSelected -> {
                updateNameAndImage(
                    name = event.suggestion.name,
                    image = event.suggestion.image
                )
            }

            is CreatePrivacyZoneScreenUiEvent.OnAddressSuggestionSelected -> selectSuggestion(event.suggestion)
            is CreatePrivacyZoneScreenUiEvent.OnAddressChanged -> searchAddress(event.address)
            is CreatePrivacyZoneScreenUiEvent.OnImageChanged -> updateNameAndImage(image = event.image)
            is CreatePrivacyZoneScreenUiEvent.OnNameChanged -> updateNameAndImage(name = event.name)
            is CreatePrivacyZoneScreenUiEvent.OnNotifyOthersInZoneChanged -> updateNotifyOthersInZone(event.enabled)
            is CreatePrivacyZoneScreenUiEvent.OnInZoneMessageChanged -> updateInZoneMessage(event.message)
            is CreatePrivacyZoneScreenUiEvent.OnRadiusChanged -> updateRadius(event.index)
            is CreatePrivacyZoneScreenUiEvent.OnMapTap -> updatePosition(lat = event.lat, lng = event.lng)
            is CreatePrivacyZoneScreenUiEvent.OnMapPositionChanged -> mapPosition = event.position
            is CreatePrivacyZoneScreenUiEvent.OnMapModeSwitch -> {
                _uiState.update {
                    it.mapSuccess {
                        it.copy(isSatelliteModeEnabled = !it.isSatelliteModeEnabled)
                    }
                }
            }

            is CreatePrivacyZoneScreenUiEvent.ChangeSearchFieldFocus -> {
                _uiState.update {
                    it.mapSuccess {
                        it.copy(
                            addressUiMode = if (event.isFocused) {
                                if (it.addressSuggestions.isEmpty()) {
                                    PlaceAddressSearchUiMode.SEARCH_EXPANDED
                                } else {
                                    PlaceAddressSearchUiMode.FULLY_EXPANDED
                                }
                            } else {
                                PlaceAddressSearchUiMode.HIDDEN
                            }
                        )
                    }
                }

                if (event.isFocused) {
                    _eventChannel.trySend(CreatePrivacyZoneScreenEvent.RequestKeyboardFocusOnAddressInput)
                }
            }

            CreatePrivacyZoneScreenUiEvent.ToggleSearchFieldFocus -> {
                val shouldFocus =
                    _uiState.value.data?.addressUiMode == PlaceAddressSearchUiMode.HIDDEN
                _uiState.update {
                    it.mapSuccess {
                        it.copy(
                            addressUiMode = if (shouldFocus) {
                                if (it.addressSuggestions.isEmpty()) {
                                    PlaceAddressSearchUiMode.SEARCH_EXPANDED
                                } else {
                                    PlaceAddressSearchUiMode.FULLY_EXPANDED
                                }
                            } else {
                                PlaceAddressSearchUiMode.HIDDEN
                            },
                            isAddressLoading = false
                        )
                    }
                }

                if (shouldFocus) {
                    _eventChannel.trySend(CreatePrivacyZoneScreenEvent.RequestKeyboardFocusOnAddressInput)
                }
            }
        }
    }

    private fun tryCreatePrivacyZone() {
        val data = _uiState.value.data ?: return

        if (data.fullLoading || data.actionLoading) {
            return
        }

        if (data.lat == null || data.lng == null) {
            coroutineScope.launch {
                _eventChannel.send(CreatePrivacyZoneScreenEvent.MissingLocation)
            }
            return
        }

        val filteredName = data.name.trim().replace("\n", " ")
        if (filteredName.isBlank()) {
            coroutineScope.launch {
                _eventChannel.send(CreatePrivacyZoneScreenEvent.MissingInfo)
            }
            _uiState.update {
                it.mapSuccess { it.copy(hasNameError = true) }
            }
            return
        }

        val zone = zoneFlow.value.data
        val zoneData = PrivacyZoneData(
            name = data.name,
            image = data.image,
            lat = data.lat,
            lng = data.lng,
            radiusMeters = data.radiusValue.meters,
            expiresAt = zone?.data?.data?.expiresAt ?: "",
            notifyOthersInZone = data.notifyOthersInZone,
            inZoneMessage = if (data.notifyOthersInZone) {
                data.inZoneMessage?.trim()?.takeIf { it.isNotBlank() }
            } else {
                null
            },
        )

        if (zone != null) {
            val updatedZone = zone.copy(data = CryptoResource.Ready(zoneData))
            updatePrivacyZoneUseCase.invoke(updatedZone)
                .processZoneFlow()
        } else {
            coroutineScope.launch {
                if (checkPrivacyZoneLimits()) {
                    createPrivacyZoneUseCase.invoke(
                        groupId = groupId,
                        data = zoneData
                    ).processZoneFlow()
                }
            }
        }
    }

    private suspend fun checkPrivacyZoneLimits(): Boolean {
        val premiumStatus = premiumStatusManager.checkPrivacyZoneCountLimits(groupId.asGroupOrSpaceId())
        when (premiumStatus) {
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
                return false
            }

            is PremiumStatusLimitState.PlanLimitReached -> {
                if (premiumStatus.payload.isOwner) {
                    messageDispatcher.showSnackbar(MR.strings.privacy_zones_create_plan_limit_info.localizedString())
                    _uiState.update {
                        it.mapSuccess {
                            it.copy(
                                alertDialog = MainAlertDialog.PlanLimitForOwner(
                                    content = MR.strings.plan_limit_privacy_zones_alert_info.localizedString(),
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
                                    content = MR.strings.plan_limit_privacy_zones_alert_info.localizedString(),
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

    private fun Flow<Unit>.processZoneFlow() {
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
                            if (checkPrivacyZoneLimits()) {
                                refreshEverythingUseCase.scheduleRefreshAllGroupsWithPlaces()
                                showSnackbar(data.exception.errorString())
                                checkPrivacyZoneLimits()
                            }
                        } else {
                            showSnackbar(data.exception.errorString())
                        }
                    }

                    Payload.Loading -> {}
                    is Payload.Success -> {
                        _eventChannel.send(CreatePrivacyZoneScreenEvent.ZoneUpdated)
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
                _eventChannel.send(CreatePrivacyZoneScreenEvent.MissingInfo)
            }
            _uiState.update {
                it.mapSuccess { it.copy(hasNameError = true) }
            }
            return
        }

        _uiState.update {
            it.mapSuccess {
                it.copy(screenStep = CreatePrivacyZoneScreenStep.FULL)
            }
        }

        _eventChannel.trySend(CreatePrivacyZoneScreenEvent.RequestKeyboardFocusOnAddressInput)
    }

    private fun updateNotifyOthersInZone(enabled: Boolean) {
        _uiState.update {
            it.mapSuccess { state ->
                state.copy(
                    notifyOthersInZone = enabled,
                    inZoneMessage = if (enabled) state.inZoneMessage else null,
                )
            }
        }
    }

    private fun updateInZoneMessage(message: String) {
        val trimmedMessage = if (message.trim().length > zoneNameLengthLimit) {
            uiState.value.data?.inZoneMessage ?: ""
        } else {
            message
        }

        _uiState.update {
            it.mapSuccess {
                it.copy(inZoneMessage = trimmedMessage)
            }
        }
    }

    private fun updateNameAndImage(
        name: String? = uiState.value.data?.name,
        image: ImageModel? = uiState.value.data?.image
    ) {
        val name = if ((name?.trim()?.length ?: 0) > zoneNameLengthLimit) {
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
                        PlaceAddressSearchUiMode.HIDDEN
                    } else {
                        it.addressUiMode
                    }
                )
            }
        }
    }

    private fun updateRadius(index: Int) {
        _uiState.update {
            it.mapSuccess { state ->
                val radiusValue = state.radiusPresets.getOrNull(index) ?: state.radiusValue
                state.copy(
                    radiusValue = radiusValue,
                    radiusText = formatRadius(radiusValue)
                )
            }
        }
    }

    private fun formatRadius(radiusValue: ApproximateLocationDistancePair): String {
        return valueFormatter.getDistanceSmall(
            meters = when (valueFormatter.measurementSystem) {
                MeasurementSystemType.METRIC -> radiusValue.meters.toDouble()
                MeasurementSystemType.IMPERIAL -> radiusValue.metersForImperial
            },
            distanceType = DistanceType.DISTANCE,
            fractionsMode = FormattingFractionsMode.MINIMAL
        )
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
                        PlaceAddressSearchUiMode.SEARCH_EXPANDED
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
                            addressUiMode = PlaceAddressSearchUiMode.FULLY_EXPANDED,
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
                    addressUiMode = PlaceAddressSearchUiMode.HIDDEN,
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
                    image = ImageModel.emoji(Emojis.alien),
                    name = MR.strings.privacy_zone_create_suggestion_area51.localizedString()
                )
            )

            add(
                SuggestionSectionChipData(
                    image = ImageModel.emoji(Emojis.shoppingBags),
                    name = MR.strings.privacy_zone_create_suggestion_secret_shopping.localizedString()
                )
            )

            add(
                SuggestionSectionChipData(
                    image = ImageModel.emoji(Emojis.scenery),
                    name = MR.strings.privacy_zone_create_suggestion_hidden_place.localizedString()
                )
            )

            add(
                SuggestionSectionChipData(
                    image = ImageModel.emoji(Emojis.star),
                    name = MR.strings.privacy_zone_create_suggestion_just_for_me.localizedString()
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
