package app.paralino.core.domain.location

import app.paralino.core.config
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.repo.places.IPlacesLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.IDeviceApiProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class UserMemberIdUseCase constructor(
    private val userRepository: UserRepository,
    private val memberLocal: ILocationLocalDataSource,
    private val placeLocal: IPlacesLocalDataSource,
    private val deviceApiProvider: IDeviceApiProvider
) {

    suspend fun getCurrentDeviceType(): DeviceType {
        return deviceApiProvider.deviceType
    }

    suspend fun getCurrentDeviceId(): String {
        return config.deviceId
    }

    suspend fun getCurrentSessionId(): String {
        return config.sessionId
    }

    suspend fun getCurrentLocationForGroup(groupId: String): MemberLocation? {
        val locationsForGroup = getCurrentUserMemberForGroup(groupId)!!.locations

        val locationForDeviceId = locationsForGroup.firstOrNull {
            it.deviceId == config.deviceId
        }

        return locationForDeviceId
    }

    suspend fun getCurrentLocationIdForGroup(groupId: String): String? {
        return getCurrentLocationForGroup(groupId)?.id
    }

    suspend fun getCurrentUserMemberIdForGroup(groupId: String): String? {
        return memberLocal.getUserMemberId(
            groupId = groupId,
            userId = userRepository.cachedUserId
        )
    }

    suspend fun getCurrentUserMemberForGroup(groupId: String): MemberModelWithLocations? {
        return memberLocal.getMember(
            groupId = groupId,
            userId = userRepository.cachedUserId
        ).firstOrNull()
    }

    suspend fun getCurrentUserMemberIdForPlace(placeId: String): String? {
        val place = placeLocal.getPlaceWithoutSettingsOnce(placeId)
        return place?.groupId?.let {
            getCurrentUserMemberIdForGroup(it)
        }
    }

}