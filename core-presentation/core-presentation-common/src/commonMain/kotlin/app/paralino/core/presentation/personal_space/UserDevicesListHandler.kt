package app.paralino.core.presentation.personal_space

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.DeactivatePersonalDeviceUseCase
import app.paralino.core.domain.user.DeletePersonalDeviceUseCase
import app.paralino.core.domain.user.PersonalSpaceRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.exception
import app.paralino.core.models.location.LocationUpdateDataFromDevice
import app.paralino.core.models.location.UserDeviceModel
import app.paralino.core.models.user.UserSession
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.AppSnackbarData
import app.paralino.core.presentation.common.AppSnackbarPayload
import app.paralino.core.presentation.common.GroupMemberItemLocationState
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.MemberLocationContextMapper
import app.paralino.core.presentation.common.MemberLocationVisibilityType
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class UserDevicesListHandler(
    private val userRepo: UserRepository,
    private val deletePersonalDevice: DeletePersonalDeviceUseCase,
    private val deactivatePersonalDevice: DeactivatePersonalDeviceUseCase,
    private val personalSpaceRefreshUseCase: PersonalSpaceRefreshUseCase,
    private val valueFormatter: IValueFormatter,
    private val appPrefs: AppPrefs,
    geofenceManager: IGeofenceManager,
    private val coroutineScope: CoroutineScope,
    private val messageDispatcher: ICoreMessageDispatcher,
    private val onDevicesChanged: (List<UserDeviceOrSessionItemState>) -> Unit,
    private val onAlertDialogChanged: (MainAlertDialog?) -> Unit,
    private val onLoadingChanged: (Boolean) -> Unit,
) {
    private val memberLocationContextMapper = MemberLocationContextMapper(
        geofenceManager = geofenceManager,
        valueFormatter = valueFormatter,
    )

    private val devicesStateFlow = MutableStateFlow<List<UserDeviceItemState>>(emptyList())
    private val sessionsStateFlow = MutableStateFlow<List<UserSessionItemState>>(emptyList())
    private val mergedStateFlow = MutableStateFlow<List<UserDeviceOrSessionItemState>>(emptyList())
    private val mergeMutex = Mutex()

    fun latestDevicesState(): List<UserDeviceOrSessionItemState> = mergedStateFlow.value

    fun refreshSessions() {
        coroutineScope.launch(Dispatchers.IO) {
            val sessions = try {
                userRepo.getAllSessionsFromRemote()
                    .map { UserSessionItemState(session = it) }
            } catch (e: Exception) {
                scheduleSessionErrorMessage(e)
                emptyList()
            }

            try {
                personalSpaceRefreshUseCase.refreshPersonalSpace(cacheTimeMillis = 10.seconds.inWholeMilliseconds)
            } catch (e: Exception) {
            }

            mergeMutex.withLock {
                sessionsStateFlow.value = sessions
                val merged = mapDevicesAndSessions(
                    devices = devicesStateFlow.value,
                    sessions = sessions,
                    previous = mergedStateFlow.value,
                )
                mergedStateFlow.value = merged
                onDevicesChanged(merged)
            }
        }
    }

    suspend fun publishDevices(
        devices: List<UserDeviceModel>,
        places: List<PlaceListItemData>,
        myLocation: LocationUpdateDataFromDevice?,
    ): List<UserDeviceOrSessionItemState> {
        val mappedDevices = mapDevices(
            devices = devices,
            places = places,
            myLocation = myLocation,
        )
        return mergeMutex.withLock {
            devicesStateFlow.value = mappedDevices
            mapDevicesAndSessions(
                devices = mappedDevices,
                sessions = sessionsStateFlow.value,
                previous = mergedStateFlow.value,
            ).also {
                mergedStateFlow.value = it
            }
        }
    }

    fun onDeviceEvent(event: UserDeviceItemEvent) {
        when (event) {
            is UserDeviceItemEvent.onDeleteDeviceClick -> {
                onAlertDialogChanged(
                    MainAlertDialog.DeleteInactiveUserOwnDevice(
                        device = event.item.device
                    )
                )
            }

            is UserDeviceItemEvent.onDeactivateDeviceClick -> {
                onAlertDialogChanged(
                    MainAlertDialog.DeactivateUserOwnDevice(
                        device = event.item.device,
                        isCurrentDevice = event.item.isCurrentDevice
                    )
                )
            }

            is UserDeviceItemEvent.onOpenSessionClick -> {
                onAlertDialogChanged(
                    MainAlertDialog.SessionInfo(
                        content = sessionInfo(event.item),
                        canDelete = !event.item.current,
                        session = event.item
                    )
                )
            }
        }
    }

    fun onDialogAction(action: AlertDialogAction) {
        when (val dialog = action.dialog) {
            is MainAlertDialog.SessionInfo -> {
                if (action.action == MainAlertDialog.Buttons.buttonOk) {
                    onAlertDialogChanged(
                        MainAlertDialog.SessionDeleteConfirmation(
                            content = sessionInfo(dialog.session),
                            session = dialog.session
                        )
                    )
                } else {
                    onAlertDialogChanged(null)
                }
            }

            is MainAlertDialog.SessionDeleteConfirmation -> {
                if (action.action == MainAlertDialog.Buttons.buttonOk) {
                    logout(sessionId = dialog.session.id)
                }
                onAlertDialogChanged(null)
            }

            is MainAlertDialog.DeleteInactiveUserOwnDevice -> {
                if (action.isOk) {
                    coroutineScope.launch {
                        onAlertDialogChanged(null)
                        delay(200)
                        onAlertDialogChanged(
                            MainAlertDialog.DeleteInactiveUserOwnDeviceConfirm(dialog.device)
                        )
                    }
                } else {
                    onAlertDialogChanged(null)
                }
            }

            is MainAlertDialog.DeleteInactiveUserOwnDeviceConfirm -> {
                if (action.action == MainAlertDialog.Buttons.buttonOk) {
                    deleteDevice(device = dialog.device)
                }
                onAlertDialogChanged(null)
            }

            is MainAlertDialog.DeactivateUserOwnDevice -> {
                if (action.action == MainAlertDialog.Buttons.buttonOk) {
                    deactivateDevice(device = dialog.device)
                }
                onAlertDialogChanged(null)
            }

            else -> {}
        }
    }

    private fun mapDevices(
        devices: List<UserDeviceModel>,
        places: List<PlaceListItemData>,
        myLocation: LocationUpdateDataFromDevice?,
    ): List<UserDeviceItemState> {
        return devices
            .sortedBy { it.deviceId != appPrefs.deviceId }
            .map { device ->
                val isCurrentDevice = device.deviceId == appPrefs.deviceId
                UserDeviceItemState(
                    device = device,
                    location = device.location?.let { location ->
                        memberLocationContextMapper.enrichMemberLocationState(
                            location = GroupMemberItemLocationState(
                                memberLocation = location,
                                name = device.deviceNameOrNull ?: location.name,
                                image = null,
                                canEdit = true,
                                showSpeedAndElevation = false,
                                visibility = MemberLocationVisibilityType.VISIBLE_ALWAYS,
                                currentDevice = isCurrentDevice,
                                formatter = valueFormatter
                            ),
                            places = places,
                            myLocation = myLocation,
                        ).first()
                    },
                    isCurrentDevice = isCurrentDevice,
                    deleteAction = if (device.location == null) UserDeviceAction.DeleteDevice() else null,
                    deactivateAction = if (device.location != null) UserDeviceAction.DeactivateDevice() else null,
                )
            }
    }

    private fun mapDevicesAndSessions(
        devices: List<UserDeviceItemState>,
        sessions: List<UserSessionItemState>,
        previous: List<UserDeviceOrSessionItemState>,
    ): List<UserDeviceOrSessionItemState> {
        return buildList {
            addAll(
                devices.map { device ->
                    val session = sessions.firstOrNull { it.session.id == device.sessionId }
                    val previousDeviceState = previous.firstOrNull {
                        it.device?.device?.id == device.device.id
                    }
                    UserDeviceOrSessionItemState.DeviceWithSession(
                        device = device.copy(
                            deactivateAction = if (device.location != null) {
                                previousDeviceState?.device?.deactivateAction
                                    ?: UserDeviceAction.DeactivateDevice()
                            } else {
                                null
                            },
                            deleteAction = if (device.location == null && !device.isCurrentDevice) {
                                previousDeviceState?.device?.deleteAction
                                    ?: UserDeviceAction.DeleteDevice()
                            } else {
                                null
                            }
                        ),
                        session = session
                    )
                }
            )

            addAll(
                sessions.mapNotNull { session ->
                    if (devices.none { it.sessionId == session.session.id }) {
                        UserDeviceOrSessionItemState.SessionWithoutDevice(
                            session = session
                        )
                    } else {
                        null
                    }
                }
            )
        }
    }

    private fun sessionInfo(session: UserSession): String {
        return buildString {
            append(session.deviceOrClientName)
            append("\n")

            append(session.osName)
            append(" ")
            append(session.osVersion)
            append("\n")

            session.createdAt.isoWithOffsetToInstant()?.let {
                valueFormatter.formatRelativeDateTime(
                    timeUTC = it.toLocalDateTime(TimeZone.UTC)
                )
            }?.let {
                append(MR.strings.personalSpace_session_createdAt.localizedString())
                append(" ")
                append(it)
                append("\n")
            }

            session.updatedAt.isoWithOffsetToInstant()?.let {
                valueFormatter.formatRelativeDateTime(
                    timeUTC = it.toLocalDateTime(TimeZone.UTC)
                )
            }?.let {
                append(MR.strings.personalSpace_session_updatedAt.localizedString())
                append(" ")
                append(it)
                append("\n")
            }

            session.expire.isoWithOffsetToInstant()?.let {
                valueFormatter.formatSimpleDateTime(
                    timeUTC = it.toLocalDateTime(TimeZone.UTC)
                )
            }?.let {
                append(MR.strings.personalSpace_session_expiresAt.localizedString())
                append(" ")
                append(it)
                append("\n")
            }

            append(session.countryName)
            append(", ")
            append(session.ip)
        }
    }

    private fun logout(sessionId: String) {
        logoutOrDeleteAction(
            logoutAction = {
                userRepo.remoteLogout(sessionId = sessionId)
            },
            deleteDeviceAction = {}
        )
    }

    private fun deleteDevice(device: UserDeviceModel) {
        logoutOrDeleteAction(
            logoutAction = {
                val sessionId = device.sessionId
                if (!sessionId.isNullOrBlank()) {
                    userRepo.remoteLogout(sessionId = sessionId)
                }
            },
            deleteDeviceAction = {
                deletePersonalDevice.deletePersonalDevice(deviceId = device.deviceId)
            }
        )
    }

    private fun logoutOrDeleteAction(
        logoutAction: suspend () -> Unit,
        deleteDeviceAction: suspend () -> Unit,
    ) {
        flow {
            logoutAction()
            emit(Unit)
        }
            .mapToPayload { Unit }
            .onEach { state ->
                state.exception?.let { messageDispatcher.showSnackbar(it.errorString()) }

                onLoadingChanged(state is Payload.Loading)

                if (state is Payload.Success) {
                    refreshSessions()
                    deleteDeviceAction()
                }
            }
            .launchIn(coroutineScope)
    }

    private fun deactivateDevice(device: UserDeviceModel) {
        coroutineScope.launch {
            try {
                onLoadingChanged(true)
                deactivatePersonalDevice.deactivateLocationInPersonalSpace(deviceId = device.deviceId)
                onLoadingChanged(false)
            } catch (e: Exception) {
                onLoadingChanged(false)
                logg.e("Error deleting device", e)
                messageDispatcher.showSnackbar(e.errorString())
            }
        }
    }

    private fun scheduleSessionErrorMessage(e: Exception) {
        // showing this with delay, since it can fail faster than ui is rendered
        coroutineScope.launch {
            delay(400.milliseconds)
            messageDispatcher.showSnackbar(
                data = AppSnackbarData(
                    payload = AppSnackbarPayload(
                        title = MR.strings.personalSpace_sessions_load_error.localizedString(),
                        text = e.errorString()
                    )
                )
            )
        }
    }
}
