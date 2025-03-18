package app.paralino.core.domain.location

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.provider.IDeviceApiProvider

class UserMemberIdUseCase constructor(
    private val groupLocal: GetMemberWithLocationsUseCase,
    private val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs
) {

    suspend fun getCurrentDeviceType(): DeviceType {
        return deviceApiProvider.deviceType
    }

    suspend fun getDefaultDeviceName(groupId: String?): String {
        return groupId?.let { getCurrentLocationForGroup(it)?.deviceData?.data?.deviceName }
            ?: groupLocal.getMemberModelsWithLocationForUser(appPrefs.userId)
                .mapNotNull { it.currentDeviceLocation?.name }
                .takeIf { it.size == 1 }
                ?.first()
            ?: deviceApiProvider.defaultDeviceName
    }

    suspend fun getCurrentLocationForGroup(groupId: String): MemberLocation? {
        return groupLocal.getMemberWithLocationsOnce(
            groupId = groupId,
            userId = appPrefs.userId
        )?.currentDeviceLocation
    }

    suspend fun hasCurrentLocationIdForGroup(groupId: String): Boolean {
        return !groupLocal.getCurrentLocationIdForGroup(groupId = groupId).isNullOrBlank()
    }
}