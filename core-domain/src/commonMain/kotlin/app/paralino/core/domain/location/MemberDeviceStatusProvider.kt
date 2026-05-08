package app.paralino.core.domain.location

import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberDeviceStatus
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt

class MemberDeviceStatusProvider(
    private val locationSharingManager: LocationSharingManager,
    private val deviceApiProvider: IDeviceApiProvider,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val valueFormatter: IValueFormatter
) {

    private suspend fun getStatusType(
        groupId: GroupOrSpaceId,
        contract: LocationSharingContract?
    ): MemberDeviceStatusType {
        val locationStatus = deviceApiProvider.locationStatusProvider.currentStatus
        val isSharingActive = contract?.asValidContract()?.groupOrSpaceId == groupId ||
                locationSharingManager.isSharingActiveForGroup(groupId)
        val shareDeviceStatus = contract?.shareDeviceStatus ?: false

        // if app was not launched from foreground and we are missing background location permission
        val missingBackgroundPermission = !locationStatus.isBackgroundPermissionAllowed
                && !deviceApiProvider.appLifecycleState.value.current.activeOrInactive
                && !deviceApiProvider.appLifecycleState.value.wasEverActive

        return when {
            !isSharingActive -> {
                if (shareDeviceStatus) MemberDeviceStatusType.NOT_SHARING
                else MemberDeviceStatusType.INACTIVE
            }

            !locationStatus.isAnyLocationPermissionAllowed -> {
                if (shareDeviceStatus) MemberDeviceStatusType.MISSING_PERMISSIONS
                else MemberDeviceStatusType.INACTIVE
            }

            missingBackgroundPermission -> {
                if (shareDeviceStatus) MemberDeviceStatusType.MISSING_PERMISSIONS
                else MemberDeviceStatusType.INACTIVE
            }

            !locationStatus.isGpsOn -> {
                if (shareDeviceStatus) MemberDeviceStatusType.GPS_OFF
                else MemberDeviceStatusType.INACTIVE
            }

            locationPerformanceManager.locationPerformanceType.value.isThrottlingLocationRequest -> MemberDeviceStatusType.TOO_MANY_REQUESTS
            locationPerformanceManager.locationPerformanceType.value.current <= LocationPerformanceType.BATTERY_SAVER -> {
                if (shareDeviceStatus) MemberDeviceStatusType.BATTERY_SAVER_ON
                else MemberDeviceStatusType.IDLE
            }

            locationPerformanceManager.locationPerformanceType.value.isThrottling -> MemberDeviceStatusType.THROTTLING
            locationPerformanceManager.locationPerformanceType.value.isSlow -> MemberDeviceStatusType.IDLE
            else -> MemberDeviceStatusType.ACTIVE
        }
    }

    suspend fun getStatus(
        groupId: GroupOrSpaceId,
        contract: LocationSharingContract? = null
    ): MemberDeviceStatus {
        val sharingContract = contract ?: locationSharingManager.getUnfilteredContractForGroupOnce(groupId)
        val validContract = sharingContract?.asValidContract()
        val sharingBattery = validContract?.shareBattery == true
        val sharingNetwork = validContract?.shareNetwork == true
        return MemberDeviceStatus(
            timeUTC = valueFormatter.currentTimeForApi(),
            tz = TimeZone.currentSystemDefault().offsetAt(nowUTC()).toString(),
            batteryLevel = if (sharingBattery) deviceApiProvider.batteryLevel else null,
            isCharging = if (sharingBattery) deviceApiProvider.isCharging.takeIf { sharingBattery } else null,
            network = if (sharingNetwork) deviceApiProvider.connectionStatusProvider.initAndGetStatus().networkType?.key else null,
            signal = if (sharingNetwork) deviceApiProvider.connectionStatusProvider.getSignalStrength() else null,
            status = getStatusType(groupId, sharingContract).fullKey
        )
    }
}