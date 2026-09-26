package app.paralino.core.domain.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.StopSharingLocationUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.user.PersonalDevicesLocalDataSource

class PersonalSpaceFeatureUseCase(
    private val appPrefs: AppPrefs,
    private val personalDevices: PersonalDevicesLocalDataSource,
    private val deactivatePersonalDevice: DeactivatePersonalDeviceUseCase,
    private val stopSharingLocation: StopSharingLocationUseCase,
    private val locationSharingManager: LocationSharingManager,
) {
    val isEnabled: Boolean
        get() = appPrefs.isPersonalSpaceEnabled

    fun enable() {
        appPrefs.isPersonalSpaceEnabled = true
    }

    suspend fun disable() {
        val deactivated = tryDeactivateCurrentDevice()
        if (!deactivated) {
            try {
                stopSharingLocation.stopSharing(
                    groupOrSpaceId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()
                )
            } catch (e: Exception) {
                logg.e("Failed to stop personal space sharing while disabling the feature", e)
            }
        }
        appPrefs.isPersonalSpaceSelected = false
        appPrefs.isPersonalSpaceEnabled = false
    }

    suspend fun isCurrentDeviceActivated(): Boolean {
        val deviceId = appPrefs.deviceId
        if (deviceId.isBlank()) return false
        val hasLocation = personalDevices.getUserDeviceByDeviceIdOnce(deviceId)?.location != null
        val isSharing = locationSharingManager.isSharingActiveForGroup(
            id = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()
        )
        return hasLocation || isSharing
    }

    private suspend fun tryDeactivateCurrentDevice(): Boolean {
        val deviceId = appPrefs.deviceId
        if (deviceId.isBlank()) return false
        return try {
            deactivatePersonalDevice.deactivateLocationInPersonalSpace(deviceId = deviceId)
            true
        } catch (e: Exception) {
            logg.e("Failed to deactivate current device while disabling personal space", e)
            false
        }
    }
}
