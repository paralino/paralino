package app.paralino.core.domain.location

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.user.PersonalDevicesLocalDataSource

class UserMemberIdUseCase constructor(
    private val groupLocal: GetMemberWithLocationsUseCase,
    private val personalDevicesLocal: PersonalDevicesLocalDataSource,
    private val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs
) {

    suspend fun getCurrentDeviceType(): DeviceType {
        return deviceApiProvider.deviceType
    }

    suspend fun getDefaultDeviceName(groupId: GroupOrSpaceId?): String {
        val defaultInGroup = if (groupId?.isPersonalSpace == true) {
            personalDevicesLocal.getUserDeviceByDeviceIdOnce(deviceId = appPrefs.deviceId)
                ?.deviceNameOrNull
        } else {
            groupId?.let { getCurrentLocationForGroup(it)?.nameOrNull }
        }

        return defaultInGroup
            ?: groupLocal.getMemberModelsWithLocationForUser(appPrefs.userId)
                .mapNotNull { it.currentDeviceLocation?.name }
                .takeIf { it.size == 1 }
                ?.first()
            ?: deviceApiProvider.defaultDeviceName
    }

    suspend fun getCurrentLocationForGroup(groupId: GroupOrSpaceId): MemberLocation? {
        if (groupId.isPersonalSpace) {
            return personalDevicesLocal.getUserDeviceByDeviceIdOnce(deviceId = appPrefs.deviceId)
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