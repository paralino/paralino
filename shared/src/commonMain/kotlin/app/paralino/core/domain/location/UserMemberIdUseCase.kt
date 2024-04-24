package app.paralino.core.domain.location

import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.user.UserRepository

class UserMemberIdUseCase constructor(
    private val userRepository: UserRepository,
    private val groupLocal: UserGroupLocalDataSource,
    private val memberLocal: LocationLocalDataSource,
    private val placeLocal: PlacesLocalDataSource,
    private val deviceApiProvider: IDeviceApiProvider
) {

    suspend fun getCurrentDeviceType(): DeviceType {
        return deviceApiProvider.deviceType
    }

    suspend fun getAllSessionCount(): Int {
        return userRepository.getAllSessionsFromRemote().size
    }

    suspend fun getDefaultDeviceName(groupId: String?): String {
        return groupId?.let { getCurrentLocationForGroup(it)?.deviceData?.data?.deviceName }
            ?: groupLocal.getMemberModelsWithLocationForUser(userRepository.cachedUserId)
                .mapNotNull { it.currentDeviceLocation?.name }
                .takeIf { it.size == 1 }
                ?.first()
            ?: deviceApiProvider.defaultDeviceName
    }

    suspend fun getCurrentLocationForGroup(groupId: String): MemberLocation? {
        return getCurrentUserMemberWithLocationsForGroup(groupId)?.currentDeviceLocation
    }

    suspend fun getCurrentLocationIdForGroup(groupId: String): String? {
        return getCurrentLocationForGroup(groupId)?.id
    }

    suspend fun hasCurrentLocationIdForGroup(groupId: String): Boolean {
        return !getCurrentLocationForGroup(groupId)?.id.isNullOrBlank()
    }

    suspend fun getCurrentUserMemberIdForGroup(groupId: String): String? {
        return memberLocal.getUserMemberId(
            groupId = groupId,
            userId = userRepository.cachedUserId
        )
    }

    suspend fun getCurrentUserMemberForGroup(groupId: String): MemberModel? {
        return groupLocal.getMemberOnce(
            groupId = groupId,
            userId = userRepository.cachedUserId
        )
    }

    suspend fun getCurrentUserMemberWithLocationsForGroup(groupId: String): MemberModelWithLocations? {
        return groupLocal.getMemberWithLocationsOnce(
            groupId = groupId,
            userId = userRepository.cachedUserId
        )
    }
}