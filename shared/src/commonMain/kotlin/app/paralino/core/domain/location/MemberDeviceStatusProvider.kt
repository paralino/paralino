package app.paralino.core.domain.location

import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.LocationSharingContract
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

    private fun getStatusType(groupId: String, contract: LocationSharingContract?): MemberDeviceStatusType {
        val locationStatus = deviceStatus.locationStatusProvider.currentStatus
        val isSharingActive = contract?.asValidContract()?.groupId == groupId || locationSharingManager.isSharingActiveForGroup(groupId)

        return when {
            !isSharingActive -> MemberDeviceStatusType.NOT_SHARING
            !locationStatus.isPermissionAllowed -> MemberDeviceStatusType.MISSING_PERMISSIONS
            !locationStatus.isGpsOn -> MemberDeviceStatusType.GPS_OFF
            locationPerformanceManager.locationPerformanceType.value.isThrottlingLocationRequest -> MemberDeviceStatusType.TOO_MANY_REQUESTS
            locationPerformanceManager.locationPerformanceType.value.current == LocationPerformanceType.BATTERY_SAVER -> MemberDeviceStatusType.BATTERY_SAVER_ON
            locationPerformanceManager.locationPerformanceType.value.isThrottling -> MemberDeviceStatusType.THROTTLING
            locationPerformanceManager.locationPerformanceType.value.isSlow -> MemberDeviceStatusType.IDLE
            else -> MemberDeviceStatusType.ACTIVE
        }
    }

    suspend fun getStatus(
        groupId: String,
        contract: LocationSharingContract? = null
    ): MemberDeviceStatus {
        val sharingContract = contract ?: locationSharingManager.currentSharingContractForGroup(groupId)
        val sharingBattery = sharingContract?.shareBattery == true
        val sharingNetwork = sharingContract?.shareNetwork == true
        return MemberDeviceStatus(
            timeUTC = valueFormatter.currentTimeForApi(),
            batteryLevel = if(sharingBattery) deviceStatus.batteryLevel else null,
            isCharging = if(sharingBattery) deviceStatus.isCharging.takeIf { sharingBattery } else null,
            network = if(sharingNetwork) deviceStatus.connectionStatusProvider.initAndGetStatus().networkType?.key else null,
            signal = if(sharingNetwork) deviceStatus.connectionStatusProvider.getSignalStrength() else null,
            status = getStatusType(groupId, sharingContract).key
        )
    }
}