package app.paralino.core.domain.location

import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.repo.places.IPlacesLocalDataSource
import app.paralino.core.repo.user.UserRepository

class UserMemberIdUseCase constructor(
    private val userRepository: UserRepository,
    private val memberLocal: ILocationLocalDataSource,
    private val placeLocal: IPlacesLocalDataSource
) {

    suspend fun getCurrentDeviceMemberIdForGroup(groupId: String): String? {
        return memberLocal.getUserMemberIds(
            userId = userRepository.cachedUserId,
            groupId = groupId
        )
            // TODO replace with proper id when multiple device support is added
            .firstOrNull()
    }

    suspend fun getCurrentDeviceMemberIdForPlace(placeId: String): String? {
        val place = placeLocal.getPlaceWithoutSettingsOnce(placeId)
        return place?.groupId?.let {
            getCurrentDeviceMemberIdForGroup(it)
        }
    }

}