package app.paralino.core.presentation.privacyzones.detail

import app.paralino.core.resources.moko.*
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.privacyzone.DeletePrivacyZoneUseCase
import app.paralino.core.domain.privacyzone.GetPrivacyZonesUseCase
import app.paralino.core.domain.privacyzone.nameOrDefault
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PrivacyZoneDetailViewModel(
    val groupId: String,
    val zoneId: String,
    private val getPrivacyZonesUseCase: GetPrivacyZonesUseCase,
    private val deletePrivacyZoneUseCase: DeletePrivacyZoneUseCase,
    private val groupRepo: UserGroupLocalDataSource,
    private val premiumStatusManager: PremiumStatusManager,
    private val appPrefs: AppPrefs,
    private val valueFormatter: IValueFormatter
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<PrivacyZoneDetailScreenState>>(Payload.Loading)
    val uiState: StateFlow<Payload<PrivacyZoneDetailScreenState>> = _uiState

    private val _eventChannel = Channel<PrivacyZoneDetailScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        loadGroupPrivacyZone(groupId = groupId, zoneId = zoneId)
            .mapToPayload { it }
            .onEach { result -> _uiState.update { result } }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        coroutineScope.launch {
            try {
                getPrivacyZonesUseCase.refreshPrivacyZoneMaybe(zoneId = zoneId)
            } catch (e: Exception) {
                logg.w("privacy_zones", "Failed to refresh privacy zone: ${e.message}")
            }
        }
    }

    private fun loadGroupPrivacyZone(
        groupId: String,
        zoneId: String
    ): Flow<PrivacyZoneDetailScreenState> {
        return combine(
            groupRepo.getGroupWithoutMembers(groupId),
            getPrivacyZonesUseCase.getPrivacyZoneLocally(zoneId),
        ) { group, zone ->
            group to zone
        }.mapNotNull { (group, zone) ->
            val zoneData = zone?.data?.data ?: return@mapNotNull null

            PrivacyZoneDetailScreenState(
                groupOrSpaceId = groupId.asGroupOrSpaceId(),
                zoneId = zoneId,
                groupName = group.name,
                name = zone.nameOrDefault,
                image = zoneData.image,
                lat = zoneData.lat,
                lng = zoneData.lng,
                radiusMeters = zoneData.radiusMeters,
                expirationStatus = valueFormatter.formatExpirationDateTimeUTC(
                    expiresAtTimeUTC = zoneData.expiresAt,
                    showActiveStatus = true
                ),
                canEdit = zone.ownerUserId == appPrefs.userId,
                fullLoading = _uiState.value.data?.fullLoading ?: false,
                androidMapSDKProvider = premiumStatusManager.defaultMapProvider(),
                alertDialog = _uiState.value.data?.alertDialog,
            )
        }
    }

    fun onEvent(event: PrivacyZoneDetailScreenUiEvent) {
        when (event) {
            PrivacyZoneDetailScreenUiEvent.DeleteZone -> {
                _uiState.update {
                    it.mapSuccess { it.copy(alertDialog = MainAlertDialog.DeleteZone) }
                }
            }

            is PrivacyZoneDetailScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.DeleteZone -> {
                        _uiState.update { it.mapSuccess { it.copy(alertDialog = null) } }
                        if (event.action.isOk) {
                            deletePrivacyZone()
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun deletePrivacyZone() {
        if (_uiState.value.data?.canEdit != true) {
            // will show generic error
            showSnackbar(Throwable().errorString())
            return
        }

        deletePrivacyZoneUseCase.invoke(
            privacyZoneId = zoneId,
            groupId = groupId,
        )
            .mapToPayload { it }
            .onEach { result ->
                _uiState.update { it.mapSuccess { state -> state.copy(fullLoading = result is Payload.Loading) } }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Success -> _eventChannel.send(PrivacyZoneDetailScreenEvent.ZoneDeleted)
                    is Payload.Error -> showSnackbar(result.exception.errorString())
                }
            }
            .launchIn(coroutineScope)
    }
}
