package app.paralino.core.domain.location

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.group.UserGroupLocalDataSource

class UserMemberIdUseCase constructor(
    private val groupLocal: UserGroupLocalDataSource,
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

    suspend fun getCurrentLocationIdForGroup(groupId: String): String? {
        return getCurrentLocationForGroup(groupId)?.id
    }

    suspend fun hasCurrentLocationIdForGroup(groupId: String): Boolean {
        return !getCurrentLocationForGroup(groupId)?.id.isNullOrBlank()
    }

    suspend fun getCurrentUserMemberForGroup(groupId: String): MemberModel? {
        return groupLocal.getMemberOnce(
            groupId = groupId,
            userId = appPrefs.userId
        )
    }
}