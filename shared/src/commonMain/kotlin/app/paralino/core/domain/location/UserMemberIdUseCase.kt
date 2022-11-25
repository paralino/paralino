package app.paralino.core.domain.location

import app.paralino.core.config
import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.repo.places.IPlacesLocalDataSource
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first

class UserMemberIdUseCase constructor(
    private val userRepository: UserRepository,
    private val memberLocal: ILocationLocalDataSource,
    private val placeLocal: IPlacesLocalDataSource
) {

    suspend fun getCurrentDeviceId(): String {
        return config.token
    }

    suspend fun getCurrentLocationIdForGroup(groupId: String): String? {
        // TODO replace with proper id when multiple device support is added
        return memberLocal.getMember(
            groupId = groupId,
            userId = userRepository.cachedUserId
        ).first().locations.firstOrNull {
            it.deviceId == config.token
        }?.id
    }

    suspend fun getCurrentUserMemberIdForGroup(groupId: String): String? {
        return memberLocal.getUserMemberId(
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

}