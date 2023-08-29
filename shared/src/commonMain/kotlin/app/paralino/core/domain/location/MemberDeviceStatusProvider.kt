package app.paralino.core.domain.location

import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.MemberDeviceStatus
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.location.LocationPerformanceManager

class MemberDeviceStatusProvider(
    private val locationSharingManager: LocationSharingManager,
    private val deviceStatus: IDeviceApiProvider,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val valueFormatter: IValueFormatter
) {

    private fun getStatusType(groupId: String): MemberDeviceStatusType {
        val locationStatus = deviceStatus.locationStatusProvider.currentStatus

        return when {
            !locationSharingManager.isSharingActiveForGroup(groupId) -> MemberDeviceStatusType.NOT_SHARING
            !locationStatus.isPermissionAllowed -> MemberDeviceStatusType.MISSING_PERMISSIONS
            !locationStatus.isGpsOn -> MemberDeviceStatusType.GPS_OFF
            locationPerformanceManager.locationPerformanceType.value.isThrottlingLocationRequest -> MemberDeviceStatusType.TOO_MANY_REQUESTS
            locationPerformanceManager.locationPerformanceType.value.current == LocationPerformanceType.BATTERY_SAVER -> MemberDeviceStatusType.BATTERY_SAVER_ON
            locationPerformanceManager.locationPerformanceType.value.isThrottling -> MemberDeviceStatusType.THROTTLING
            locationPerformanceManager.locationPerformanceType.value.isSlow -> MemberDeviceStatusType.IDLE
            else -> MemberDeviceStatusType.ACTIVE
        }
    }

    suspend fun getPreSharingStatus(groupId: String): MemberDeviceStatus {
        val status = getStatusType(groupId).let {
            if(it == MemberDeviceStatusType.NOT_SHARING)
                MemberDeviceStatusType.ACTIVE
            else it
        }
        return MemberDeviceStatus(
            timeUTC = valueFormatter.currentTimeForApi(),
            batteryLevel = deviceStatus.batteryLevel,
            isCharging = deviceStatus.isCharging,
            network = deviceStatus.connectionStatusProvider.initAndGetStatus().networkType?.key,
            signal = deviceStatus.connectionStatusProvider.getSignalStrength(),
            status = status.key
        )
    }

    suspend fun getStatus(groupId: String): MemberDeviceStatus {
        return MemberDeviceStatus(
            timeUTC = valueFormatter.currentTimeForApi(),
            batteryLevel = deviceStatus.batteryLevel.takeIf { locationSharingManager.isSharingActiveForGroup(groupId) },
            isCharging = deviceStatus.isCharging.takeIf { locationSharingManager.isSharingActiveForGroup(groupId) },
            network = deviceStatus.connectionStatusProvider.initAndGetStatus().networkType?.key,
            signal = deviceStatus.connectionStatusProvider.getSignalStrength(),
            status = getStatusType(groupId).key
        )
    }
}