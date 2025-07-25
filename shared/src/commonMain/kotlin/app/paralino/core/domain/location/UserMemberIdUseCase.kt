package app.paralino.core.domain.location

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource

class UserMemberIdUseCase constructor(
    private val groupLocal: GetMemberWithLocationsUseCase,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs
) {

    suspend fun getCurrentDeviceType(): DeviceType {
        return deviceApiProvider.deviceType
    }

    suspend fun getDefaultDeviceName(groupId: GroupOrSpaceId?): String {
        return groupId?.let { getCurrentLocationForGroup(it)?.deviceData?.data?.deviceName }
            ?: groupLocal.getMemberModelsWithLocationForUser(appPrefs.userId)
                .mapNotNull { it.currentDeviceLocation?.name }
                .takeIf { it.size == 1 }
                ?.first()
            ?: deviceApiProvider.defaultDeviceName
    }

    suspend fun getCurrentLocationForGroup(groupId: GroupOrSpaceId): MemberLocation? {
        if (groupId.isPersonalSpace) {
            return personalSpaceRepo.getUserDeviceByDeviceIdOnce(deviceId = appPrefs.deviceId)
                ?.location
        } else {
            return groupLocal.getMemberWithLocationsOnce(
                groupId = groupId.id,
                userId = appPrefs.userId
            )?.currentDeviceLocation
        }
    }

    suspend fun hasCurrentLocationIdForGroup(groupId: String): Boolean {
        return !groupLocal.getCurrentLocationIdForGroup(groupId = groupId.asGroupOrSpaceId()).isNullOrBlank()
    }
}