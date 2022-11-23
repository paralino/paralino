package app.paralino.core.domain.location

import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.repo.places.IPlacesLocalDataSource
import app.paralino.core.repo.user.UserRepository

class UserMemberIdUseCase constructor(
    private val userRepository: UserRepository,
    private val memberLocal: ILocationLocalDataSource,
    private val placeLocal: IPlacesLocalDataSource
) {

    suspend fun getCurrentDeviceIdForGroup(groupId: String): String? {
        // TODO replace with proper id when multiple device support is added
        return memberLocal.getUserLocationsIds(
            groupId = groupId,
            userId = userRepository.cachedUserId
        ).firstOrNull()
    }

    suspend fun getCurrentDeviceMemberIdForGroup(groupId: String): String? {
        return memberLocal.getUserMemberId(
            groupId = groupId,
            userId = userRepository.cachedUserId
        )
    }

    suspend fun getCurrentDeviceMemberIdForPlace(placeId: String): String? {
        val place = placeLocal.getPlaceWithoutSettingsOnce(placeId)
        return place?.groupId?.let {
            getCurrentDeviceMemberIdForGroup(it)
        }
    }

}