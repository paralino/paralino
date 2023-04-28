package app.paralino.core.domain.location

import app.paralino.core.config
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.provider.IDeviceApiProvider

class UserMemberIdUseCase constructor(
    private val userRepository: UserRepository,
    private val memberLocal: LocationLocalDataSource,
    private val placeLocal: PlacesLocalDataSource,
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

    suspend fun getAllSessionCount(): Int {
        return userRepository.getAllSessionsFromRemote().size
    }

    suspend fun getCurrentLocationForGroup(groupId: String): MemberLocation? {
        val locationsForGroup = getCurrentUserMemberWithLocationsForGroup(groupId)!!.locations

        val locationForDeviceId = locationsForGroup.firstOrNull {
            it.deviceId == config.deviceId
        }

        return locationForDeviceId
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
        return memberLocal.getMemberOnce(
            groupId = groupId,
            userId = userRepository.cachedUserId
        )
    }

    suspend fun getCurrentUserMemberWithLocationsForGroup(groupId: String): MemberModelWithLocations? {
        return memberLocal.getMemberWithLocationsOnce(
            groupId = groupId,
            userId = userRepository.cachedUserId
        )
    }

    suspend fun getCurrentUserMemberIdForPlace(placeId: String): String? {
        val place = placeLocal.getPlaceWithoutSettingsOnce(placeId)
        return place?.groupId?.let {
            getCurrentUserMemberIdForGroup(it)
        }
    }

    suspend fun getCurrentLocationIdForPlace(placeId: String): String? {
        val place = placeLocal.getPlaceWithoutSettingsOnce(placeId)
        return place?.groupId?.let {
            getCurrentLocationIdForGroup(it)
        }
    }

}