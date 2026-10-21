package app.paralino.core.presentation.settings.userdevices

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.domain.user.DeactivatePersonalDeviceUseCase
import app.paralino.core.domain.user.DeletePersonalDeviceUseCase
import app.paralino.core.domain.user.PersonalSpaceRefreshUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.exception
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.presentation.personal_space.UserDevicesListHandler
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class UserDevicesViewModel(
    private val personalSpaceLocal: UserPersonalSpaceLocalDataSource,
    private val placesRepo: PlacesRepository,
    private val locationCacheRepository: LocationCacheRepository,
    private val appPrefs: AppPrefs,
    userRepo: UserRepository,
    deletePersonalDevice: DeletePersonalDeviceUseCase,
    deactivatePersonalDevice: DeactivatePersonalDeviceUseCase,
    personalSpaceRefreshUseCase: PersonalSpaceRefreshUseCase,
    valueFormatter: IValueFormatter,
    geofenceManager: IGeofenceManager,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        UserDevicesScreenState(
            isLoadingData = true,
            loadingAction = false,
            devices = emptyList(),
            alertDialog = null,
        )
    )
    val uiState: StateFlow<UserDevicesScreenState> = _uiState

    private val devicesHandler = UserDevicesListHandler(
        userRepo = userRepo,
        deletePersonalDevice = deletePersonalDevice,
        deactivatePersonalDevice = deactivatePersonalDevice,
        personalSpaceRefreshUseCase = personalSpaceRefreshUseCase,
        valueFormatter = valueFormatter,
        appPrefs = appPrefs,
        geofenceManager = geofenceManager,
        coroutineScope = coroutineScope,
        messageDispatcher = messageDispatcher,
        onDevicesChanged = { devices ->
            _uiState.update { it.copy(devices = devices) }
        },
        onAlertDialogChanged = { dialog ->
            _uiState.update { it.copy(alertDialog = dialog) }
        },
        onLoadingChanged = { loading ->
            _uiState.update { it.copy(loadingAction = loading) }
        },
    )

    init {
        devicesHandler.refreshSessions()

        combine(
            personalSpaceLocal.personalSpaceState.mapNotNull { it.data },
            placesRepo.getPlacesForGroupLocally(groupId = appPrefs.userId),
            locationCacheRepository.lastLocationFlow,
        ) { personalSpaceData, places, myLocation ->
            val placesData = places
                .filter { it.data.data?.lat != null && it.data.data?.lng != null }
                .map { PlaceListItemData(it) }

            devicesHandler.publishDevices(
                devices = personalSpaceData.devices,
                places = placesData,
                myLocation = myLocation,
            )

            _uiState.update {
                it.copy(
                    isLoadingData = false,
                    devices = devicesHandler.latestDevicesState(),
                )
            }
        }
            .mapToPayload { it }
            .onEach { result ->
                if (result is Payload.Error) {
                    showSnackbar(result.exception.errorString())
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    fun onEvent(event: UserDevicesScreenUiEvent) {
        when (event) {
            is UserDevicesScreenUiEvent.OnDeviceEvent -> devicesHandler.onDeviceEvent(event.event)
            is UserDevicesScreenUiEvent.DialogAction -> devicesHandler.onDialogAction(event.action)
        }
    }
}
